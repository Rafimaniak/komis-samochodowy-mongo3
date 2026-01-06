package pl.komis.dto;

import lombok.Data;

@Data
public class SearchCriteria {
    private String marka;
    private String model;
    private Double minCena;      // ZMIANA: z minCena na minCena
    private Double maxCena;      // ZMIANA: z maxCena na maxCena
    private Integer minRok;      // ZMIANA: z minRok na minRok
    private Integer maxRok;      // ZMIANA: z maxRok na maxRok
    private String status;
    private Integer minPrzebieg; // ZMIANA: z minPrzebieg na minPrzebieg
    private Integer maxPrzebieg; // ZMIANA: z maxPrzebieg na maxPrzebieg
    private String rodzajPaliwa;
    private String skrzyniaBiegow;
    private String kolor;
    private Integer limit;

    public SearchCriteria() {}

    public SearchCriteria(String marka, String model, Double minCena, Double maxCena) {
        this.marka = marka;
        this.model = model;
        this.minCena = minCena;
        this.maxCena = maxCena;
    }
}