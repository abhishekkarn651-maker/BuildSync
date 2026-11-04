package com.buildsync.media.api;

import com.buildsync.media.application.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;

/**
 * Controlled public media controller safely serving user avatars and media assets.
 */
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media Management", description = "Public media and avatar streaming endpoints")
public class MediaController {

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/avatars/{filename:.+}")
    @Operation(summary = "Stream public avatar asset", description = "Serves stored developer avatar image by unique filename")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar image found and streamed"),
            @ApiResponse(responseCode = "404", description = "Avatar image not found")
    })
    public ResponseEntity<Resource> getAvatar(@PathVariable("filename") String filename) {
        Resource resource = storageService.loadAvatarAsResource(filename);

        MediaType mediaType = MediaType.IMAGE_JPEG;
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}

