package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.komis.model.Samochod;
import pl.komis.service.SamochodService;
import pl.komis.dto.SearchCriteria;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SamochodService samochodService;

    @GetMapping
    public String searchPage(Model model) {
        model.addAttribute("marki", samochodService.findAllMarki());
        model.addAttribute("searchCriteria", new SearchCriteria());
        return "search/form";  // ZMIANA: Zwraca search/form zamiast samochody/lista
    }

    @PostMapping
    public String search(@ModelAttribute SearchCriteria criteria, Model model) {
        // Użyj metody z SamochodService
        List<Samochod> wyniki = samochodService.searchCars(criteria);

        model.addAttribute("samochody", wyniki);
        model.addAttribute("marki", samochodService.findAllMarki());
        model.addAttribute("searchCriteria", criteria);
        model.addAttribute("resultsCount", wyniki != null ? wyniki.size() : 0);  // DODAJ: resultsCount

        return "search/results";
    }

    @GetMapping("/simple")
    public String searchSimple(
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            Model modelView) {

        List<Samochod> wyniki = samochodService.searchCarsSimple(marka, model, status);

        // Utwórz obiekt SearchCriteria dla szablonu
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setMarka(marka);
        searchCriteria.setModel(model);
        searchCriteria.setStatus(status);

        modelView.addAttribute("samochody", wyniki);
        modelView.addAttribute("marki", samochodService.findAllMarki());
        modelView.addAttribute("resultsCount", wyniki != null ? wyniki.size() : 0);
        modelView.addAttribute("searchCriteria", searchCriteria); // DODAJ TO!

        return "search/results";
    }
    @GetMapping("/quick")
    public String searchQuick(
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            Model modelView) {

        List<Samochod> wyniki = samochodService.searchCarsSimple(marka, model, status);

        modelView.addAttribute("samochody", wyniki);
        modelView.addAttribute("marki", samochodService.findAllMarki());
        modelView.addAttribute("resultsCount", wyniki != null ? wyniki.size() : 0);
        modelView.addAttribute("searchMarka", marka);
        modelView.addAttribute("searchModel", model);
        modelView.addAttribute("searchStatus", status);

        return "search/quick-results";
    }
}