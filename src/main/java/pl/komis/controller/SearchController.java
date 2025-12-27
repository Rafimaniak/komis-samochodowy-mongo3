// src\main\java\pl\komis\controller\SearchController.java

package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.komis.model.Samochod;
import pl.komis.service.SamochodService;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SamochodService samochodService;

    @GetMapping
    public String showSearchForm(Model model) {
        model.addAttribute("searchCriteria", new SamochodService.SearchCriteria());
        model.addAttribute("marki", samochodService.findAllMarki());
        model.addAttribute("tytul", "Zaawansowane wyszukiwanie");
        return "search/form";
    }

    @PostMapping
    public String searchCars(@ModelAttribute SamochodService.SearchCriteria criteria, Model model) {
        List<Samochod> results = samochodService.searchCars(criteria);

        // DEBUG
        System.out.println("DEBUG Zaawansowane wyszukiwanie:");
        System.out.println("DEBUG - Marka: " + criteria.getMarka());
        System.out.println("DEBUG - Model: " + criteria.getModel());
        System.out.println("DEBUG - Status: " + criteria.getStatus());
        System.out.println("DEBUG - Min rok: " + criteria.getMinRok());
        System.out.println("DEBUG - Max rok: " + criteria.getMaxRok());
        System.out.println("DEBUG - Wyniki: " + results.size() + " samochodów");

        model.addAttribute("searchCriteria", criteria);
        model.addAttribute("samochody", results);
        model.addAttribute("marki", samochodService.findAllMarki());
        model.addAttribute("tytul", "Wyniki wyszukiwania");
        model.addAttribute("resultsCount", results.size());

        return "search/results";
    }

    // DODAJ TĘ METODĘ DLA WYSZUKIWANIA GET
    @GetMapping("/results")
    public String searchCarsGet(
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minRok,
            @RequestParam(required = false) Integer maxRok,
            @RequestParam(required = false) Integer minPrzebieg,
            @RequestParam(required = false) Integer maxPrzebieg,
            @RequestParam(required = false) String rodzajPaliwa,
            Model modelAttr) {

        SamochodService.SearchCriteria criteria = new SamochodService.SearchCriteria();
        criteria.setMarka(marka);
        criteria.setModel(model);
        criteria.setStatus(status);
        criteria.setMinRok(minRok);
        criteria.setMaxRok(maxRok);
        criteria.setMinPrzebieg(minPrzebieg);
        criteria.setMaxPrzebieg(maxPrzebieg);
        criteria.setRodzajPaliwa(rodzajPaliwa);

        List<Samochod> results = samochodService.searchCars(criteria);

        modelAttr.addAttribute("searchCriteria", criteria);
        modelAttr.addAttribute("samochody", results);
        modelAttr.addAttribute("marki", samochodService.findAllMarki());
        modelAttr.addAttribute("tytul", "Wyniki wyszukiwania");
        modelAttr.addAttribute("resultsCount", results.size());

        return "search/results";
    }

    @GetMapping("/quick")
    public String quickSearch(
            @RequestParam(required = false) String marka,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            Model modelAttr) {

        List<Samochod> results = samochodService.searchCarsSimple(marka, model, status);

        modelAttr.addAttribute("samochody", results);
        modelAttr.addAttribute("marki", samochodService.findAllMarki());
        modelAttr.addAttribute("tytul", "Szybkie wyszukiwanie");
        modelAttr.addAttribute("resultsCount", results.size());
        modelAttr.addAttribute("searchMarka", marka);
        modelAttr.addAttribute("searchModel", model);
        modelAttr.addAttribute("searchStatus", status);

        return "search/quick-results";
    }
}