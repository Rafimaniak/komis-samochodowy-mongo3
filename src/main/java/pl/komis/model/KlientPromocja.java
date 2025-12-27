package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDate;

@Document(collection = "klienci_promocje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KlientPromocja {

    @Id
    private String id;

    @DBRef
    private Klient klient;

    @DBRef
    private Promocja promocja;

    private LocalDate dataPrzyznania;
}