package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.repository.KlientRepository;
import pl.komis.repository.SamochodRepository;
import pl.komis.repository.ZakupRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SamochodRepository samochodRepository;
    private final ZakupRepository zakupRepository;
    private final KlientRepository klientRepository;
    private final MongoDBFunctionService mongoDBFunctionService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Podstawowe statystyki z repozytoriów
        stats.put("liczbaSamochodow", samochodRepository.count());
        stats.put("liczbaDostepnych", samochodRepository.countByStatus("DOSTEPNY"));
        stats.put("liczbaZakupow", zakupRepository.count());
        stats.put("liczbaKlientow", klientRepository.count());

        // Zaawansowane statystyki z funkcji MongoDB
        stats.put("statystykiZakupow", mongoDBFunctionService.getStatystykiZakupow());
        stats.put("statystykiKlientow", mongoDBFunctionService.getStatystykiKlientow());

        return stats;
    }
}
