package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pl.komis.model.KlientPromocja;

import java.util.List;

@Repository
public interface KlientPromocjaRepository extends MongoRepository<KlientPromocja, String> {

    @Query("{'klient.$id': ?0}")
    List<KlientPromocja> findByKlientId(String klientId);

    @Query("{'promocja.$id': ?0}")
    List<KlientPromocja> findByPromocjaId(String promocjaId);

    @Query("{'klient.$id': ?0, 'promocja.$id': ?1}")
    KlientPromocja findByKlientIdAndPromocjaId(String klientId, String promocjaId);

    @Query("{'klient.$id': ?0, 'promocja.$id': ?1}")
    boolean existsByKlientIdAndPromocjaId(String klientId, String promocjaId);
}