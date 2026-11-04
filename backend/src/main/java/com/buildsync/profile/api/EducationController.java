package com.buildsync.profile.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.profile.application.EducationService;
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
 * REST controller for managing education history on developer profiles.
 */
@RestController
@RequestMapping("/api/v1/profiles/me/education")
@Tag(name = "Education Management", description = "Endpoints for managing academic background on developer profiles")
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping
    @Operation(
            summary = "Get current developer's education records",
            description = "Retrieves all education history records for the authenticated developer's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Education records list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EducationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EducationResponse>> getMyEducation(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<EducationResponse> education = educationService.getEducation(userId);
        return ResponseEntity.ok(education);
    }

    @PostMapping
    @Operation(
            summary = "Add education record",
            description = "Creates a new academic education record on the authenticated developer's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Education record created successfully",
                    content = @Content(schema = @Schema(implementation = EducationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields or date ranges",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EducationResponse> addEducation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EducationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        EducationResponse response = educationService.addEducation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update education record",
            description = "Updates an existing education record owned by the authenticated developer",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Education record updated successfully",
                    content = @Content(schema = @Schema(implementation = EducationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields or date ranges",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Education record or profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EducationResponse> updateEducation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody EducationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        EducationResponse response = educationService.updateEducation(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete education record",
            description = "Deletes an education record owned by the authenticated developer",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Education record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Education record or profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        educationService.deleteEducation(userId, id);
        return ResponseEntity.noContent().build();
    }
}

