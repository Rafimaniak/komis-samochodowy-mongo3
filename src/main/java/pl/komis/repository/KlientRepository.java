package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.Klient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface KlientRepository extends MongoRepository<Klient, String> {

    Optional<Klient> findByEmail(String email);

    // Sprawdź czy klient o danym emailu już istnieje
    boolean existsByEmail(String email);

    // Znajdź klientów z tym samym emailem (do naprawy duplikatów)
    List<Klient> findAllByEmail(String email);

    @Query("{'_id': ?0}")
    Optional<Klient> findKlientById(String klientId);

    // Dodatkowe metody dla naprawy
    @Query(value = "{'email': ?0}", exists = true)
    boolean klientExistsByEmail(String email);

    // Usuń klientów o danym emailu (oprócz jednego)
    @Query(value = "{'email': ?0}", delete = true)
    void deleteAllByEmail(String email);
}