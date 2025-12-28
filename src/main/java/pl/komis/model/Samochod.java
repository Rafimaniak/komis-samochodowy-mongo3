package pl.komis.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "samochody")
@Data
public class Samochod {

    @Id
    private String id;

    private String marka;
    private String model;
    private Integer rokProdukcji;
    private Integer przebieg;
    private Double pojemnoscSilnika;
    private String rodzajPaliwa;
    private String skrzyniaBiegow;
    private String kolor;
    private BigDecimal cena;
    private String status;
    private LocalDate dataDodania;

    // Przechowujemy tylko nazwę pliku
    private String zdjecieNazwa;

    @DBRef
    private Klient zarezerwowanyPrzez;

    private LocalDate dataRezerwacji;

    @Transient
    public String getZdjecieUrl() {
        if (zdjecieNazwa == null || zdjecieNazwa.isEmpty()) {
            return "/images/samochody/domyslny.jpg";
        }
        return "/images/samochody/" + zdjecieNazwa;
    }
}