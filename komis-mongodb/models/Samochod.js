import mongoose from 'mongoose';

const samochodSchema = new mongoose.Schema({
  marka: { type: String, required: true },
  model: { type: String, required: true },
  rok_produkcji: { type: Number, required: true },
  przebieg: { type: Number, required: true },
  pojemnosc_silnika: Number,
  rodzaj_paliwa: {
    type: String,
    enum: ['Benzyna', 'Diesel', 'LPG', 'Elektryczny', 'Hybryda']
  },
  skrzynia_biegow: {
    type: String,
    enum: ['Manualna', 'Automatyczna']
  },
  kolor: String,
  cena: { type: mongoose.Types.Decimal128, required: true },
  status: {
    type: String,
    enum: ['Dostępny', 'Sprzedany', 'Zarezerwowany'],
    default: 'Dostępny'
  },
  data_dodania: { type: Date, default: Date.now },
  zdjecie_url: String,
  
  // Zagnieżdżone dane
  zarezerwowany_przez: {
    imie: String,
    nazwisko: String,
    email: String
  },
  
  // Tablica serwisów w dokumencie samochodu
  serwisy: [{
    data_serwisu: Date,
    opis_uslugi: String,
    koszt: mongoose.Types.Decimal128,
    pracownik: {
      imie: String,
      nazwisko: String
    }
  }]
}, {
  timestamps: true  // automatycznie doda createdAt i updatedAt
});

export default mongoose.model('Samochod', samochodSchema);