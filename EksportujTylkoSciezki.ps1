# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name)

# Główna funkcja eksportująca
function Export-Structure {
    param(
        [string]$startPath = ".",
        [string]$output = "output.txt"
    )
    
    # Utwórz plik wyjściowy (lub wyczyść jeśli istnieje)
    Set-Content -Path $output -Value "Eksport struktury katalogów i plików`nWygenerowano: $(Get-Date)`n" -Force
    
    # Uzyskaj pełną ścieżkę startową
    $absoluteStart = (Get-Item $startPath).FullName
    
    # Wyrażenie regularne do pomijania folderu /target/
    $excludeTargetPattern = '(^|[\\])target([\\]|$)'
    
    # Przeszukaj wszystkie elementy (foldery i pliki)
    Get-ChildItem -Path $startPath -Recurse | 
        Where-Object { 
            # Pomijamy elementy w folderze 'target'
            $_.FullName -notmatch $excludeTargetPattern
        } | ForEach-Object {
            # Pomijanie plików wykluczonych
            if (-not $_.PSIsContainer) {
                if ($excludeFiles -contains $_.Name) { return }
                if ($_.FullName -eq (Join-Path $pwd $output)) { return }
            }
            
            $relativePath = $_.FullName.Substring($absoluteStart.Length).TrimStart('\')
            Add-Content -Path $output -Value $relativePath
        }
}

# Uruchomienie eksportu
Export-Structure -output $outputFile

# Komunikat końcowy
$outputPath = Join-Path (Get-Location) $outputFile
Write-Host "`nOperacja zakończona pomyślnie!"
Write-Host "Plik wyjściowy: $outputPath" -ForegroundColor Green
Write-Host "`nNaciśnij dowolny klawisz, aby kontynuować..."

# Zabezpieczone oczekiwanie na klawisz
if ($host.Name -eq 'ConsoleHost') {
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}