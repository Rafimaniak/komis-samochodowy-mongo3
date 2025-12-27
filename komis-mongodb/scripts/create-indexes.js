// Połącz się z MongoDB
// mongo komis

// Utwórz indeksy
db.samochody.createIndex({ marka: 1 });
db.samochody.createIndex({ model: 1 });
db.samochody.createIndex({ status: 1 });
db.samochody.createIndex({ cena: 1 });
db.samochody.createIndex({ rokProdukcji: 1 });
db.samochody.createIndex({ marka: 1, model: 1, status: 1 });

db.klienci.createIndex({ email: 1 }, { unique: true });
db.klienci.createIndex({ nazwisko: 1, imie: 1 });

db.zakupy.createIndex({ "klient.$id": 1 });
db.zakupy.createIndex({ "samochod.$id": 1 });
db.zakupy.createIndex({ dataZakupu: -1 });

db.users.createIndex({ username: 1 }, { unique: true });
db.users.createIndex({ email: 1 }, { unique: true });