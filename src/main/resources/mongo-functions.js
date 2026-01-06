const { MongoClient } = require('mongodb');
const fs = require('fs').promises;
const path = require('path');

const uri = 'mongodb://localhost:27017';
const dbName = 'komis';

async function loadFunctions() {
    const client = new MongoClient(uri, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        serverSelectionTimeoutMS: 5000
    });

    try {
        await client.connect();
        console.log('✅ Połączono z MongoDB');

        const db = client.db(dbName);

        // 1. Najpierw sprawdź czy możemy wykonać prostą komendę
        try {
            const ping = await db.command({ ping: 1 });
            console.log('✅ MongoDB odpowiada:', ping);
        } catch (error) {
            console.error('❌ MongoDB nie odpowiada:', error.message);
            return;
        }

        // 2. Wczytaj plik
        const scriptPath = path.join(__dirname, 'src/main/resources/mongo-functions.js');
        let script;

        try {
            script = await fs.readFile(scriptPath, 'utf8');
            console.log(`📄 Wczytano plik: ${scriptPath} (${script.length} znaków)`);
        } catch (error) {
            console.error(`❌ Nie można wczytać pliku: ${error.message}`);

            // Utwórz prosty skrypt testowy
            script = createTestScript();
            console.log('📝 Używam testowego skryptu...');
        }

        // 3. Wykonaj skrypt w częściach
        const functions = extractFunctions(script);
        console.log(`📦 Znaleziono ${functions.length} funkcji do załadowania`);

        for (let i = 0; i < functions.length; i++) {
            const func = functions[i];
            console.log(`⏳ [${i+1}/${functions.length}] Ładowanie: ${func.name}`);

            try {
                // Wykonaj jako polecenie eval
                const result = await db.command({
                    eval: func.code
                });

                if (result.ok === 1) {
                    console.log(`✅ Załadowano: ${func.name}`);
                } else {
                    console.error(`❌ Błąd: ${func.name} - ${JSON.stringify(result)}`);
                }

            } catch (error) {
                console.error(`❌ Błąd wykonania ${func.name}:`, error.message);

                // Spróbuj użyć db.collection
                try {
                    await saveFunctionViaCollection(db, func);
                    console.log(`✅ Załadowano (alternatywnie): ${func.name}`);
                } catch (e2) {
                    console.error(`❌ Alternatywna metoda też nie działa: ${e2.message}`);
                }
            }
        }

        // 4. Sprawdź które funkcje zostały załadowane
        const systemJs = db.collection('system.js');
        const loaded = await systemJs.find({}).toArray();
        console.log('\n📊 Załadowane funkcje:');
        loaded.forEach(f => console.log(`   • ${f._id}`));

        if (loaded.length === 0) {
            console.log('⚠️  Żadne funkcje nie zostały załadowane!');
            console.log('💡 Spróbuj wykonania przez mongosh:');
            console.log(`   mongosh komis "${scriptPath}"`);
        }

    } catch (error) {
        console.error('❌ Krytyczny błąd:', error.message);
    } finally {
        await client.close();
        console.log('\n🔌 Połączenie zamknięte');
    }
}

function extractFunctions(script) {
    const functions = [];
    const regex = /db\.system\.js\.save\(\s*\{\s*_id:\s*"([^"]+)"\s*,\s*value:\s*(function[^}]+(?:{[^}]*})*)/g;

    let match;
    while ((match = regex.exec(script)) !== null) {
        functions.push({
            name: match[1],
            code: `db.system.js.save({ _id: "${match[1]}", value: ${match[2]})`
        });
    }

    // Jeśli nie znaleziono funkcji tym regexem, spróbuj prostszego
    if (functions.length === 0) {
        console.log('⚠️  Nie znaleziono funkcji regexem, używam prostszego parsowania...');

        // Podziel na bloki rozpoczynające się od db.system.js.save
        const blocks = script.split('db.system.js.save');
        for (let i = 1; i < blocks.length; i++) {
            let block = 'db.system.js.save' + blocks[i];
            const endIndex = findMatchingBrace(block, block.indexOf('{'));
            if (endIndex > 0) {
                block = block.substring(0, endIndex + 1);

                // Wyciągnij nazwę funkcji
                const nameMatch = block.match(/_id:\s*"([^"]+)"/);
                if (nameMatch) {
                    functions.push({
                        name: nameMatch[1],
                        code: block
                    });
                }
            }
        }
    }

    return functions;
}

function findMatchingBrace(str, start) {
    let count = 0;
    for (let i = start; i < str.length; i++) {
        if (str[i] === '{') count++;
        if (str[i] === '}') {
            count--;
            if (count === 0) return i;
        }
    }
    return -1;
}

async function saveFunctionViaCollection(db, func) {
    // Spróbuj bezpośrednio zapisać do kolekcji
    const systemJs = db.collection('system.js');

    // Wymaga parsowania funkcji - to uproszczone
    const funcMatch = func.code.match(/value:\s*(function[^{]+{[\s\S]*})/);
    if (funcMatch) {
        await systemJs.insertOne({
            _id: func.name,
            value: funcMatch[1]
        });
    } else {
        throw new Error('Nie można sparsować funkcji');
    }
}

function createTestScript() {
    return `
    // Testowa funkcja 1
    db.system.js.save({
        _id: "testFunkcja1",
        value: function() {
            return "Test 1 działa!";
        }
    });

    // Testowa funkcja 2
    db.system.js.save({
        _id: "testFunkcja2",
        value: function(a, b) {
            return a + b;
        }
    });
    `;
}

// Uruchom
loadFunctions();