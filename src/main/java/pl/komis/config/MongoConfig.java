package pl.komis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

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

        // Dodaj konwertery
        converter.setCustomConversions(customConversions());

        // Usuń pole _class z dokumentów
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        converter.afterPropertiesSet();

        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateToDateConverter());
        converters.add(new DateToLocalDateConverter());
        return new MongoCustomConversions(converters);
    }

    // Konwerter LocalDate -> Date (do zapisu do MongoDB)
    public static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
        @Override
        public Date convert(LocalDate source) {
            if (source == null) {
                return null;
            }
            return Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }

    // Konwerter Date -> LocalDate (do odczytu z MongoDB)
    public static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
        @Override
        public LocalDate convert(Date source) {
            if (source == null) {
                return null;
            }
            return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }
}