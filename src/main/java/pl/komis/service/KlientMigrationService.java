package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.komis.model.Klient;
import pl.komis.model.User;
import pl.komis.model.Zakup;
import pl.komis.repository.KlientRepository;
import pl.komis.repository.UserRepository;
import pl.komis.repository.ZakupRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KlientMigrationService {

    private final KlientRepository klientRepository;
    private final UserRepository userRepository;
    private final ZakupRepository zakupRepository;
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    /**
     * Tworzy unikalny indeks na polu email w kolekcji klientów
     */
    @Transactional
    public String createUniqueEmailIndex() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps(Klient.class);

            // Usuń istniejące indeksy na emailu (jeśli istnieją)
            indexOps.getIndexInfo().forEach(index -> {
                if (index.getIndexFields().stream()
                        .anyMatch(field -> field.getKey().equals("email"))) {
                    indexOps.dropIndex(index.getName());
                }
            });

            // Utwórz unikalny indeks
            org.springframework.data.mongodb.core.index.Index index =
                    new org.springframework.data.mongodb.core.index.Index()
                            .on("email", org.springframework.data.domain.Sort.Direction.ASC)
                            .unique();

            indexOps.ensureIndex(index);

            return "Utworzono unikalny indeks na polu email w kolekcji klientów";
        } catch (Exception e) {
            return "Błąd tworzenia indeksu: " + e.getMessage();
        }
    }

    /**
     * Naprawia duplikaty klientów - łączy ich dane i usuwa duplikaty
     */
    @Transactional
    public String mergeDuplicateClients() {
        StringBuilder result = new StringBuilder();
        result.append("=== SCALANIE DUPLIKATÓW KLIENTÓW ===\n");

        // Znajdź wszystkich klientów
        List<Klient> allClients = klientRepository.findAll();

        // Grupuj po emailu
        Map<String, List<Klient>> clientsByEmail = allClients.stream()
                .filter(k -> k.getEmail() != null && !k.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(Klient::getEmail));

        int mergedCount = 0;
        int deletedCount = 0;

        for (Map.Entry<String, List<Klient>> entry : clientsByEmail.entrySet()) {
            String email = entry.getKey();
            List<Klient> duplicates = entry.getValue();

            if (duplicates.size() > 1) {
                result.append("\nEmail: ").append(email)
                        .append(" - znaleziono ").append(duplicates.size()).append(" duplikatów");

                // Wybierz głównego klienta (z największą liczbą zakupów lub najstarszego)
                Klient mainClient = duplicates.get(0);
                for (int i = 1; i < duplicates.size(); i++) {
                    Klient duplicate = duplicates.get(i);

                    // Scal dane
                    mainClient.setLiczbaZakupow(mainClient.getLiczbaZakupow() + duplicate.getLiczbaZakupow());
                    mainClient.setSaldoPremii(mainClient.getSaldoPremii().add(duplicate.getSaldoPremii()));
                    mainClient.setTotalWydane(mainClient.getTotalWydane().add(duplicate.getTotalWydane()));

                    // Przenieś zakupy do głównego klienta
                    List<Zakup> zakupyDuplikata = zakupRepository.findByKlientId(duplicate.getId());
                    for (Zakup zakup : zakupyDuplikata) {
                        zakup.setKlient(mainClient);
                        zakupRepository.save(zakup);
                    }

                    // Przenieś użytkowników do głównego klienta
                    List<User> users = userRepository.findAll().stream()
                            .filter(u -> u.getKlient() != null &&
                                    u.getKlient().getId().equals(duplicate.getId()))
                            .collect(Collectors.toList());

                    for (User user : users) {
                        user.setKlient(mainClient);
                        userRepository.save(user);
                    }

                    // Usuń duplikat
                    klientRepository.delete(duplicate);
                    deletedCount++;
                }

                // Zaktualizuj procent premii na podstawie scalonej liczby zakupów
                if (mainClient.getLiczbaZakupow() >= 5) {
                    mainClient.setProcentPremii(new BigDecimal("15"));
                } else if (mainClient.getLiczbaZakupow() >= 3) {
                    mainClient.setProcentPremii(new BigDecimal("10"));
                } else if (mainClient.getLiczbaZakupow() >= 2) {
                    mainClient.setProcentPremii(new BigDecimal("5"));
                } else {
                    mainClient.setProcentPremii(BigDecimal.ZERO);
                }

                // Zapisz głównego klienta
                klientRepository.save(mainClient);
                mergedCount++;

                result.append("\n  Zachowano klienta ID: ").append(mainClient.getId())
                        .append(" - zakupy: ").append(mainClient.getLiczbaZakupow())
                        .append(", saldo: ").append(mainClient.getSaldoPremii()).append(" zł");
            }
        }

        result.append("\n\n=== PODSUMOWANIE ===");
        result.append("\nScalono: ").append(mergedCount).append(" grup duplikatów");
        result.append("\nUsunięto: ").append(deletedCount).append(" duplikatów");

        return result.toString();
    }

    /**
     * Sprawdza i naprawia klientów bez imienia/nazwiska
     */
    @Transactional
    public String fixMissingNames() {
        StringBuilder result = new StringBuilder();
        result.append("=== NAPRAWIANIE BRAKUJĄCYCH DANYCH KLIENTÓW ===\n");

        List<Klient> allClients = klientRepository.findAll();
        int fixedCount = 0;

        for (Klient klient : allClients) {
            boolean needsFix = false;

            // Ustaw domyślne imię jeśli brak
            if (klient.getImie() == null || klient.getImie().trim().isEmpty()) {
                klient.setImie("Klient");
                needsFix = true;
            }

            // Ustaw domyślne nazwisko jeśli brak
            if (klient.getNazwisko() == null || klient.getNazwisko().trim().isEmpty()) {
                klient.setNazwisko("Nieznany");
                needsFix = true;
            }

            // Ustaw domyślny telefon jeśli brak
            if (klient.getTelefon() == null || klient.getTelefon().trim().isEmpty()) {
                klient.setTelefon("000000000");
                needsFix = true;
            }

            // Ustaw domyślne wartości liczbowe jeśli null
            if (klient.getLiczbaZakupow() == null) {
                klient.setLiczbaZakupow(0);
                needsFix = true;
            }

            if (klient.getProcentPremii() == null) {
                klient.setProcentPremii(BigDecimal.ZERO);
                needsFix = true;
            }

            if (klient.getSaldoPremii() == null) {
                klient.setSaldoPremii(BigDecimal.ZERO);
                needsFix = true;
            }

            if (klient.getTotalWydane() == null) {
                klient.setTotalWydane(BigDecimal.ZERO);
                needsFix = true;
            }

            if (needsFix) {
                klientRepository.save(klient);
                fixedCount++;
                result.append("Naprawiono klienta: ").append(klient.getEmail())
                        .append(" (ID: ").append(klient.getId()).append(")\n");
            }
        }

        result.append("\nNaprawiono: ").append(fixedCount).append(" klientów");
        return result.toString();
    }

    /**
     * Sprawdza czy istnieją duplikaty klientów
     */
    @Transactional(readOnly = true)
    public String checkForDuplicates() {
        StringBuilder result = new StringBuilder();
        result.append("=== SPRAWDZANIE DUPLIKATÓW KLIENTÓW ===\n");

        List<Klient> allClients = klientRepository.findAll();
        Map<String, List<Klient>> clientsByEmail = allClients.stream()
                .filter(k -> k.getEmail() != null && !k.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(Klient::getEmail));

        int duplicateGroups = 0;

        for (Map.Entry<String, List<Klient>> entry : clientsByEmail.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups++;
                result.append("\nEmail: ").append(entry.getKey())
                        .append(" - ").append(entry.getValue().size()).append(" duplikatów:");

                for (Klient klient : entry.getValue()) {
                    result.append("\n  - ID: ").append(klient.getId())
                            .append(" | ").append(klient.getImie()).append(" ").append(klient.getNazwisko())
                            .append(" | zakupy: ").append(klient.getLiczbaZakupow())
                            .append(" | saldo: ").append(klient.getSaldoPremii()).append(" zł");
                }
            }
        }

        if (duplicateGroups == 0) {
            result.append("\nNie znaleziono duplikatów klientów");
        } else {
            result.append("\n\nZnaleziono ").append(duplicateGroups)
                    .append(" grup duplikatów");
        }

        return result.toString();
    }
}