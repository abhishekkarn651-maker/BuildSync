package com.buildsync.profile.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.profile.application.DeveloperProfileService;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.ExperienceLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller exposing endpoints for developer profile creation, retrieval, and modification.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Developer Profiles", description = "Endpoints for managing developer profiles and public portfolio showcase")
public class DeveloperProfileController {

    private final DeveloperProfileService profileService;

    public DeveloperProfileController(DeveloperProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(
            summary = "Search and discover developers",
            description = "Publicly accessible endpoint for searching developers with multi-criteria filters, pagination, and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Developers found and paginated", content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedResponse<DeveloperSearchResponse>> searchDevelopers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "skill", required = false) String skill,
            @RequestParam(value = "experienceLevel", required = false) ExperienceLevel experienceLevel,
            @RequestParam(value = "availability", required = false) AvailabilityStatus availability,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "sort", defaultValue = "recently_updated") String sort) {

        PagedResponse<DeveloperSearchResponse> response = profileService.searchDevelopers(
                keyword, skill, experienceLevel, availability, location, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(
            summary = "Create developer profile",
            description = "Creates a new developer profile for the authenticated user. Rejects creation if a profile already exists.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profile created successfully", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Profile already exists for current user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current developer profile",
            description = "Retrieves the full profile details for the currently authenticated developer.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for authenticated user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> getCurrentProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileResponse response = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update current developer profile",
            description = "Updates profile fields for the authenticated developer. Guarantees profile ownership.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for authenticated user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> updateCurrentProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileResponse response = profileService.updateProfileByUserId(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload developer avatar",
            description = "Uploads a JPEG or PNG avatar image (up to 5 MB) for the authenticated developer profile.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or corrupted image file, unsupported format, or oversized file", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for authenticated user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileResponse response = profileService.uploadAvatar(userId, file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/avatar")
    @Operation(
            summary = "Remove developer avatar",
            description = "Removes the avatar from the authenticated developer's profile and deletes the image from storage.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar removed successfully", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for authenticated user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> deleteAvatar(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileResponse response = profileService.deleteAvatar(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Get developer profile by username",
            description = "Publicly accessible endpoint to view a developer's profile by their unique username."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Developer profile found", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or developer profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> getProfileByUsername(@PathVariable("username") String username) {
        ProfileResponse response = profileService.getProfileByUsername(username);
        return ResponseEntity.ok(response);
    }
}

