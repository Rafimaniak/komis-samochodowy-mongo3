// server.js - Z FUNKCJĄ EKSPORTU
const express = require('express');
const { MongoClient } = require('mongodb');
const fs = require('fs').promises;
const path = require('path');
const archiver = require('archiver');

const app = express();
const PORT = 3001;

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Strona główna
app.get('/', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017', {
      serverSelectionTimeoutMS: 3000
    });
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    await client.close();
    
    res.send(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Komis MongoDB - Eksport danych</title>
        <style>
          body { font-family: Arial; padding: 20px; background: #f0f0f0; }
          .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
          .success { color: green; font-weight: bold; }
          .error { color: red; }
          pre { background: #f5f5f5; padding: 15px; border-radius: 5px; overflow: auto; }
          button, .btn { 
            padding: 10px 20px; 
            margin: 5px; 
            background: #4CAF50; 
            color: white; 
            border: none; 
            cursor: pointer; 
            text-decoration: none;
            display: inline-block;
            border-radius: 4px;
          }
          .btn-red { background: #f44336; }
          .btn-blue { background: #2196F3; }
          .btn-purple { background: #9C27B0; }
          .export-section { 
            border: 1px solid #ddd; 
            padding: 20px; 
            margin: 20px 0; 
            border-radius: 5px; 
            background: #f9f9f9; 
          }
          .collection-list { margin: 10px 0; }
          .collection-item { 
            background: #e9e9e9; 
            padding: 8px 12px; 
            margin: 5px 0; 
            border-radius: 3px; 
            display: flex; 
            justify-content: space-between; 
            align-items: center;
          }
          .status { padding: 10px; margin: 10px 0; border-radius: 4px; }
          .status-success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
          .status-error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
          .status-info { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; }
        </style>
      </head>
      <body>
        <div class="container">
          <h1>🚗 Komis Samochodowy - MongoDB</h1>
          
          <div class="status status-success">
            <strong>✅ MongoDB działa! Połączono pomyślnie.</strong>
          </div>
          
          <div class="export-section">
            <h2>📤 Eksport danych</h2>
            
            <h3>Kolekcje w bazie 'komis':</h3>
            <div class="collection-list">
              ${collections.map(c => `
                <div class="collection-item">
                  <span>${c.name}</span>
                  <a href="/export/${c.name}" class="btn btn-blue" target="_blank">Pobierz JSON</a>
                </div>
              `).join('')}
            </div>
            
            <div style="margin-top: 20px;">
              <h3>Opcje eksportu:</h3>
              <a href="/export/all/json" class="btn btn-blue">Eksportuj wszystkie dane (JSON)</a>
              <a href="/export/all/csv" class="btn btn-blue">Eksportuj wszystkie dane (CSV)</a>
              <a href="/export/backup" class="btn btn-purple">Pełna kopia zapasowa (ZIP)</a>
            </div>
          </div>
          
          <div class="export-section">
            <h2>🧪 Testuj API</h2>
            <button onclick="testEndpoint('/api/test')">Test połączenia</button>
            <button onclick="testEndpoint('/api/samochody')">Pokaż samochody</button>
            <button onclick="addCar()">Dodaj samochód</button>
            <button onclick="exportData()">Eksportuj samochody</button>
          </div>
          
          <div class="export-section">
            <h2>🗄️ Zarządzanie bazą</h2>
            <button onclick="createCollection()">Utwórz kolekcję testową</button>
            <button onclick="clearData()" class="btn-red">Wyczyść wszystkie dane</button>
          </div>
          
          <div id="result" style="margin-top: 20px;"></div>
        </div>
        
        <script>
          async function testEndpoint(url) {
            showLoading();
            try {
              const response = await fetch(url);
              const data = await response.json();
              showResult(data);
            } catch (error) {
              showResult({ error: error.message });
            }
          }
          
          async function addCar() {
            showLoading();
            try {
              const response = await fetch('/api/samochody', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  marka: ['Toyota', 'Ford', 'BMW', 'Audi', 'Mercedes'][Math.floor(Math.random() * 5)],
                  model: 'Model ' + Math.floor(Math.random() * 1000),
                  rok_produkcji: 2015 + Math.floor(Math.random() * 10),
                  przebieg: Math.floor(Math.random() * 200000),
                  cena: 20000 + Math.floor(Math.random() * 150000),
                  status: 'Dostępny',
                  data_dodania: new Date().toISOString().split('T')[0]
                })
              });
              const data = await response.json();
              showResult(data);
            } catch (error) {
              showResult({ error: error.message });
            }
          }
          
          async function exportData() {
            showLoading();
            try {
              const response = await fetch('/export/samochody');
              if (response.ok) {
                // Pobierz plik
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'samochody.json';
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                
                showResult({ 
                  success: true, 
                  message: 'Plik został wygenerowany i pobrany!',
                  fileName: 'samochody.json'
                });
              }
            } catch (error) {
              showResult({ error: error.message });
            }
          }
          
          async function createCollection() {
            showLoading();
            try {
              const response = await fetch('/api/create-test-collection', { method: 'POST' });
              const data = await response.json();
              showResult(data);
            } catch (error) {
              showResult({ error: error.message });
            }
          }
          
          async function clearData() {
            if (confirm('Czy na pewno chcesz usunąć wszystkie dane? Ta operacja jest nieodwracalna!')) {
              showLoading();
              try {
                const response = await fetch('/api/clear-all', { method: 'DELETE' });
                const data = await response.json();
                showResult(data);
              } catch (error) {
                showResult({ error: error.message });
              }
            }
          }
          
          function showLoading() {
            document.getElementById('result').innerHTML = 
              '<div class="status status-info">Ładowanie...</div>';
          }
          
          function showResult(data) {
            document.getElementById('result').innerHTML = 
              '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
          }
        </script>
      </body>
      </html>
    `);
    
  } catch (error) {
    res.send(`
      <!DOCTYPE html>
      <html>
      <head><title>Błąd MongoDB</title></head>
      <body style="font-family: Arial; padding: 20px;">
        <h1>❌ Błąd połączenia z MongoDB</h1>
        <p>${error.message}</p>
        
        <h3>Rozwiązania:</h3>
        <ol>
          <li><strong>Otwórz PowerShell jako Administrator</strong></li>
          <li>Wpisz te komendy:</li>
          <pre style="background: #333; color: white; padding: 10px;">
cd "C:\\Program Files\\MongoDB\\Server\\8.2\\bin"
.\\mongod.exe</pre>
          <li>Nie zamykaj okna PowerShell!</li>
          <li>Odśwież tę stronę</li>
        </ol>
        
        <p><a href="/">Odśwież stronę</a></p>
      </body>
      </html>
    `);
  }
});

// API endpoint - test
app.get('/api/test', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    await client.close();
    
    res.json({
      status: 'success',
      message: 'MongoDB działa poprawnie',
      collections: collections.map(c => c.name),
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.json({ status: 'error', message: error.message });
  }
});

// API endpoint - pobierz samochody
app.get('/api/samochody', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collection = db.collection('samochody');
    const samochody = await collection.find({}).toArray();
    await client.close();
    
    res.json({
      status: 'success',
      count: samochody.length,
      samochody: samochody
    });
  } catch (error) {
    res.json({ status: 'error', message: error.message, samochody: [] });
  }
});

// API endpoint - dodaj samochód
app.post('/api/samochody', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collection = db.collection('samochody');
    
    const samochod = {
      ...req.body,
      _id: require('mongodb').ObjectId().toString(),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    const result = await collection.insertOne(samochod);
    await client.close();
    
    res.json({
      status: 'success',
      message: 'Samochód dodany',
      id: result.insertedId,
      samochod: samochod
    });
  } catch (error) {
    res.json({ status: 'error', message: error.message });
  }
});

// EKSPORT - Pojedyncza kolekcja
app.get('/export/:collectionName', async (req, res) => {
  try {
    const { collectionName } = req.params;
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collection = db.collection(collectionName);
    const data = await collection.find({}).toArray();
    await client.close();
    
    // Ustaw nagłówki dla pobierania pliku
    res.setHeader('Content-Disposition', `attachment; filename="${collectionName}_export.json"`);
    res.setHeader('Content-Type', 'application/json');
    
    res.send(JSON.stringify(data, null, 2));
    
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// EKSPORT - Wszystkie kolekcje jako JSON
app.get('/export/all/json', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    
    const exportData = {};
    
    for (const collInfo of collections) {
      const collection = db.collection(collInfo.name);
      const data = await collection.find({}).toArray();
      exportData[collInfo.name] = data;
    }
    
    await client.close();
    
    // Ustaw nagłówki dla pobierania pliku
    res.setHeader('Content-Disposition', 'attachment; filename="komis_full_export.json"');
    res.setHeader('Content-Type', 'application/json');
    
    res.send(JSON.stringify(exportData, null, 2));
    
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// EKSPORT - Wszystkie kolekcje jako CSV
app.get('/export/all/csv', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    
    let csvContent = 'KOMIS SAMOCHODOWY - EKSPORT DANYCH\n\n';
    
    for (const collInfo of collections) {
      csvContent += `=== ${collInfo.name.toUpperCase()} ===\n`;
      
      const collection = db.collection(collInfo.name);
      const data = await collection.find({}).limit(100).toArray();
      
      if (data.length > 0) {
        // Nagłówki kolumn
        const headers = Object.keys(data[0]);
        csvContent += headers.join(',') + '\n';
        
        // Dane
        data.forEach(item => {
          const row = headers.map(header => {
            const value = item[header];
            if (value === null || value === undefined) return '';
            if (typeof value === 'object') return JSON.stringify(value).replace(/,/g, ';');
            return String(value).replace(/,/g, ';');
          });
          csvContent += row.join(',') + '\n';
        });
      }
      
      csvContent += '\n\n';
    }
    
    await client.close();
    
    // Ustaw nagłówki dla pobierania pliku
    res.setHeader('Content-Disposition', 'attachment; filename="komis_export.csv"');
    res.setHeader('Content-Type', 'text/csv');
    
    res.send(csvContent);
    
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// EKSPORT - Backup ZIP (wymaga archiver)
app.get('/export/backup', async (req, res) => {
  try {
    const { MongoClient } = require('mongodb');
    const archiver = require('archiver');
    
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    
    // Ustaw nagłówki dla ZIP
    res.setHeader('Content-Disposition', 'attachment; filename="komis_backup.zip"');
    res.setHeader('Content-Type', 'application/zip');
    
    const archive = archiver('zip', { zlib: { level: 9 } });
    archive.pipe(res);
    
    // Dodaj każdej kolekcji jako osobny plik JSON
    for (const collInfo of collections) {
      const collection = db.collection(collInfo.name);
      const data = await collection.find({}).toArray();
      
      archive.append(JSON.stringify(data, null, 2), { name: `${collInfo.name}.json` });
    }
    
    // Dodaj plik README
    const readme = `# Backup bazy danych - Komis Samochodowy
Data utworzenia: ${new Date().toISOString()}
Kolekcje: ${collections.map(c => c.name).join(', ')}
    
Jak przywrócić dane:
1. Uruchom MongoDB
2. Wykonaj dla każdego pliku JSON:
   mongoimport --db komis --collection NAZWA_KOLEKCJI --file NAZWA_KOLEKCJI.json --jsonArray
   
   lub użyj Mongo Compass do importu.`;
    
    archive.append(readme, { name: 'README.txt' });
    archive.finalize();
    
    await client.close();
    
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Utwórz kolekcję testową
app.post('/api/create-test-collection', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    
    await db.createCollection('test_export');
    await db.collection('test_export').insertOne({
      test: 'Przykładowe dane do eksportu',
      timestamp: new Date(),
      version: '1.0'
    });
    
    await client.close();
    
    res.json({ 
      success: true, 
      message: 'Utworzono kolekcję testową',
      collection: 'test_export'
    });
  } catch (error) {
    res.json({ success: false, message: error.message });
  }
});

// Wyczyść wszystkie dane (OSTROŻNIE!)
app.delete('/api/clear-all', async (req, res) => {
  try {
    const client = new MongoClient('mongodb://localhost:27017');
    await client.connect();
    const db = client.db('komis');
    const collections = await db.listCollections().toArray();
    
    for (const collInfo of collections) {
      await db.collection(collInfo.name).deleteMany({});
    }
    
    await client.close();
    
    res.json({ 
      success: true, 
      message: `Wyczyszczono ${collections.length} kolekcji`,
      collections: collections.map(c => c.name)
    });
  } catch (error) {
    res.json({ success: false, message: error.message });
  }
});

// Uruchom serwer
app.listen(PORT, () => {
  console.log(`
╔═══════════════════════════════════════════════════╗
║     🚗 KOMPIS SAMOCHODOWY - MongoDB + EKSPORT    ║
║     🚀 Serwer uruchomiony                        ║
╠═══════════════════════════════════════════════════╣
║ 🌐  Adres: http://localhost:${PORT}              ║
║ 📤  Eksport: http://localhost:${PORT}/export/*   ║
║ 🗄️   MongoDB: localhost:27017                    ║
║ 📁  Baza: komis                                  ║
╚═══════════════════════════════════════════════════╝

📋 Funkcje eksportu:
1. /export/samochody     - eksport kolekcji samochodów
2. /export/all/json      - eksport wszystkich danych (JSON)
3. /export/all/csv       - eksport wszystkich danych (CSV)
4. /export/backup        - pełna kopia zapasowa (ZIP)
`);
});