package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Serwis;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SerwisRepository extends MongoRepository<Serwis, String> {

    // Używaj nazw pól z modelu (samochodId, pracownikId)
    List<Serwis> findBySamochodId(String samochodId);
    List<Serwis> findByPracownikId(String pracownikId);

    List<Serwis> findByDataSerwisuBetween(LocalDate start, LocalDate end);

    @Query("{'koszt': null}")
    List<Serwis> findZarezerwowane();

    @Query("{'koszt': {$ne: null}}")
    List<Serwis> findZakonczone();

    // ZMIENIONE: Używamy dedykowanych metod zamiast @Query dla count
    long countByKosztIsNull();
    long countByKosztIsNotNull();
}