package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
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

    @Transactional
    public String mergeDuplicateClients() {
        StringBuilder result = new StringBuilder();
        result.append("=== SCALANIE DUPLIKATÓW KLIENTÓW ===\n");

        List<Klient> allClients = klientRepository.findAll();
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

                Klient mainClient = duplicates.get(0);
                for (int i = 1; i < duplicates.size(); i++) {
                    Klient duplicate = duplicates.get(i);

                    // Scal dane
                    mainClient.setLiczbaZakupow(mainClient.getLiczbaZakupow() + duplicate.getLiczbaZakupow());
                    mainClient.setSaldoPremii(mainClient.getSaldoPremii()+duplicate.getSaldoPremii());
                    mainClient.setTotalWydane(mainClient.getTotalWydane()+duplicate.getTotalWydane());

                    // Przenieś zakupy do głównego klienta (poprzez klientId)
                    List<Zakup> zakupyDuplikata = zakupRepository.findByKlientId(duplicate.getId());
                    for (Zakup zakup : zakupyDuplikata) {
                        zakup.setKlientId(mainClient.getId());
                        zakupRepository.save(zakup);
                    }

                    // Przenieś użytkowników do głównego klienta (poprzez klientId)
                    List<User> users = userRepository.findAll().stream()
                            .filter(u -> u.getKlientId() != null &&
                                    u.getKlientId().equals(duplicate.getId()))
                            .collect(Collectors.toList());

                    for (User user : users) {
                        user.setKlientId(mainClient.getId());
                        userRepository.save(user);
                    }

                    // Usuń duplikat
                    klientRepository.delete(duplicate);
                    deletedCount++;
                }

                // Zaktualizuj procent premii
                if (mainClient.getLiczbaZakupow() >= 5) {
                    mainClient.setProcentPremii(15.0);
                } else if (mainClient.getLiczbaZakupow() >= 3) {
                    mainClient.setProcentPremii(10.0);
                } else if (mainClient.getLiczbaZakupow() >= 2) {
                    mainClient.setProcentPremii(5.0);
                } else {
                    mainClient.setProcentPremii(0.0);
                }

                klientRepository.save(mainClient);
                mergedCount++;
            }
        }

        result.append("\n\n=== PODSUMOWANIE ===");
        result.append("\nScalono: ").append(mergedCount).append(" grup duplikatów");
        result.append("\nUsunięto: ").append(deletedCount).append(" duplikatów");

        return result.toString();
    }
    public String checkForDuplicates() {
        StringBuilder result = new StringBuilder();
        result.append("=== SPRAWDZANIE DUPLIKATÓW KLIENTÓW ===\n");

        List<Klient> allClients = klientRepository.findAll();
        Map<String, List<Klient>> clientsByEmail = allClients.stream()
                .filter(k -> k.getEmail() != null && !k.getEmail().trim().isEmpty())
                .collect(Collectors.groupingBy(Klient::getEmail));

        int duplicateGroups = 0;
        int totalDuplicates = 0;

        for (Map.Entry<String, List<Klient>> entry : clientsByEmail.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups++;
                totalDuplicates += entry.getValue().size();
                result.append("\nEmail: ").append(entry.getKey())
                        .append(" - liczba duplikatów: ").append(entry.getValue().size());
            }
        }

        result.append("\n\n=== PODSUMOWANIE ===");
        result.append("\nZnaleziono grup duplikatów: ").append(duplicateGroups);
        result.append("\nŁączna liczba duplikatów: ").append(totalDuplicates);

        return result.toString();
    }

    public String createUniqueEmailIndex() {
        try {
            // Utwórz indeks unikalności dla email
            IndexOperations indexOps = mongoTemplate.indexOps(Klient.class);
            Index index = new Index().on("email", Sort.Direction.ASC).unique();
            indexOps.ensureIndex(index);

            return "Utworzono indeks unikalności dla pola email.";
        } catch (Exception e) {
            return "Błąd tworzenia indeksu: " + e.getMessage();
        }
    }

    public String fixMissingNames() {
        StringBuilder result = new StringBuilder();
        result.append("=== NAPRAWIANIE BRAKUJĄCYCH DANYCH KLIENTÓW ===\n");

        List<Klient> allClients = klientRepository.findAll();
        int fixedCount = 0;

        for (Klient klient : allClients) {
            boolean updated = false;

            if (klient.getImie() == null || klient.getImie().trim().isEmpty()) {
                klient.setImie("Nieznane");
                updated = true;
            }

            if (klient.getNazwisko() == null || klient.getNazwisko().trim().isEmpty()) {
                klient.setNazwisko("Nieznane");
                updated = true;
            }

            if (updated) {
                klientRepository.save(klient);
                fixedCount++;
            }
        }

        result.append("\nNaprawiono ").append(fixedCount).append(" klientów.");
        return result.toString();
    }
}