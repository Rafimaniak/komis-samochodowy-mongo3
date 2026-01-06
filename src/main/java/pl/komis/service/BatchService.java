package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pl.komis.model.Samochod;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final MongoTemplate mongoTemplate;

    public void updateCeny(BigDecimal procentPodwyzki) {
        Query query = new Query(Criteria.where("status").is("DOSTEPNY"));
        Update update = new Update().multiply("cena",
                BigDecimal.ONE.add(procentPodwyzki.divide(new BigDecimal("100"))));

        mongoTemplate.updateMulti(query, update, Samochod.class);
    }

    public void archiwizujStareSamochody(int lat) {
        LocalDate threshold = LocalDate.now().minusYears(lat);

        Query query = new Query(Criteria.where("dataDodania").lt(threshold)
                .and("status").is("DOSTEPNY"));

        Update update = new Update().set("status", "ARCHIWALNY");

        mongoTemplate.updateMulti(query, update, Samochod.class);
    }
}
