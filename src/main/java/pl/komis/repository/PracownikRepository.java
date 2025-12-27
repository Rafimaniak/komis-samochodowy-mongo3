package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.komis.model.Pracownik;

@Repository
public interface PracownikRepository extends MongoRepository<Pracownik, String> {
}