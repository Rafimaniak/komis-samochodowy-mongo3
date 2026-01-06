package pl.komis.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
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
    private Double cena;
    private String status;
    private LocalDate dataDodania;
    private String zdjecieNazwa;

    // ZMIANA: z @DBRef na String
    @Field("zarezerwowany_przez_klient_id")
    private String zarezerwowanyPrzezKlientId;

    @Field("data_rezerwacji")
    private LocalDate dataRezerwacji;

    @Transient
    public String getZdjecieUrl() {
        if (zdjecieNazwa == null || zdjecieNazwa.isEmpty()) {
            return "/images/samochody/domyslny.jpg";
        }
        return "/images/samochody/" + zdjecieNazwa;
    }

    // Dodane metody pomocnicze - BEZ Optional w metodach!

    @Transient
    public boolean jestDostepny() {
        return "DOSTEPNY".equals(status);
    }

    @Transient
    public boolean jestZarezerwowany() {
        return "ZAREZERWOWANY".equals(status);
    }

    @Transient
    public boolean jestSprzedany() {
        return "SPRZEDANY".equals(status);
    }

    @Transient
    public String getPelnaNazwa() {
        String markaStr = marka != null ? marka : "";
        String modelStr = model != null ? model : "";
        return markaStr + " " + modelStr;
    }

    @Transient
    public String getOpis() {
        StringBuilder sb = new StringBuilder();

        if (rokProdukcji != null) {
            sb.append(rokProdukcji);
        }
        sb.append(", ");

        if (przebieg != null) {
            sb.append(przebieg).append(" km");
        }
        sb.append(", ");

        if (pojemnoscSilnika != null) {
            sb.append(pojemnoscSilnika).append(" L");
        }
        sb.append(", ");

        if (rodzajPaliwa != null) {
            sb.append(rodzajPaliwa);
        }

        return sb.toString();
    }

    // Metoda do bezpiecznego ustawiania ceny
    public void setCenaSafely(Double cena) {
        this.cena = cena != null ? cena : 0.0;
    }

    // Metoda do bezpiecznego ustawiania przebiegu
    public void setPrzebiegSafely(Integer przebieg) {
        this.przebieg = przebieg != null ? przebieg : 0;
    }

    // Metoda do bezpiecznego ustawiania roku produkcji
    public void setRokProdukcjiSafely(Integer rokProdukcji) {
        this.rokProdukcji = rokProdukcji != null ? rokProdukcji : LocalDate.now().getYear();
    }

    // Dodane: Metoda sprawdzająca czy samochód ma zdjęcie
    @Transient
    public boolean maZdjecie() {
        return zdjecieNazwa != null && !zdjecieNazwa.isEmpty();
    }

    // Dodane: Metoda zwracająca rok produkcji jako String (bezpiecznie)
    @Transient
    public String getRokProdukcjiString() {
        return rokProdukcji != null ? rokProdukcji.toString() : "";
    }

    // Dodane: Metoda zwracająca cenę jako String (bezpiecznie)
    @Transient
    public String getCenaString() {
        return cena != null ? cena.toString() + " zł" : "0 zł";
    }
}