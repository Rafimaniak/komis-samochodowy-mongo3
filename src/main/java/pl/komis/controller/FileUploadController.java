package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FileUploadController {

    // Ścieżka do katalogu ze zdjęciami
    private static final String UPLOAD_DIR = "src/main/resources/static/images/samochody/";

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            // Utwórz katalog jeśli nie istnieje
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generuj unikalną nazwę pliku
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Zapisz plik
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath);

            redirectAttributes.addFlashAttribute("message",
                    "Plik " + originalFileName + " został pomyślnie wgrany!");
            redirectAttributes.addFlashAttribute("fileName", uniqueFileName);

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message",
                    "Błąd podczas wgrywania pliku: " + e.getMessage());
        }

        return "redirect:/samochody/dodaj";
    }
}