package com.buildsync.project.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.project.application.ProjectService;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller managing collaborative projects, ownership lifecycle, and public discovery.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Management", description = "Endpoints for creating, managing, and discovering collaborative projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(
            summary = "Create a project",
            description = "Creates a new project owned by the authenticated developer.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created successfully", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for project input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        ProjectResponse response = projectService.createProject(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @Operation(
            summary = "Get my projects",
            description = "Retrieves all projects owned by the currently authenticated developer.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        List<ProjectSummaryResponse> response = projectService.getMyProjects(ownerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    @Operation(
            summary = "Get project by ID or slug",
            description = "Retrieves project details. Public projects are accessible anonymously. Private projects are only visible to the project owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project details retrieved", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found or private", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable("projectId") String projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        ProjectResponse response = projectService.getProjectByIdOrSlug(projectId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    @Operation(
            summary = "Update project details",
            description = "Updates project fields and technologies. Only the project owner can update their project.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated successfully", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for update input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can modify this project", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProjectRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectResponse response = projectService.updateProject(projectId, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    @Operation(
            summary = "Delete project",
            description = "Deletes a project and its technology associations. Only the project owner can delete their project.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the owner can delete this project", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProject(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        projectService.deleteProject(projectId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Browse public projects",
            description = "Publicly discoverable project showcase supporting keyword search, category, status, and technology filtering with pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public projects found and paginated", content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query or pagination parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedResponse<ProjectSummaryResponse>> browseProjects(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) ProjectCategory category,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "technology", required = false) String technology,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "sort", defaultValue = "recently_updated") String sort) {

        PagedResponse<ProjectSummaryResponse> response = projectService.browsePublicProjects(
                keyword, category, status, technology, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/technologies")
    @Operation(
            summary = "Get project technologies",
            description = "Retrieves technical skills associated with the project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technologies retrieved"),
            @ApiResponse(responseCode = "404", description = "Project not found or private", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectTechnologyResponse>> getProjectTechnologies(
            @PathVariable("projectId") String projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        List<ProjectTechnologyResponse> response = projectService.getProjectTechnologies(projectId, currentUserId);
        return ResponseEntity.ok(response);
    }
}

