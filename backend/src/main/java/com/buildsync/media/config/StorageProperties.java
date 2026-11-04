package com.buildsync.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for file storage and media uploads.
 */
@Configuration
@ConfigurationProperties(prefix = "buildsync.storage")
public class StorageProperties {

    /**
     * Root directory for uploaded files.
     */
    private String uploadDir = "uploads";

    /**
     * Subdirectory specifically for avatar image assets.
     */
    private String avatarDir = "avatars";

    /**
     * Maximum permitted file size in bytes (defaults to 5 MB = 5,242,880 bytes).
     */
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    /**
     * Supported MIME types for avatar images.
     */
    private List<String> allowedContentTypes = Arrays.asList("image/jpeg", "image/png");

    /**
     * Maximum allowed image width/height in pixels.
     */
    private int maxDimension = 4096;

    /**
     * Minimum allowed image width/height in pixels.
     */
    private int minDimension = 32;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getAvatarDir() {
        return avatarDir;
    }

    public void setAvatarDir(String avatarDir) {
        this.avatarDir = avatarDir;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public int getMaxDimension() {
        return maxDimension;
    }

    public void setMaxDimension(int maxDimension) {
        this.maxDimension = maxDimension;
    }

    public int getMinDimension() {
        return minDimension;
    }

    public void setMinDimension(int minDimension) {
        this.minDimension = minDimension;
    }
}

