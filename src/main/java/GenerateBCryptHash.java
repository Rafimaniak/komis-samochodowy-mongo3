import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash1 = "$2a$10$RQzLGZh0ien2Sc4PQRMXOODpClf5grhfJHh0tCl/.6CISYeWkvolW";
        String hash2 = "$2a$10$ZrBWFCKfzxdPO/z3e4JIVOKXKAe7G2gd5YQg8hNqMq271Y5bwcoXU0";

        System.out.println("Hash1 matches 'admin': " + encoder.matches("admin", hash1));
        System.out.println("Hash2 matches 'admin': " + encoder.matches("admin", hash2));

        // Wygeneruj nowy hash dla "admin"
        String hash3 = encoder.encode("admin");
        System.out.println("Nowy hash: " + hash3);
        System.out.println("Hash3 matches 'admin': " + encoder.matches("admin", hash3));
    }
}