package com.buildsync.profile.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.profile.application.SkillService;
import com.buildsync.profile.domain.SkillCategory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for master skills search and developer profile skill management.
 */
@RestController
@Tag(name = "Skills Management", description = "Endpoints for searching master skills catalogue and managing profile skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/api/v1/skills")
    @Operation(
            summary = "Search master skills catalogue",
            description = "Public endpoint to search and filter available skills by keyword and category"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching skills list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SkillResponse.class))))
    })
    public ResponseEntity<List<SkillResponse>> searchSkills(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) SkillCategory category) {
        List<SkillResponse> skills = skillService.searchSkills(search, category);
        return ResponseEntity.ok(skills);
    }

    @GetMapping("/api/v1/profiles/me/skills")
    @Operation(
            summary = "Get current developer's skills",
            description = "Retrieves all skills assigned to the currently authenticated developer's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile skills list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfileSkillResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProfileSkillResponse>> getMySkills(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<ProfileSkillResponse> skills = skillService.getProfileSkills(userId);
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/api/v1/profiles/me/skills")
    @Operation(
            summary = "Add skill to developer profile",
            description = "Assigns a skill with proficiency level to the authenticated user's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Skill assigned successfully",
                    content = @Content(schema = @Schema(implementation = ProfileSkillResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Skill or profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Skill already assigned to profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileSkillResponse> addSkill(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddSkillRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileSkillResponse response = skillService.addSkillToProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/profiles/me/skills/{skillId}")
    @Operation(
            summary = "Update skill proficiency",
            description = "Updates the proficiency level of an assigned skill on the authenticated user's profile",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skill proficiency updated",
                    content = @Content(schema = @Schema(implementation = ProfileSkillResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Skill assignment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileSkillResponse> updateProficiency(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("skillId") UUID skillId,
            @Valid @RequestBody UpdateSkillProficiencyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ProfileSkillResponse response = skillService.updateSkillProficiency(userId, skillId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/profiles/me/skills/{skillId}")
    @Operation(
            summary = "Remove skill from profile",
            description = "Removes an assigned skill from the authenticated user's profile without deleting it from master catalogue",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Skill removed from profile successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Skill assignment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> removeSkill(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("skillId") UUID skillId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        skillService.removeSkillFromProfile(userId, skillId);
        return ResponseEntity.noContent().build();
    }
}

