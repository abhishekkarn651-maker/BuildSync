package com.buildsync.media;

import com.buildsync.common.exception.FileValidationException;
import com.buildsync.media.application.ImageValidator;
import com.buildsync.media.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorTest {

    private ImageValidator imageValidator;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setMaxFileSizeBytes(5 * 1024 * 1024);
        properties.setMinDimension(32);
        properties.setMaxDimension(4096);
        imageValidator = new ImageValidator(properties);
    }

    private byte[] generateImageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Valid JPEG image passes validation")
    void validate_ValidJpeg_Success() throws IOException {
        byte[] jpegBytes = generateImageBytes(100, 100, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", jpegBytes);

        assertThatCode(() -> imageValidator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid PNG image passes validation")
    void validate_ValidPng_Success() throws IOException {
        byte[] pngBytes = generateImageBytes(150, 150, "png");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", pngBytes);

        assertThatCode(() -> imageValidator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Empty file throws FileValidationException")
    void validate_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Avatar file must not be empty");
    }

    @Test
    @DisplayName("Oversized file throws FileValidationException")
    void validate_OversizedFile_ThrowsException() {
        properties.setMaxFileSizeBytes(100); // 100 bytes limit
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[200]);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("exceeds the maximum permitted limit");
    }

    @Test
    @DisplayName("Unsupported MIME type throws FileValidationException")
    void validate_UnsupportedMimeType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Unsupported content type");
    }

    @Test
    @DisplayName("Unsupported file extension throws FileValidationException")
    void validate_UnsupportedExtension_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.gif", "image/jpeg", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Unsupported file extension");
    }

    @Test
    @DisplayName("Corrupted image with fake extension throws FileValidationException")
    void validate_CorruptedImage_ThrowsException() {
        byte[] fakeBytes = "Not an image at all!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", fakeBytes);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Invalid image signature");
    }

    @Test
    @DisplayName("Image below minimum dimensions throws FileValidationException")
    void validate_BelowMinimumDimensions_ThrowsException() throws IOException {
        byte[] tinyBytes = generateImageBytes(16, 16, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "tiny.jpg", "image/jpeg", tinyBytes);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("too small");
    }

    @Test
    @DisplayName("Image exceeding maximum dimensions throws FileValidationException")
    void validate_ExceedsMaximumDimensions_ThrowsException() throws IOException {
        properties.setMaxDimension(50);
        byte[] largeDimBytes = generateImageBytes(80, 80, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeDimBytes);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("exceed the maximum permitted limit");
    }
}

