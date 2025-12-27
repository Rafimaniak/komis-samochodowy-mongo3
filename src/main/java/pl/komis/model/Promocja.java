// Promocja.java
package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDate;

@Document(collection = "promocje")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocja {
    @Id
    private String id;

    private String nazwa;
    private String opis;
    private String rodzaj;
    private Double wartosc;
    private Boolean aktywna;

    @Field("dataRozpoczecia")
    private LocalDate dataRozpoczecia;

    @Field("dataZakonczenia")
    private LocalDate dataZakonczenia;
}