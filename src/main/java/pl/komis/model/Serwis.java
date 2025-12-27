package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "serwis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Serwis {

    @Id
    private String id;

    @DBRef
    private Samochod samochod;

    @DBRef
    private Pracownik pracownik;

    private String opisUslugi;
    private BigDecimal koszt;
    private LocalDate dataSerwisu;

    public String getStatus() {
        if (koszt == null) {
            return "ZAREZERWOWANY";
        } else {
            return "ZAKOŃCZONY";
        }
    }

    public boolean isZarezerwowany() {
        return koszt == null;
    }

    public boolean isZakonczony() {
        return koszt != null;
    }
}