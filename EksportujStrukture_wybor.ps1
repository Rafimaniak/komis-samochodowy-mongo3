param (
    [Parameter(Mandatory = $true, ValueFromRemainingArguments = $true)]
    [string[]]$Files
)

$outputFile = "struktura_i_zawartosc.txt"

# Inicjalizacja tokenów
$script:__tokens = @{}

if ($Files.Count -eq 1 -and $Files[0] -match '\s') {
    $line = $Files[0]
    $quotedMatches = [regex]::Matches($line, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
    foreach ($q in $quotedMatches) {
        $token = [Guid]::NewGuid().ToString()
        $line = $line -replace [regex]::Escape('"' + $q + '"'), $token
        $script:__tokens[$token] = $q
    }

    $parts = $line -split '\s+' | Where-Object { $_ -ne '' }
    $resolved = foreach ($p in $parts) {
        if ($script:__tokens.ContainsKey($p)) { $script:__tokens[$p] } else { $p }
    }

    $Files = $resolved
    $script:__tokens.Clear()
}

function Test-IsTextFile {
    param([string]$Path)
    
    $textExtensions = @(
        '.ini', '.log', '.json', '.js', '.css', '.html', '.htm',
        '.bat', '.java', '.py', '.c', '.cpp', '.h', '.cs',
        '.yml', '.yaml', '.php', '.asp', '.aspx', '.jsp', '.sh', '.fxml',
        '.txt', '.md', '.xml', '.sql', '.ps1', '.csv', '.properties',
        '.ts', '.tsx', '.jsx', '.vue', '.svelte'
    )
    
    $extension = [System.IO.Path]::GetExtension($Path).ToLower()
    return $textExtensions -contains $extension
}

function Test-IsExcludedPath {
    param([string]$Path)
    
    return $Path -match '[\\/]node_modules[\\/]'
}

# Użyj Windows-1250
$windowsEncoding = [System.Text.Encoding]::GetEncoding(1250)
$outputPath = Join-Path (Get-Location) $outputFile

# Nagłówek
$header = "Eksport wybranych plików`r`nWygenerowano: $(Get-Date)`r`n"
[System.IO.File]::WriteAllText($outputPath, $header, $windowsEncoding)

# Liczniki
$processedCount = 0
$errorCount = 0
$skippedCount = 0

Write-Host "Rozpoczynanie eksportu wybranych plików..." -ForegroundColor Yellow
Write-Host "Liczba plików do przetworzenia: $($Files.Count)" -ForegroundColor Cyan
Write-Host "=" * 50

foreach ($file in $Files) {
    if ([string]::IsNullOrWhiteSpace($file)) { continue }

    Write-Host "Przetwarzanie: $file" -ForegroundColor Cyan
    
    $absolutePath = Resolve-Path -Path $file -ErrorAction SilentlyContinue

    if (-not $absolutePath) {
        Write-Host "  BŁĄD: Plik nie istnieje" -ForegroundColor Red
        $errorMessage = "`r`n`r`nŚCIEŻKA: $file`r`n" + ("-" * 40) + "`r`n[BŁĄD: PLIK NIE ISTNIEJE]`r`n"
        [System.IO.File]::AppendAllText($outputPath, $errorMessage, $windowsEncoding)
        $errorCount++
        continue
    }

    $absolutePath = $absolutePath.Path

    if (Test-IsExcludedPath -Path $absolutePath) {
        Write-Host "  POMINIĘTO: Plik w folderze node_modules" -ForegroundColor Yellow
        $skipMessage = "`r`n`r`nŚCIEŻKA: $absolutePath`r`n" + ("-" * 40) + "`r`n[POMINIĘTO: PLIK W FOLDERZE node_modules]`r`n"
        [System.IO.File]::AppendAllText($outputPath, $skipMessage, $windowsEncoding)
        $skippedCount++
        continue
    }

    if (-not (Test-IsTextFile -Path $absolutePath)) {
        Write-Host "  POMINIĘTO: Niejawny format" -ForegroundColor Yellow
        $skipMessage = "`r`n`r`nŚCIEŻKA: $absolutePath`r`n" + ("-" * 40) + "`r`n[POMINIĘTO: NIEJAWNY FORMAT]`r`n"
        [System.IO.File]::AppendAllText($outputPath, $skipMessage, $windowsEncoding)
        $skippedCount++
        continue
    }

    $separator = "-" * ($absolutePath.Length + 12)
    $pathMessage = "`r`n`r`nŚCIEŻKA: $absolutePath`r`n$separator`r`n"
    [System.IO.File]::AppendAllText($outputPath, $pathMessage, $windowsEncoding)

    try {
        # Prosty odczyt z domyślnym kodowaniem systemu
        $content = [System.IO.File]::ReadAllText($absolutePath, [System.Text.Encoding]::Default)
        
        if ([string]::IsNullOrWhiteSpace($content)) {
            $emptyMessage = "[PLIK PUSTY]`r`n"
            [System.IO.File]::AppendAllText($outputPath, $emptyMessage, $windowsEncoding)
            Write-Host "  OK (plik pusty)" -ForegroundColor Gray
        }
        else {
            [System.IO.File]::AppendAllText($outputPath, "$content`r`n", $windowsEncoding)
            Write-Host "  OK" -ForegroundColor Green
        }
        
        $processedCount++
    }
    catch {
        Write-Host "  BŁĄD: $($_.Exception.Message)" -ForegroundColor Red
        $errorMessage = "[BŁĄD ODCZYTU: $($_.Exception.Message)]`r`n"
        [System.IO.File]::AppendAllText($outputPath, $errorMessage, $windowsEncoding)
        $errorCount++
    }
}

# Podsumowanie
$summary = @"

================ PODSUMOWANIE ================
Przetworzonych plików: $processedCount
Pominiętych plików: $skippedCount
Błędów odczytu: $errorCount
Razem plików wejściowych: $($Files.Count)
Data zakończenia: $(Get-Date)

"@

[System.IO.File]::AppendAllText($outputPath, $summary, $windowsEncoding)

Write-Host "`n" + "=" * 50
Write-Host "Operacja zakończona pomyślnie!" -ForegroundColor Green
Write-Host "Plik wyjściowy: $outputPath" -ForegroundColor Green
Write-Host "`nPodsumowanie:" -ForegroundColor Cyan
Write-Host "  Przetworzonych plików: $processedCount" -ForegroundColor Green
Write-Host "  Pominiętych plików: $skippedCount" -ForegroundColor Yellow
Write-Host "  Błędów odczytu: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "`nUwaga: Plik używa kodowania Windows-1250 (ANSI)" -ForegroundColor Yellow
Write-Host "Otwórz go w Notatniku (Notepad) dla poprawnych polskich znaków." -ForegroundColor Yellow