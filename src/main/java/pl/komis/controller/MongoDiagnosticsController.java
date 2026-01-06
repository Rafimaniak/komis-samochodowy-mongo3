package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/mongo")
@PreAuthorize("hasRole('ADMIN')")
public class MongoDiagnosticsController {

    private final MongoTemplate mongoTemplate;

    @GetMapping("/stats")
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();

        // Statystyki kolekcji
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            Document statsDoc = mongoTemplate.executeCommand(
                    new Document("collStats", collectionName)
            );
            stats.put(collectionName, statsDoc);
        }

        // Aktywne indeksy
        Map<String, List<Document>> indexes = new HashMap<>();
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            List<Document> collectionIndexes = mongoTemplate.executeCommand(
                    new Document("listIndexes", collectionName)
            ).getList("cursor", Document.class, new ArrayList<>());
            indexes.put(collectionName, collectionIndexes);
        }
        stats.put("indexes", indexes);

        return stats;
    }

    @PostMapping("/optimize/{collectionName}")
    public String optimizeCollection(@PathVariable String collectionName) {
        // Spring Data MongoDB automatycznie tworzy indeksy z adnotacji
        // Możesz utworzyć konkretny indeks ręcznie jeśli potrzebujesz
        return "Kolekcja: " + collectionName + " - indeksy są tworzone automatycznie przez Spring Data MongoDB";
    }
}