package com.aims.service;

import com.aims.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final Set<String> DOC_TYPES = Set.of("application/pdf");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        validateType(category, contentType);

        String extension = getExtension(file.getOriginalFilename());
        String filename = category + "-" + UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file");
        }
        return "/api/files/" + filename;
    }

    public Path resolve(String filename) {
        Path resolved = uploadDir.resolve(filename).normalize();
        if (!resolved.startsWith(uploadDir)) {
            throw new BadRequestException("Invalid file path");
        }
        return resolved;
    }

    private void validateType(String category, String contentType) {
        boolean valid = switch (category) {
            case "photo" -> IMAGE_TYPES.contains(contentType);
            case "video" -> VIDEO_TYPES.contains(contentType);
            case "cv" -> DOC_TYPES.contains(contentType) || IMAGE_TYPES.contains(contentType);
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Unsupported file type: " + contentType);
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.'));
    }
}
