package pl.komis.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "zakupy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Zakup {
    @Id
    private String id;

    @Field("samochod_id")
    private String samochodId;

    @Field("klient_id")
    private String klientId;

    @Field("pracownik_id")
    private String pracownikId;

    @Field("samochod_marka")
    private String samochodMarka;

    @Field("samochod_model")
    private String samochodModel;

    @Field("klient_imie_nazwisko")
    private String klientImieNazwisko;

    @Field("pracownik_imie_nazwisko")
    private String pracownikImieNazwisko;

    private LocalDate dataZakupu;
    private Double cenaZakupu;
    private Double cenaBazowa;

    @Builder.Default
    private Double zastosowanyRabat = 0.0;

    @Builder.Default
    private Double naliczonaPremia = 0.0;

    @Builder.Default
    private Double wykorzystaneSaldo = 0.0;

    public Double getZaoszczedzonaKwota() {
        if (cenaBazowa != null && cenaZakupu != null) {
            return cenaBazowa-cenaZakupu;
        }
        return 0.0;
    }

    public boolean czyWykorzystanoSaldo() {
        return wykorzystaneSaldo != null && wykorzystaneSaldo.compareTo(0.0) > 0;
    }
}