package com.buildsync.media.application;

import com.buildsync.common.exception.FileValidationException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.exception.StorageException;
import com.buildsync.media.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link StorageService}.
 * Handles storage directory initialization, collision-free filename generation,
 * path traversal defense, and safe file lifecycle management.
 */
@Service
public class FileSystemStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);
    public static final String MEDIA_AVATAR_PREFIX = "/api/v1/media/avatars/";

    private final StorageProperties storageProperties;
    private Path avatarStorageLocation;

    public FileSystemStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        try {
            this.avatarStorageLocation = Paths.get(
                    storageProperties.getUploadDir(),
                    storageProperties.getAvatarDir()
            ).toAbsolutePath().normalize();

            Files.createDirectories(this.avatarStorageLocation);
            log.info("Initialized avatar storage directory at: {}", this.avatarStorageLocation);
        } catch (IOException e) {
            log.error("Failed to initialize avatar storage directory", e);
            throw new StorageException("Could not initialize avatar storage directory", e);
        }
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Cannot store an empty file");
        }

        String extension = extractSanitizedExtension(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path destinationPath = sanitizeAndResolvePath(uniqueFilename);

        try {
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored avatar image: {} -> {}", file.getOriginalFilename(), destinationPath);
            return MEDIA_AVATAR_PREFIX + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to store file: {}", uniqueFilename, e);
            throw new StorageException("Failed to store uploaded avatar file", e);
        }
    }

    @Override
    public void deleteAvatar(String avatarReference) {
        if (avatarReference == null || avatarReference.trim().isEmpty()) {
            return;
        }

        String filename = extractFilename(avatarReference);
        try {
            Path targetPath = sanitizeAndResolvePath(filename);
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                log.info("Deleted avatar file: {}", targetPath);
            } else {
                log.warn("Avatar file to delete was not found on disk: {}", targetPath);
            }
        } catch (Exception e) {
            log.error("Failed to delete avatar file: {}", filename, e);
            throw new StorageException("Could not delete avatar file: " + filename, e);
        }
    }

    @Override
    public Resource loadAvatarAsResource(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new ResourceNotFoundException("Avatar filename must not be blank");
        }

        Path filePath = sanitizeAndResolvePath(filename);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Avatar file not found: " + filename);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for avatar resource: {}", filename, e);
            throw new ResourceNotFoundException("Avatar file not found: " + filename);
        }
    }

    private String extractSanitizedExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        String clean = originalFilename.trim().toLowerCase(Locale.ROOT);
        if (clean.endsWith(".png")) {
            return ".png";
        }
        return ".jpg";
    }

    private String extractFilename(String reference) {
        String clean = reference.trim();
        if (clean.contains("/")) {
            clean = clean.substring(clean.lastIndexOf('/') + 1);
        }
        if (clean.contains("\\")) {
            clean = clean.substring(clean.lastIndexOf('\\') + 1);
        }
        return clean;
    }

    private Path sanitizeAndResolvePath(String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileValidationException("Invalid filename containing path traversal characters: " + filename);
        }

        Path resolved = this.avatarStorageLocation.resolve(filename).normalize();
        if (!resolved.startsWith(this.avatarStorageLocation)) {
            throw new FileValidationException("Path traversal attempt detected for file: " + filename);
        }

        return resolved;
    }

    public Path getAvatarStorageLocation() {
        return avatarStorageLocation;
    }
}

