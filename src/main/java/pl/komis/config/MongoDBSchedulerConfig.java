package pl.komis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import pl.komis.service.MongoDBFunctionService;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MongoDBSchedulerConfig {

    private final MongoDBFunctionService mongoDBFunctionService;
    private final Environment env; // Opcjonalnie, jeśli używasz Environment

    @Value("${mongodb.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${mongodb.scheduler.cron.cleanup:0 0 2 * * ?}")
    private String cleanupCron;

    // Metoda 1: Używając @Scheduled z dynamicznym cronem
    @Scheduled(cron = "${mongodb.scheduler.cron.cleanup:0 0 2 * * ?}")
    public void scheduledCleanup() {
        if (schedulerEnabled) {
            log.info("Rozpoczynam zaplanowane czyszczenie przeterminowanych rezerwacji...");
            String result = mongoDBFunctionService.czyscPrzeterminowaneRezerwacje();
            log.info(result);
        }
    }

    // Metoda 2: Ręczne wywołanie (jeśli potrzebujesz)
    public void manualCleanup() {
        log.info("Rozpoczynam ręczne czyszczenie przeterminowanych rezerwacji...");
        String result = mongoDBFunctionService.czyscPrzeterminowaneRezerwacje();
        log.info(result);
    }

    // Metoda pomocnicza do pobierania właściwości
    public String getProperty(String key) {
        return env.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }
}