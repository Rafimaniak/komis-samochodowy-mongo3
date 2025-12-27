package pl.komis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Document(collection = "klienci_promocje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KlientPromocja {
    @Id
    private String id;

    @Field("klient_id")
    private String klientId;

    @Field("promocja_id")
    private String promocjaId;

    @Field("dataPrzyznania")
    private LocalDate dataPrzyznania;

    private Boolean wykorzystana;
}