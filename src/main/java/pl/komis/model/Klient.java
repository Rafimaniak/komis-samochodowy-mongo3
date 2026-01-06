package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Document(collection = "klienci")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Klient {
    @Id
    private String id;

    private String imie;
    private String nazwisko;

    @Indexed(unique = true)
    private String email;

    private String telefon;
    private Integer liczbaZakupow = 0;
    private Double procentPremii = 0.0;
    private Double saldoPremii = 0.0;
    private Double totalWydane = 0.0;

    public boolean mozeWykorzystacSaldo(Double kwota) {
        return saldoPremii != null && saldoPremii.compareTo(kwota) >= 0;
    }

    public void uzyjSalda(Double kwota) {
        if (mozeWykorzystacSaldo(kwota)) {
            saldoPremii = saldoPremii - kwota;
        }
    }
}