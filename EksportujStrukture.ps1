# Konfiguracja
$outputFile = "struktura_i_zawartosc.txt"
$excludeFiles = @($outputFile, $MyInvocation.MyCommand.Name)

# Funkcja sprawdzająca czy plik jest tekstowy (na podstawie rozszerzenia)
function Test-IsTextFile {
    param([string]$Path)
    
    $textExtensions = @(
         '.ini', '.log','.json', '.js', '.css', '.html', '.htm',
         '.bat', '.java', '.py', '.c', '.cpp', '.h', '.cs',
        '.yml', '.yaml', '.php', '.asp', '.aspx', '.jsp', '.sh', '.fxml'
        #,'.sql','.ps1', '.xml', '.properties', '.md', '.txt', '.csv'
    )
    
    $extension = [System.IO.Path]::GetExtension($Path).ToLower()
    return $textExtensions -contains $extension
}

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
    
    # Przeszukaj wszystkie pliki z pominięciem folderu target
    Get-ChildItem -Path $startPath -Recurse -File | 
        Where-Object { $_.Directory.FullName -notmatch $excludeTargetPattern } |
        ForEach-Object {
            $absolutePath = $_.FullName

            # Pomijanie plików wykluczonych po nazwie
            if ($excludeFiles -contains $_.Name) { return }
            
            # Pomijanie pliku wyjściowego po pełnej ścieżce
            if ($absolutePath -eq (Join-Path $pwd $output)) { return }

            # Pomijaj pliki binarne
            if (-not (Test-IsTextFile -Path $absolutePath)) { return }
            
            $relativePath = $absolutePath.Substring($absoluteStart.Length).TrimStart('\')
            $separator = "-" * ($relativePath.Length + 12)
            
            Add-Content -Path $output -Value "`n`nŚCIEŻKA: $relativePath"
            Add-Content -Path $output -Value $separator
            
            # Próba odczytu zawartości
            try {
                $content = Get-Content -Path $absolutePath -Raw -ErrorAction Stop
                
                if ([string]::IsNullOrWhiteSpace($content)) {
                    Add-Content -Path $output -Value "[PLIK PUSTY]"
                }
                else {
                    Add-Content -Path $output -Value $content
                }
            }
            catch [System.UnauthorizedAccessException] {
                Add-Content -Path $output -Value "[BRAK UPRAWNIEŃ - NIE MOŻNA ODCZYTAĆ]"
            }
            catch {
                Add-Content -Path $output -Value "[BŁĄD ODCZYTU: $($_.Exception.Message)]"
            }
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