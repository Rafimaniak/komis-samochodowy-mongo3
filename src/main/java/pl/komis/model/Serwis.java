package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "serwis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Serwis {
    @Id
    private String id;

    @Field("samochod_id")
    private String samochodId;

    @Field("pracownik_id")
    private String pracownikId;

    @Field("opisUslugi")
    private String opisUslugi;

    private BigDecimal koszt;

    @Field("dataSerwisu")
    private LocalDate dataSerwisu;

    // Metody pomocnicze
    public String getStatus() {
        return (koszt == null) ? "ZAREZERWOWANY" : "ZAKOŃCZONY";
    }

    public boolean isZarezerwowany() {
        return koszt == null;
    }

    public boolean isZakonczony() {
        return koszt != null;
    }
}