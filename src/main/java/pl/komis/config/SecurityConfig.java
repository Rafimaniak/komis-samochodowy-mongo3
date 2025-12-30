package pl.komis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import pl.komis.model.User;
import pl.komis.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, @Lazy pl.komis.service.UserService userService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/api/**",
                                "/samochody", "/samochody/szczegoly", "/register",
                                "/search", "/search/**", "/login", "/debug/**",
                                "/favicon.ico", "/webjars/**", "/static/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/klienci", "/pracownicy", "/serwis", "/serwis/**").hasRole("ADMIN")
                        .requestMatchers("/samochody/nowy", "/samochody/edytuj/**",
                                "/samochody/zapisz", "/samochody/usun/**").hasRole("ADMIN")
                        .requestMatchers("/zakupy").hasRole("ADMIN")
                        .requestMatchers("/zakupy/moje").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/samochody", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .userDetailsService(userService)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CommandLineRunner createDefaultUsers() {
        return args -> {
            PasswordEncoder encoder = passwordEncoder();

            if (userRepository.count() == 0) {
                System.out.println("=== TWORZENIE DOMYŚLNYCH UŻYTKOWNIKÓW ===");

                User admin = User.builder()
                        .username("admin")
                        .email("admin@komis.pl")
                        .password(encoder.encode("admin"))
                        .role("ADMIN")
                        .enabled(true)
                        .build();
                userRepository.save(admin);
                System.out.println("Utworzono admina: admin / admin");

                User user = User.builder()
                        .username("user")
                        .email("user@komis.pl")
                        .password(encoder.encode("user"))
                        .role("USER")
                        .enabled(true)
                        .build();
                userRepository.save(user);
                System.out.println("Utworzono usera: user / user");

                System.out.println("=== UTWORZONO " + userRepository.count() + " UŻYTKOWNIKÓW ===");
            } else {
                System.out.println("Użytkownicy już istnieją w bazie (" + userRepository.count() + ")");
            }
        };
    }
}