package com.buildsync.media.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service abstraction allowing local filesystem storage or cloud object-storage
 * (such as AWS S3 or Google Cloud Storage) without impacting core profile domain logic.
 */
public interface StorageService {

    /**
     * Stores an uploaded avatar file, generating a collision-resistant unique filename.
     *
     * @param file the uploaded multipart file
     * @return the controlled public media URL reference (e.g. /api/v1/media/avatars/{filename})
     */
    String storeAvatar(MultipartFile file);

    /**
     * Deletes an avatar file referenced by its media URL or filename.
     *
     * @param avatarReference the stored avatar reference or URL
     */
    void deleteAvatar(String avatarReference);

    /**
     * Loads a stored avatar image as a Spring Resource for controlled serving.
     *
     * @param filename the sanitized server-side filename
     * @return the Resource representing the file
     */
    Resource loadAvatarAsResource(String filename);
}

