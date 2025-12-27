// migrate-postgres-to-mongo.js
const { Client } = require('pg');
const { MongoClient } = require('mongodb');

async function migrate() {
  console.log('🔄 Rozpoczynam migrację z PostgreSQL do MongoDB 8.2...');
  
  // Konfiguracja PostgreSQL
  const pgConfig = {
    host: 'localhost',
    port: 5433,
    database: 'komis',
    user: 'postgres',
    password: 'student' // ZMIEŃ NA SWOJE HASŁO
  };
  
  const pgClient = new Client(pgConfig);
  
  try {
    // Połącz z PostgreSQL
    await pgClient.connect();
    console.log('✅ Połączono z PostgreSQL');
    
    // Połącz z MongoDB
    const mongoClient = new MongoClient('mongodb://localhost:27017');
    await mongoClient.connect();
    const db = mongoClient.db('komis');
    console.log('✅ Połączono z MongoDB 8.2');
    
    // 1. Migracja samochodów
    console.log('\n🚗 Migracja samochodów...');
    const samochodyResult = await pgClient.query('SELECT * FROM samochody');
    const samochodyCollection = db.collection('samochody');
    
    if (samochodyResult.rows.length > 0) {
      const samochody = samochodyResult.rows.map(car => ({
        marka: car.marka,
        model: car.model,
        rok_produkcji: car.rok_produkcji,
        przebieg: car.przebieg,
        pojemnosc_silnika: car.pojemnosc_silnika,
        rodzaj_paliwa: car.rodzaj_paliwa,
        skrzynia_biegow: car.skrzynia_biegow,
        kolor: car.kolor,
        cena: car.cena,
        status: car.status,
        data_dodania: car.data_dodania,
        zdjecie_url: car.zdjecie_url || '',
        migrated_at: new Date()
      }));
      
      await samochodyCollection.deleteMany({});
      await samochodyCollection.insertMany(samochody);
      console.log(`✅ Przeniesiono ${samochody.length} samochodów`);
    }
    
    // 2. Migracja klientów
    console.log('\n👥 Migracja klientów...');
    const klienciResult = await pgClient.query('SELECT * FROM klient');
    const klienciCollection = db.collection('klienci');
    
    if (klienciResult.rows.length > 0) {
      const klienci = klienciResult.rows.map(klient => ({
        imie: klient.imie,
        nazwisko: klient.nazwisko,
        email: klient.email,
        telefon: klient.telefon,
        liczba_zakupow: klient.liczba_zakupow || 0,
        procent_premii: klient.procent_premii || 0,
        saldo_premii: klient.saldo_premii || 0,
        total_wydane: klient.total_wydane || 0,
        migrated_at: new Date()
      }));
      
      await klienciCollection.deleteMany({});
      await klienciCollection.insertMany(klienci);
      console.log(`✅ Przeniesiono ${klienci.length} klientów`);
    }
    
    // 3. Migracja pracowników
    console.log('\n👨‍💼 Migracja pracowników...');
    const pracownicyResult = await pgClient.query('SELECT * FROM pracownicy');
    const pracownicyCollection = db.collection('pracownicy');
    
    if (pracownicyResult.rows.length > 0) {
      const pracownicy = pracownicyResult.rows.map(p => ({
        imie: p.imie,
        nazwisko: p.nazwisko,
        email: p.email,
        telefon: p.telefon,
        stanowisko: p.stanowisko,
        data_zatrudnienia: p.data_zatrudnienia,
        migrated_at: new Date()
      }));
      
      await pracownicyCollection.deleteMany({});
      await pracownicyCollection.insertMany(pracownicy);
      console.log(`✅ Przeniesiono ${pracownicy.length} pracowników`);
    }
    
    console.log('\n🎉 MIGRACJA ZAKOŃCZONA SUKCESEM!');
    console.log('===================================');
    console.log(`Samochody: ${samochodyResult.rows.length}`);
    console.log(`Klienci: ${klienciResult.rows.length}`);
    console.log(`Pracownicy: ${pracownicyResult.rows.length}`);
    console.log('===================================');
    
  } catch (error) {
    console.error('❌ Błąd podczas migracji:', error.message);
  } finally {
    await pgClient.end();
    process.exit(0);
  }
}

// Uruchom migrację jeśli uruchomiony bezpośrednio
if (require.main === module) {
  migrate();
}

module.exports = { migrate };