package com.buildsync.media;

import com.buildsync.common.exception.FileValidationException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.media.application.FileSystemStorageService;
import com.buildsync.media.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    private FileSystemStorageService storageService;
    private StorageProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setAvatarDir("avatars");

        storageService = new FileSystemStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("storeAvatar should store file in directory and return public media URL")
    void storeAvatar_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "test-image-content".getBytes());

        String avatarUrl = storageService.storeAvatar(file);

        assertThat(avatarUrl).startsWith("/api/v1/media/avatars/");
        assertThat(avatarUrl).endsWith(".png");

        String filename = avatarUrl.replace("/api/v1/media/avatars/", "");
        Path storedFile = storageService.getAvatarStorageLocation().resolve(filename);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo("test-image-content".getBytes());
    }

    @Test
    @DisplayName("loadAvatarAsResource should return resource for existing file")
    void loadAvatarAsResource_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my-pic.jpg", "image/jpeg", "image-bytes".getBytes());
        String avatarUrl = storageService.storeAvatar(file);
        String filename = avatarUrl.replace("/api/v1/media/avatars/", "");

        Resource resource = storageService.loadAvatarAsResource(filename);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("loadAvatarAsResource should throw ResourceNotFoundException for missing file")
    void loadAvatarAsResource_MissingFile() {
        assertThatThrownBy(() -> storageService.loadAvatarAsResource("non-existent-avatar.jpg"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Avatar file not found");
    }

    @Test
    @DisplayName("loadAvatarAsResource should reject path traversal attempt")
    void loadAvatarAsResource_PathTraversal() {
        assertThatThrownBy(() -> storageService.loadAvatarAsResource("../outside.jpg"))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    @DisplayName("deleteAvatar should remove file from storage directory")
    void deleteAvatar_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pic.jpg", "image/jpeg", "to-delete".getBytes());
        String avatarUrl = storageService.storeAvatar(file);
        String filename = avatarUrl.replace("/api/v1/media/avatars/", "");

        Path storedFile = storageService.getAvatarStorageLocation().resolve(filename);
        assertThat(Files.exists(storedFile)).isTrue();

        storageService.deleteAvatar(avatarUrl);

        assertThat(Files.exists(storedFile)).isFalse();
    }
}

