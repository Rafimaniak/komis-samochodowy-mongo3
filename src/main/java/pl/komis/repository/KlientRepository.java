package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Klient;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface KlientRepository extends MongoRepository<Klient, String> {

    Optional<Klient> findByEmail(String email);

    // Zamiast projekcji, pobierz cały dokument i wyciągnij pole w serwisie
    @Query("{'_id': ?0}")
    Optional<Klient> findKlientById(String klientId);

    // Usuń problematyczne metody z projekcją na BigDecimal
    // Zamiast tego użyj metod w serwisie które pobierają cały dokument
}