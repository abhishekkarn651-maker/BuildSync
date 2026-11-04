package com.buildsync.media.application;

import com.buildsync.common.exception.FileValidationException;
import com.buildsync.media.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Deep image validator verifying file size, MIME types, extensions, magic byte signatures,
 * pixel dimensions, and byte integrity to reject corrupted or malicious uploads.
 */
@Component
public class ImageValidator {

    private static final Logger log = LoggerFactory.getLogger(ImageValidator.class);

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final StorageProperties storageProperties;

    public ImageValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * Validates an uploaded avatar image file.
     * Throws {@link FileValidationException} if validation fails.
     *
     * @param file the uploaded file
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Avatar file must not be empty");
        }

        // 1. File size check
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new FileValidationException(String.format(
                    "File size (%d bytes) exceeds the maximum permitted limit of 5 MB", file.getSize()));
        }

        // 2. Client MIME type check
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") && !contentType.equalsIgnoreCase("image/png"))) {
            throw new FileValidationException("Unsupported content type. Only JPEG and PNG images are supported");
        }

        // 3. Filename extension check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileValidationException("Uploaded file must have a valid filename");
        }

        String lowerFilename = originalFilename.trim().toLowerCase(Locale.ROOT);
        if (!lowerFilename.endsWith(".jpg") && !lowerFilename.endsWith(".jpeg") && !lowerFilename.endsWith(".png")) {
            throw new FileValidationException("Unsupported file extension. Only .jpg, .jpeg, and .png are allowed");
        }

        // 4. Read file bytes for deep magic bytes and image content validation
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read file bytes: {}", originalFilename, e);
            throw new FileValidationException("Unable to read uploaded file data", e);
        }

        if (bytes.length < 8) {
            throw new FileValidationException("File data is truncated or too small to be a valid image");
        }

        // 5. Magic byte verification
        boolean isJpeg = matchesMagic(bytes, JPEG_MAGIC);
        boolean isPng = matchesMagic(bytes, PNG_MAGIC);

        if (!isJpeg && !isPng) {
            throw new FileValidationException("Invalid image signature. File content does not match JPEG or PNG format");
        }

        // 6. Decode image content and verify dimensions
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            log.warn("Corrupted image stream during ImageIO decoding: {}", originalFilename, e);
            throw new FileValidationException("Image file is corrupted or could not be decoded", e);
        }

        if (image == null) {
            throw new FileValidationException("Corrupted image file: failed to decode visual data");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < storageProperties.getMinDimension() || height < storageProperties.getMinDimension()) {
            throw new FileValidationException(String.format(
                    "Image dimensions (%dx%d) are too small. Minimum required is %dx%d pixels",
                    width, height, storageProperties.getMinDimension(), storageProperties.getMinDimension()));
        }

        if (width > storageProperties.getMaxDimension() || height > storageProperties.getMaxDimension()) {
            throw new FileValidationException(String.format(
                    "Image dimensions (%dx%d) exceed the maximum permitted limit of %dx%d pixels",
                    width, height, storageProperties.getMaxDimension(), storageProperties.getMaxDimension()));
        }
    }

    private boolean matchesMagic(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}

