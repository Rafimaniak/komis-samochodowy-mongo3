import mongoose from 'mongoose';

const klientSchema = new mongoose.Schema({
  imie: { type: String, required: true },
  nazwisko: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  telefon: String,
  typ_klienta: {
    type: String,
    enum: ['Kupujący', 'Sprzedający', 'Oba'],
    default: 'Kupujący'
  },
  liczba_zakupow: { type: Number, default: 0 },
  procent_premii: { type: mongoose.Types.Decimal128, default: 0 },
  saldo_premii: { type: mongoose.Types.Decimal128, default: 0 },
  total_wydane: { type: mongoose.Types.Decimal128, default: 0 },
  
  // Zagnieżdżona lista zakupów
  zakupy: [{
    data_zakupu: { type: Date, default: Date.now },
    samochod: {
      marka: String,
      model: String,
      cena_bazowa: mongoose.Types.Decimal128
    },
    cena_zakupu: mongoose.Types.Decimal128,
    zastosowany_rabat: mongoose.Types.Decimal128,
    naliczona_premia: mongoose.Types.Decimal128,
    wykorzystane_saldo: mongoose.Types.Decimal128
  }],
  
  promocje: [{
    nazwa: String,
    rodzaj: String,
    wartosc: Number,
    data_przyznania: Date,
    aktywna: Boolean
  }]
}, {
  timestamps: true
});

export default mongoose.model('Klient', klientSchema);