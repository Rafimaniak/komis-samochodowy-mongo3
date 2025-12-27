package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Serwis;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SerwisRepository extends MongoRepository<Serwis, String> {

    List<Serwis> findByDataSerwisuBetween(LocalDate od, LocalDate do_);

    @Query("{'samochod.$id': ?0}")
    List<Serwis> findBySamochodId(String samochodId);

    @Query("{'pracownik.$id': ?0}")
    List<Serwis> findByPracownikId(String pracownikId);

    @Query("{'koszt': null}")
    long countReservedServices();

    @Query("{'koszt': {$ne: null}}")
    long countCompletedServices();

    @Query("{'koszt': {$ne: null}}")
    List<Serwis> findCompletedServices();
}