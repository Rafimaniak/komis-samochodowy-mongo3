package pl.komis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "pl.komis.repository")
public class MongoConfig {

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory,
                                       MongoMappingContext context) {

        MappingMongoConverter converter = new MappingMongoConverter(
                new DefaultDbRefResolver(mongoDatabaseFactory), context);

        converter.setCustomConversions(customConversions());
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.afterPropertiesSet();

        // Utwórz indeksy
        createIndexes(new MongoTemplate(mongoDatabaseFactory, converter));

        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateToDateConverter());
        converters.add(new DateToLocalDateConverter());
        return new MongoCustomConversions(converters);
    }

    private void createIndexes(MongoTemplate mongoTemplate) {
        try {
            // Indeksy dla samochodów
            IndexOperations samochodOps = mongoTemplate.indexOps("samochody");
            samochodOps.ensureIndex(new Index().on("marka", Sort.Direction.ASC));
            samochodOps.ensureIndex(new Index().on("status", Sort.Direction.ASC));
            samochodOps.ensureIndex(new Index().on("cena", Sort.Direction.ASC));

            // NIE TWÓRZ indeksu dla email - już jest w Klient.java przez @Indexed
            // IndexOperations klientOps = mongoTemplate.indexOps("klienci");
            // klientOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());

            // Indeksy dla zakupów
            IndexOperations zakupOps = mongoTemplate.indexOps("zakupy");
            zakupOps.ensureIndex(new Index().on("klient_id", Sort.Direction.ASC));
            zakupOps.ensureIndex(new Index().on("dataZakupu", Sort.Direction.DESC));

            System.out.println("Indeksy MongoDB utworzone pomyślnie");
        } catch (Exception e) {
            System.err.println("Ostrzeżenie przy tworzeniu indeksów: " + e.getMessage());
            // Nie przerywaj - aplikacja może działać
        }
    }

    public static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
        @Override
        public Date convert(LocalDate source) {
            if (source == null) return null;
            return Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }

    public static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
        @Override
        public LocalDate convert(Date source) {
            if (source == null) return null;
            return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }
}