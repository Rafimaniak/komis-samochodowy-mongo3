package pl.komis.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
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

    @DBRef
    private Samochod samochod;

    @DBRef
    private Klient klient;

    @DBRef
    private Pracownik pracownik;

    private LocalDate dataZakupu;
    private BigDecimal cenaZakupu;
    private BigDecimal cenaBazowa;

    @Builder.Default
    private BigDecimal zastosowanyRabat = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal naliczonaPremia = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal wykorzystaneSaldo = BigDecimal.ZERO;

    public BigDecimal getZaoszczedzonaKwota() {
        if (cenaBazowa != null && cenaZakupu != null) {
            return cenaBazowa.subtract(cenaZakupu);
        }
        return BigDecimal.ZERO;
    }

    public boolean czyWykorzystanoSaldo() {
        return wykorzystaneSaldo != null && wykorzystaneSaldo.compareTo(BigDecimal.ZERO) > 0;
    }
}