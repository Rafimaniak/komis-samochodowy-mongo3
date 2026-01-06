package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.komis.model.Samochod;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SamochodRepository extends MongoRepository<Samochod, String> {

    // Tylko podstawowe operacje CRUD
    // Wszystkie złożone operacje przez funkcje MongoDB

    List<Samochod> findByStatus(String status);
    long countByStatus(String status);

    // Reszta logiki w funkcjach MongoDB
}