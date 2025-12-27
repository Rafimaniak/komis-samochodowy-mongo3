import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== HASHE BCrypt dla 'haslo123' ===");
        for (int i = 1; i <= 10; i++) {
            String hash = encoder.encode("haslo123");
            System.out.println("Hash " + i + ": " + hash);
        }

        // Aby zweryfikować
        String testHash = encoder.encode("haslo123");
        boolean matches = encoder.matches("haslo123", testHash);
        System.out.println("\nWeryfikacja: hasło 'haslo123' pasuje do hash? " + matches);
    }
}