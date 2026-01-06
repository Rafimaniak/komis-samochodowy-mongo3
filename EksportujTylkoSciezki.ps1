# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name)

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
    $excludePattern = '(^|[\\])(target|node_modules)([\\]|$)'
    
    # Licznik elementów
    $folderCount = 0
    $fileCount = 0
    
    # Przeszukaj wszystkie elementy (foldery i pliki)
    Get-ChildItem -Path $startPath -Recurse | 
        Where-Object { 
            $_.FullName -notmatch $excludePattern
        } | ForEach-Object {
            if (-not $_.PSIsContainer) {
                if ($excludeFiles -contains $_.Name) { return }
                if ($_.FullName -eq (Join-Path $pwd $output)) { return }
                $fileCount++
            }
            else {
                $folderCount++
            }
            
            $relativePath = $_.FullName.Substring($absoluteStart.Length).TrimStart('\')
            [System.IO.File]::AppendAllText($outputPath, "$relativePath`r`n", $windowsEncoding)
        }
    
    # Dodaj podsumowanie
    $summary = @"

================ PODSUMOWANIE ================
Foldery: $folderCount
Pliki: $fileCount
Razem: $($folderCount + $fileCount) elementów
Data zakończenia: $(Get-Date)

"@
    
    [System.IO.File]::AppendAllText($outputPath, $summary, $windowsEncoding)
    
    return @{
        Folders = $folderCount
        Files = $fileCount
        Total = $folderCount + $fileCount
    }
}

# Uruchomienie eksportu
Write-Host "Rozpoczynanie eksportu struktury katalogów..." -ForegroundColor Yellow
Write-Host "Wykluczone foldery: target, node_modules" -ForegroundColor Yellow
Write-Host "=" * 50

$result = Export-Structure -output $outputFile

# Komunikat końcowy
$outputPath = Join-Path (Get-Location) $outputFile
Write-Host "`n" + "=" * 50
Write-Host "Operacja zakończona pomyślnie!" -ForegroundColor Green
Write-Host "Plik wyjściowy: $outputPath" -ForegroundColor Green
Write-Host "Znaleziono $($result.Total) elementów:" -ForegroundColor Cyan
Write-Host "  - Foldery: $($result.Folders)" -ForegroundColor Cyan
Write-Host "  - Pliki: $($result.Files)" -ForegroundColor Cyan
Write-Host "`nUwaga: Plik używa kodowania Windows-1250 (ANSI)" -ForegroundColor Yellow
Write-Host "Otwórz go w Notatniku (Notepad) dla poprawnych polskich znaków." -ForegroundColor Yellow
Write-Host "`nNaciśnij dowolny klawisz, aby kontynuować..."

# Zabezpieczone oczekiwanie na klawisz
if ($host.Name -eq 'ConsoleHost') {
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}