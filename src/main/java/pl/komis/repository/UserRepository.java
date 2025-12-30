package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("{'role': ?0}")
    List<User> findByRole(String role);

    long countByRole(String role);

    @Query("{'enabled': true}")
    List<User> findActiveUsers();

    @Query("{'enabled': false}")
    List<User> findInactiveUsers();

    // POPRAWIONE: Zmień na właściwe zapytanie dla @DBRef
    @Query("{'klient.$id': ?0}")
    List<User> findByKlientId(String klientId);
}