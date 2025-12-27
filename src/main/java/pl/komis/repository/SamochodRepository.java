package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Samochod;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SamochodRepository extends MongoRepository<Samochod, String> {

    List<Samochod> findByMarkaIgnoreCase(String marka);
    List<Samochod> findByModelIgnoreCase(String model);
    List<Samochod> findByStatus(String status);
    List<Samochod> findByCenaBetween(BigDecimal minCena, BigDecimal maxCena);

    @Query(value = "{}", fields = "{'marka' : 1}")
    List<Samochod> findAllMarkiDistinct();

    long countByStatus(String status);

    // Proste wyszukiwanie z opcjonalnymi parametrami
    @Query("{$or: [" +
            "{'marka': {$regex: ?0, $options: 'i'}}, " +
            "{'model': {$regex: ?1, $options: 'i'}}, " +
            "{'status': {$regex: ?2, $options: 'i'}} " +
            "]}")
    List<Samochod> searchCarsSimple(String marka, String model, String status);
    // Zaawansowane wyszukiwanie - użyj Criteria w serwisie zamiast @Query
    // Metoda searchCars będzie implementowana w serwisie za pomocą Criteria
}