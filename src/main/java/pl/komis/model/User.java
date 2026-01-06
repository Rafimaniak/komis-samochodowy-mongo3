package pl.komis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    private String username;
    private String email;
    private String password;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;

    // ZMIANA: z @DBRef na String
    @Field("klient_id")
    private String klientId;

    // Transient - ładowane na żądanie
    // private transient Klient klient;
}