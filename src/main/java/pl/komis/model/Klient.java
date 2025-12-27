package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.math.BigDecimal;
import java.util.List;

@Document(collection = "klienci")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Klient {

    @Id
    private String id;

    private String imie;
    private String nazwisko;
    private String email;
    private String telefon;

    @DBRef
    private List<Zakup> zakupy;

    private Integer liczbaZakupow = 0;
    private BigDecimal procentPremii = BigDecimal.ZERO;
    private BigDecimal saldoPremii = BigDecimal.ZERO;
    private BigDecimal totalWydane = BigDecimal.ZERO;

    public boolean mozeWykorzystacSaldo(BigDecimal kwota) {
        return saldoPremii != null && saldoPremii.compareTo(kwota) >= 0;
    }

    public void uzyjSalda(BigDecimal kwota) {
        if (mozeWykorzystacSaldo(kwota)) {
            saldoPremii = saldoPremii.subtract(kwota);
        }
    }
}