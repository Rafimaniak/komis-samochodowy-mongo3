param (
    [Parameter(Mandatory = $true, ValueFromRemainingArguments = $true)]
    [string[]]$Files
)

$outputFile = "struktura_i_zawartosc.txt"

# Inicjalizacja tokenów (ważne — zapobiega błędowi na null)
$script:__tokens = @{}

# Jeśli przekazano tylko jeden argument, a wewnątrz są spacje — rozbij go na osobne ścieżki
if ($Files.Count -eq 1 -and $Files[0] -match '\s') {
    $line = $Files[0]

    # Rozpoznaj fragmenty w cudzysłowach i zamień je na tokeny
    $quotedMatches = [regex]::Matches($line, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
    foreach ($q in $quotedMatches) {
        $token = [Guid]::NewGuid().ToString()
        $line = $line -replace [regex]::Escape('"' + $q + '"'), $token
        $script:__tokens[$token] = $q
    }

    # teraz split po białych znakach
    $parts = $line -split '\s+' | Where-Object { $_ -ne '' }

    # przywróć fragmenty które były w cudzysłowach
    $resolved = foreach ($p in $parts) {
        if ($script:__tokens.ContainsKey($p)) { $script:__tokens[$p] } else { $p }
    }

    $Files = $resolved

    # wyczyść tymczasowe tokeny
    $script:__tokens.Clear()
}

# Funkcja sprawdzająca czy plik jest tekstowy (na podstawie rozszerzenia)
function Test-IsTextFile {
    param([string]$Path)
    
    $textExtensions = @(
        '.ini', '.log', '.json', '.js', '.css', '.html', '.htm',
        '.bat', '.java', '.py', '.c', '.cpp', '.h', '.cs',
        '.yml', '.yaml', '.php', '.asp', '.aspx', '.jsp', '.sh', '.fxml'
    )
    
    $extension = [System.IO.Path]::GetExtension($Path).ToLower()
    return $textExtensions -contains $extension
}

# Utwórz lub wyczyść plik wyjściowy
Set-Content -Path $outputFile -Value "Eksport wybranych plików`nWygenerowano: $(Get-Date)`n" -Force

foreach ($file in $Files) {

    if ([string]::IsNullOrWhiteSpace($file)) {
        continue
    }

    $absolutePath = Resolve-Path -Path $file -ErrorAction SilentlyContinue

    if (-not $absolutePath) {
        Add-Content -Path $outputFile -Value "`n`nŚCIEŻKA: $file"
        Add-Content -Path $outputFile -Value "--------------------------------"
        Add-Content -Path $outputFile -Value "[BŁĄD: PLIK NIE ISTNIEJE]"
        continue
    }

    $absolutePath = $absolutePath.Path

    # Pomijaj pliki binarne
    if (-not (Test-IsTextFile -Path $absolutePath)) {
        Add-Content -Path $outputFile -Value "`n`nŚCIEŻKA: $file"
        Add-Content -Path $outputFile -Value "--------------------------------"
        Add-Content -Path $outputFile -Value "[POMINIĘTO: NIEJAWNY FORMAT]"
        continue
    }

    $separator = "-" * ($absolutePath.Length + 12)

    Add-Content -Path $outputFile -Value "`n`nŚCIEŻKA: $absolutePath"
    Add-Content -Path $outputFile -Value $separator

    try {
        $content = Get-Content -Path $absolutePath -Raw -ErrorAction Stop
        
        if ([string]::IsNullOrWhiteSpace($content)) {
            Add-Content -Path $outputFile -Value "[PLIK PUSTY]"
        }
        else {
            Add-Content -Path $outputFile -Value $content
        }
    }
    catch {
        Add-Content -Path $outputFile -Value "[BŁĄD ODCZYTU: $($_.Exception.Message)]"
    }
}

Write-Host "`nOperacja zakończona pomyślnie!"
Write-Host "Plik wyjściowy: $(Join-Path (Get-Location) $outputFile)" -ForegroundColor Green
