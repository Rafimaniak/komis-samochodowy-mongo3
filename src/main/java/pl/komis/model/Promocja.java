package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "promocje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocja {

    @Id
    private String id;

    private String nazwa;
    private String rodzaj;
    private Double wartosc;
    private String opis;
    private LocalDate dataRozpoczecia;
    private LocalDate dataZakonczenia;
    private Boolean aktywna;
}