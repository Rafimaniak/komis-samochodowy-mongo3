package pl.komis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.komis.model.Promocja;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromocjaRepository extends MongoRepository<Promocja, String> {

    List<Promocja> findByRodzaj(String rodzaj);

    List<Promocja> findByAktywna(boolean aktywna);

    List<Promocja> findByAktywnaTrue();

    List<Promocja> findByDataRozpoczeciaBeforeAndDataZakonczeniaAfter(LocalDate data1, LocalDate data2);

    default List<Promocja> findByAktywnaTrueAndDataRozpoczeciaBeforeAndDataZakonczeniaAfter(LocalDate date1, LocalDate date2) {
        return findAll().stream()
                .filter(p -> p.getAktywna() != null && p.getAktywna())
                .filter(p -> p.getDataRozpoczecia() != null && p.getDataRozpoczecia().isBefore(date1))
                .filter(p -> p.getDataZakonczenia() != null && p.getDataZakonczenia().isAfter(date2))
                .toList();
    }
}