package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.model.Klient;
import pl.komis.repository.KlientRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KlientService {

    private final KlientRepository klientRepository;

    public List<Klient> findAll() {
        return klientRepository.findAll();
    }

    public Optional<Klient> findById(String id) {
        return klientRepository.findKlientById(id);
    }

    public Optional<Klient> findByEmail(String email) {
        return klientRepository.findByEmail(email);
    }

    public Klient save(Klient klient) {
        return klientRepository.save(klient);
    }

    public void delete(String id) {
        klientRepository.deleteById(id);
    }

    public Integer getLiczbaZakupow(String klientId) {
        return klientRepository.findKlientById(klientId)
                .map(Klient::getLiczbaZakupow)
                .orElse(0);
    }

    public Double getSaldoPremii(String klientId) {
        return klientRepository.findKlientById(klientId)
                .map(Klient::getSaldoPremii)
                .orElse(0.0);
    }

    public Double getProcentPremii(String klientId) {
        return klientRepository.findKlientById(klientId)
                .map(Klient::getProcentPremii)
                .orElse(0.0);
    }

    public Double getTotalWydane(String klientId) {
        return klientRepository.findKlientById(klientId)
                .map(Klient::getTotalWydane)
                .orElse(0.0);
    }

    public void naprawSaldo(String klientId) {
        Klient klient = findById(klientId)
                .orElseThrow(() -> new RuntimeException("Klient nie znaleziony"));
        // Oblicz saldo na podstawie zakupów
        save(klient);
    }
    public boolean existsByEmail(String email) {
        return klientRepository.existsByEmail(email);
    }

    public List<Klient> findAllByEmail(String email) {
        return klientRepository.findAllByEmail(email);
    }
}