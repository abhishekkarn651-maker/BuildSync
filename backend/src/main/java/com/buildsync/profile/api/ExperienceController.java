package com.buildsync.profile.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.profile.application.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing work experience on developer profiles.
 */
@RestController
@RequestMapping("/api/v1/profiles/me/experience")
@Tag(name = "Experience Management", description = "Endpoints for managing professional work experience on developer profiles")
public class ExperienceController {

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    @Operation(
            summary = "Get current developer's work experience records",
            description = "Retrieves all professional experience records for the authenticated developer's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Experience records list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExperienceResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ExperienceResponse>> getMyExperience(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<ExperienceResponse> experience = experienceService.getExperience(userId);
        return ResponseEntity.ok(experience);
    }

    @PostMapping
    @Operation(
            summary = "Add work experience record",
            description = "Creates a new work experience record on the authenticated developer's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Experience record created successfully",
                    content = @Content(schema = @Schema(implementation = ExperienceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields or date ranges",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ExperienceResponse> addExperience(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExperienceRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ExperienceResponse response = experienceService.addExperience(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update work experience record",
            description = "Updates an existing work experience record owned by the authenticated developer",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Experience record updated successfully",
                    content = @Content(schema = @Schema(implementation = ExperienceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields or date ranges",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Experience record or profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ExperienceResponse> updateExperience(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ExperienceRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ExperienceResponse response = experienceService.updateExperience(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete work experience record",
            description = "Deletes a work experience record owned by the authenticated developer",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Experience record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Experience record or profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteExperience(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        experienceService.deleteExperience(userId, id);
        return ResponseEntity.noContent().build();
    }
}

