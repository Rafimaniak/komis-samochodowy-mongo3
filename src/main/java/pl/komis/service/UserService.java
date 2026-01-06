package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.komis.model.Klient;
import pl.komis.model.User;
import pl.komis.repository.KlientRepository;
import pl.komis.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final KlientRepository klientRepository;
    private final PasswordEncoder passwordEncoder;
    private final KlientService klientService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(!user.getEnabled())
                .build();
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User saveUser(User user) {
        // Jeśli hasło nie jest zakodowane w BCrypt, zakoduj je
        if (user.getPassword() != null &&
                !user.getPassword().startsWith("$2a$") &&
                !user.getPassword().startsWith("$2b$") &&
                !user.getPassword().startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Ustaw createdAt jeśli nie ustawione
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }
    @Transactional(readOnly = true)
    public List<User> findByKlientId(String klientId) {
        return userRepository.findByKlientId(klientId);
    }

    @Transactional
    public User updateUser(User user) {
        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("Użytkownik nie istnieje: " + user.getId());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Użytkownik nie istnieje: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(String userId, String newPassword) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public boolean verifyPassword(String userId, String rawPassword) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional
    public User createSimpleUser(String username, String email, String password) {
        if (usernameExists(username)) {
            throw new RuntimeException("Użytkownik już istnieje: " + username);
        }

        if (emailExists(email)) {
            throw new RuntimeException("Email już istnieje: " + email);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))  // Hasło jest kodowane tutaj
                .role("USER")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User createUserByAdmin(User user, String password, String role) {
        if (usernameExists(user.getUsername())) {
            throw new RuntimeException("Użytkownik już istnieje: " + user.getUsername());
        }
        if (emailExists(user.getEmail())) {
            throw new RuntimeException("Email już istnieje: " + user.getEmail());
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(user.getEnabled() != null ? user.getEnabled() : true);
        user.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void toggleUserStatus(String id) {
        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(String id) {
        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(String id) {
        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByRole(String role) {
        return userRepository.countByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers();
    }

    @Transactional(readOnly = true)
    public List<User> findInactiveUsers() {
        return userRepository.findInactiveUsers();
    }

    @Transactional
    public Klient ensureUserHasKlient(String userId) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        // Jeśli użytkownik już ma klientId, sprawdź czy klient istnieje
        if (user.getKlientId() != null) {
            Optional<Klient> existingKlient = klientRepository.findById(user.getKlientId());
            if (existingKlient.isPresent()) {
                return existingKlient.get();
            }
        }

        // Sprawdź czy istnieje klient o emailu użytkownika
        Optional<Klient> klientByEmail = klientRepository.findByEmail(user.getEmail());
        Klient klient;

        if (klientByEmail.isPresent()) {
            klient = klientByEmail.get();
        } else {
            // Utwórz nowego klienta
            klient = new Klient();
            klient.setImie(extractFirstName(user.getUsername()));
            klient.setNazwisko(extractLastName(user.getUsername()));
            klient.setEmail(user.getEmail());
            klient.setTelefon("000000000");
            klient.setLiczbaZakupow(0);
            klient.setProcentPremii(0.0);
            klient.setSaldoPremii(0.0);
            klient.setTotalWydane(0.0);

            klient = klientRepository.save(klient);
        }

        // Przypisz klientId do użytkownika
        user.setKlientId(klient.getId());
        userRepository.save(user);

        return klient;
    }

    private String extractFirstName(String username) {
        if (username == null || username.isEmpty()) return "Użytkownik";
        String[] parts = username.split("\\.");
        if (parts.length > 0) return capitalize(parts[0]);
        return capitalize(username);
    }

    private String extractLastName(String username) {
        if (username == null || username.isEmpty()) return "Klient";
        String[] parts = username.split("\\.");
        if (parts.length > 1) return capitalize(parts[1]);
        return "Klient";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Transactional
    public String checkAllUsersPasswords() {
        StringBuilder result = new StringBuilder();
        result.append("=== SPRAWDZANIE HASEŁ UŻYTKOWNIKÓW ===<br>");

        List<User> allUsers = findAllUsers();
        for (User user : allUsers) {
            String password = user.getPassword();
            boolean isBCrypt = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");

            result.append("Użytkownik: ").append(user.getUsername())
                    .append(" | ID: ").append(user.getId())
                    .append(" | Hasło: ").append(password.substring(0, Math.min(20, password.length()))).append("...")
                    .append(" | BCrypt: ").append(isBCrypt ? "TAK" : "NIE")
                    .append("<br>");
        }

        return result.toString();
    }

    @Transactional
    public void fixNonBCryptPasswords() {
        List<User> allUsers = findAllUsers();
        int fixedCount = 0;

        for (User user : allUsers) {
            String password = user.getPassword();
            boolean isBCrypt = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");

            if (!isBCrypt) {
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                userRepository.save(user);
                fixedCount++;
                System.out.println("Naprawiono hasło dla użytkownika: " + user.getUsername());
            }
        }

        System.out.println("Naprawiono " + fixedCount + " haseł");
    }

    @Transactional
    public void changeUserRole(String userId, String newRole) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(String userId) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        return "ADMIN".equals(user.getRole());
    }

    @Transactional(readOnly = true)
    public boolean isUser(String userId) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        return "USER".equals(user.getRole());
    }

    @Transactional
    public void resetAllPasswordsToDefault() {
        List<User> allUsers = findAllUsers();

        for (User user : allUsers) {
            String defaultPassword = "default123";
            String encodedPassword = passwordEncoder.encode(defaultPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
        }

        System.out.println("Zresetowano hasła dla " + allUsers.size() + " użytkowników");
    }

    @Transactional
    public void disableAllUsers() {
        List<User> allUsers = findAllUsers();

        for (User user : allUsers) {
            user.setEnabled(false);
            userRepository.save(user);
        }

        System.out.println("Dezaktywowano " + allUsers.size() + " użytkowników");
    }

    @Transactional
    public void enableAllUsers() {
        List<User> allUsers = findAllUsers();

        for (User user : allUsers) {
            user.setEnabled(true);
            userRepository.save(user);
        }

        System.out.println("Aktywowano " + allUsers.size() + " użytkowników");
    }
}