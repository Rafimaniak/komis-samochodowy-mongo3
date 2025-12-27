package pl.komis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.komis.model.Pracownik;
import pl.komis.repository.PracownikRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracownikService {

    private final PracownikRepository pracownikRepository;

    public List<Pracownik> findAll() {
        return pracownikRepository.findAll();
    }

    public Optional<Pracownik> findById(String id) {
        return pracownikRepository.findById(id);
    }

    public Pracownik save(Pracownik pracownik) {
        return pracownikRepository.save(pracownik);
    }

    public void delete(String id) {
        pracownikRepository.deleteById(id);
    }
}