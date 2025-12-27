package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pracownicy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pracownik {

    @Id
    private String id;

    private String imie;
    private String nazwisko;
    private String stanowisko;
    private String telefon;
    private String email;
    private LocalDate dataZatrudnienia;

    @DBRef
    private List<Serwis> serwisy = new ArrayList<>();

    @DBRef
    private List<Zakup> zakupy = new ArrayList<>();
}