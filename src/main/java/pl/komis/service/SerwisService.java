package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.model.Serwis;
import pl.komis.repository.SerwisRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SerwisService {

    private final SerwisRepository serwisRepository;

    public List<Serwis> findAll() {
        return serwisRepository.findAll();
    }

    public Optional<Serwis> findById(String id) {
        return serwisRepository.findById(id);
    }

    public Serwis save(Serwis serwis) {
        return serwisRepository.save(serwis);
    }

    public void delete(String id) {
        serwisRepository.deleteById(id);
    }

    public List<Serwis> findByDateRange(LocalDate od, LocalDate do_) {
        return serwisRepository.findByDataSerwisuBetween(od, do_);
    }

    public List<Serwis> findBySamochodId(String samochodId) {
        return serwisRepository.findBySamochodId(samochodId);
    }

    public List<Serwis> findByPracownikId(String pracownikId) {
        return serwisRepository.findByPracownikId(pracownikId);
    }

    public long countReservedServices() {
        return serwisRepository.countReservedServices();
    }

    public long countCompletedServices() {
        return serwisRepository.countCompletedServices();
    }

    public BigDecimal getTotalServiceCost() {
        List<Serwis> completed = serwisRepository.findCompletedServices();
        return completed.stream()
                .map(Serwis::getKoszt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Dodatkowe metody dla kontrolera

    public List<Serwis> findReservedServices() {
        return findAll().stream()
                .filter(s -> s.getKoszt() == null)
                .toList();
    }

    public List<Serwis> findCompletedServices() {
        return findAll().stream()
                .filter(s -> s.getKoszt() != null)
                .toList();
    }
}