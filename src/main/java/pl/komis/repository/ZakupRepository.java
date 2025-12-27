package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Zakup;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ZakupRepository extends MongoRepository<Zakup, String> {

    @Query(value = "{'klient.$id': ?0}")
    List<Zakup> findByKlientId(String klientId);

    @Query(value = "{'pracownik.$id': ?0}")
    List<Zakup> findByPracownikId(String pracownikId);

    List<Zakup> findByDataZakupuBetween(LocalDate startDate, LocalDate endDate);

    @Query(value = "{'samochod.$id': ?0}")
    boolean existsBySamochodId(String samochodId);

    @Query(value = "{'samochod.$id': ?0, 'klient.$id': ?1}")
    boolean existsBySamochodIdAndKlientId(String samochodId, String klientId);
}