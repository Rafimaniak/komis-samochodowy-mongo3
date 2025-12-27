package pl.komis.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
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
    private String zdjecieUrl;

    @DBRef
    private Klient zarezerwowanyPrzez;

    private LocalDate dataRezerwacji;
}