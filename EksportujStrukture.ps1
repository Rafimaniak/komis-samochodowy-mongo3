# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name, "package-lock.json")

# Funkcja sprawdzająca czy plik jest tekstowy
function Test-IsTextFile {
    param([string]$Path)
    
    $textExtensions = @(
        '.ini', '.log', '.json', '.js', '.css', '.html', '.htm',
        '.bat', '.java', '.py', '.c', '.cpp', '.h', '.cs',
        '.yml', '.yaml', '.php', '.asp', '.aspx', '.jsp', '.sh', '.fxml',
        '.txt', '.md', '.xml', '.sql', '.ps1', '.csv', '.properties'
    )
    
    $extension = [System.IO.Path]::GetExtension($Path).ToLower()
    return $textExtensions -contains $extension
}

# Funkcja sprawdzająca czy plik powinien być pominięty
function Test-ShouldSkipFile {
    param([string]$Path)
    
    $skipPatterns = @(
        'package-lock.json',
        '*.log',
        '*.zip',
        '*.rar',
        '*.7z',
        '*.tar',
        '*.gz',
        '*.jar',
        '*.war',
        '*.exe',
        '*.dll',
        '*.so',
        '*.bin',
        'node_modules',
        'target',
        '.git'
    )
    
    $fileName = [System.IO.Path]::GetFileName($Path)
    $fileExt = [System.IO.Path]::GetExtension($Path).ToLower()
    
    # Sprawdź rozmiar pliku (pomiń > 1MB)
    try {
        $fileSize = (Get-Item $Path -ErrorAction Stop).Length
        if ($fileSize -gt 1MB) {  # 1MB = 1,048,576 bajtów
            Write-Host "  POMINIĘTO: Plik zbyt duży ($([math]::Round($fileSize/1MB, 2)) MB)" -ForegroundColor Yellow
            return $true
        }
    }
    catch {
        # Jeśli nie można sprawdzić rozmiaru, kontynuuj
    }
    
    # Sprawdź listę wykluczonych plików
    foreach ($pattern in $skipPatterns) {
        if ($pattern -eq $fileName) {
            Write-Host "  POMINIĘTO: Plik na liście wykluczeń" -ForegroundColor Yellow
            return $true
        }
        
        if ($pattern.StartsWith("*") -and $fileExt -eq $pattern.Substring(1)) {
            Write-Host "  POMINIĘTO: Rozszerzenie na liście wykluczeń" -ForegroundColor Yellow
            return $true
        }
    }
    
    return $false
}

# Główna funkcja eksportująca
function Export-Structure {
    param(
        [string]$startPath = ".",
        [string]$output = "output.txt"
    )
    
    # Użyj Windows-1250 dla pliku wyjściowego
    $windowsEncoding = [System.Text.Encoding]::GetEncoding(1250)
    $outputPath = Join-Path (Get-Location) $output
    
    # Utwórz nagłówek pliku w Windows-1250
    $header = "Eksport struktury katalogów i plików`r`nWygenerowano: $(Get-Date)`r`n"
    [System.IO.File]::WriteAllText($outputPath, $header, $windowsEncoding)
    
    # Uzyskaj pełną ścieżkę startową
    $absoluteStart = (Get-Item $startPath).FullName
    
    # Wyrażenie regularne do pomijania folderów /target/ i /node_modules/
    $excludePattern = '(^|[\\])(target|node_modules|\.git|\.idea)([\\]|$)'
    
    # Licznik
    $processedCount = 0
    $errorCount = 0
    $skippedCount = 0
    
    Write-Host "`nPrzeszukiwanie katalogu: $startPath" -ForegroundColor Cyan
    Write-Host "Wykluczone foldery: target, node_modules, .git, .idea" -ForegroundColor Yellow
    Write-Host "Wykluczone pliki: package-lock.json, *.log, *.zip, *.exe, *.dll, >1MB" -ForegroundColor Yellow
    Write-Host "=" * 50
    
    # Przeszukaj wszystkie pliki z pominięciem folderów
    Get-ChildItem -Path $startPath -Recurse -File | 
        Where-Object { $_.Directory.FullName -notmatch $excludePattern } |
        ForEach-Object {
            $absolutePath = $_.FullName
            $relativePath = $absolutePath.Substring($absoluteStart.Length).TrimStart('\')
            
            Write-Host "Sprawdzanie: $relativePath" -ForegroundColor Gray -NoNewline
            
            if ($excludeFiles -contains $_.Name) { 
                Write-Host "  POMINIĘTO: Plik wykluczony" -ForegroundColor Yellow
                $skippedCount++
                return 
            }
            
            if ($absolutePath -eq (Join-Path $pwd $output)) { 
                Write-Host "  POMINIĘTO: Plik wyjściowy" -ForegroundColor Yellow
                $skippedCount++
                return 
            }
            
            if (-not (Test-IsTextFile -Path $absolutePath)) { 
                Write-Host "  POMINIĘTO: Niejawny format" -ForegroundColor Yellow
                $skippedCount++
                return 
            }
            
            if (Test-ShouldSkipFile -Path $absolutePath) {
                $skippedCount++
                return
            }
            
            $separator = "-" * ($relativePath.Length + 12)
            
            # Dodaj ścieżkę pliku
            $pathMessage = "`r`n`r`nSCIEZKA: $relativePath`r`n$separator`r`n"
            [System.IO.File]::AppendAllText($outputPath, $pathMessage, $windowsEncoding)
            
            # Próba odczytu zawartości
            try {
                # Użyj domyślnego kodowania systemu
                $content = [System.IO.File]::ReadAllText($absolutePath, [System.Text.Encoding]::Default)
                
                if ([string]::IsNullOrWhiteSpace($content)) {
                    $emptyMessage = "[PLIK PUSTY]`r`n"
                    [System.IO.File]::AppendAllText($outputPath, $emptyMessage, $windowsEncoding)
                    Write-Host "  OK (pusty)" -ForegroundColor Gray
                }
                else {
                    [System.IO.File]::AppendAllText($outputPath, "$content`r`n", $windowsEncoding)
                    Write-Host "  OK" -ForegroundColor Green
                }
                
                $processedCount++
            }
            catch [System.UnauthorizedAccessException] {
                $errorMessage = "[BRAK UPRAWNIEŃ - NIE MOŻNA ODCZYTAĆ]`r`n"
                [System.IO.File]::AppendAllText($outputPath, $errorMessage, $windowsEncoding)
                Write-Host "  BŁĄD: Brak uprawnień" -ForegroundColor Red
                $errorCount++
            }
            catch {
                $errorMessage = "[BŁĄD ODCZYTU: $($_.Exception.Message)]`r`n"
                [System.IO.File]::AppendAllText($outputPath, $errorMessage, $windowsEncoding)
                Write-Host "  BŁĄD: $($_.Exception.Message)" -ForegroundColor Red
                $errorCount++
            }
        }
    
    # Podsumowanie
    $summary = @"

================ PODSUMOWANIE ================
Przetworzonych plików: $processedCount
Pominiętych plików: $skippedCount
Błędów odczytu: $errorCount
Data zakończenia: $(Get-Date)

UWAGA: Pominięto pliki:
- package-lock.json (zbyt duży/złożony)
- Pliki większe niż 1 MB
- Pliki binarne (*.exe, *.dll, *.jar)
- Archiwa (*.zip, *.rar, *.7z)
- Logi (*.log)

"@
    
    [System.IO.File]::AppendAllText($outputPath, $summary, $windowsEncoding)
    
    return @{
        Processed = $processedCount
        Skipped = $skippedCount
        Errors = $errorCount
    }
}

# Uruchomienie
Write-Host "Rozpoczynanie eksportu struktury katalogów..." -ForegroundColor Yellow
Write-Host "=" * 50

$result = Export-Structure -output $outputFile

# Komunikat końcowy
$outputPath = Join-Path (Get-Location) $outputFile
Write-Host "`n" + "=" * 50
Write-Host "Operacja zakończona pomyślnie!" -ForegroundColor Green
Write-Host "Plik wyjściowy: $outputPath" -ForegroundColor Green
Write-Host "`nPodsumowanie:" -ForegroundColor Cyan
Write-Host "  Przetworzonych plików: $($result.Processed)" -ForegroundColor Green
Write-Host "  Pominiętych plików: $($result.Skipped)" -ForegroundColor Yellow
Write-Host "  Błędów odczytu: $($result.Errors)" -ForegroundColor $(if ($result.Errors -gt 0) { "Red" } else { "Green" })
Write-Host "`nUwaga: Plik używa kodowania Windows-1250 (ANSI)" -ForegroundColor Yellow
Write-Host "Otwórz go w Notatniku (Notepad) dla poprawnych polskich znaków." -ForegroundColor Yellow
Write-Host "`nNaciśnij dowolny klawisz, aby kontynuować..."

if ($host.Name -eq 'ConsoleHost') {
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}