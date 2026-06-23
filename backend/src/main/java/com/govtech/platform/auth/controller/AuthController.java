package com.govtech.platform.auth.controller;

import com.govtech.platform.auth.dto.ChangePasswordRequest;
import com.govtech.platform.auth.dto.LoginRequest;
import com.govtech.platform.auth.dto.LoginResponse;
import com.govtech.platform.auth.service.AuthService;
import com.govtech.platform.common.response.ApiResponse;
import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and password management.
 *
 * <p>Base path: {@code /api/v1/auth} (context-path {@code /api} is set globally)</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /v1/auth/login}            — public; returns JWT</li>
 *   <li>{@code PATCH /v1/auth/change-password} — authenticated; any role</li>
 * </ul>
 *
 * <h2>Postman testing</h2>
 * <pre>
 * POST http://localhost:8080/api/v1/auth/login
 * Content-Type: application/json
 * {
 *   "username": "admin@gov.lk",
 *   "password": "Admin@123"
 * }
 *
 * PATCH http://localhost:8080/api/v1/auth/change-password
 * Authorization: Bearer &lt;token&gt;
 * Content-Type: application/json
 * {
 *   "currentPassword": "Citizen@123",
 *   "newPassword": "NewPass@456"
 * }
 * </pre>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and password management endpoints. POST /login is public — no token required.")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT Bearer token.
     *
     * <p>Returns {@code mustChangePassword: true} for newly created citizen accounts.
     * The frontend should redirect the citizen to the change-password screen in that case.</p>
     */
    @PostMapping("/login")
    @Operation(
            summary     = "Login and receive a JWT token (PUBLIC)",
            description = "Authenticate with username and password. No Authorization header required. "
                        + "Default accounts: admin@gov.lk/Admin@123, agent@gov.lk/Agent@123, citizen@gov.lk/Citizen@123 "
                        + "(note: running the Postman collection changes the citizen password to Citizen@12345). "
                        + "If mustChangePassword is true, the user must change their temporary password before using other features."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Login successful — JWT token returned.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error — username or password is blank.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Invalid credentials.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * <p>Accessible by any authenticated role (ADMIN, SERVICE_AGENT, CITIZEN).
     * The current password must match before the new password is accepted.</p>
     */
    @PatchMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary     = "Change password (any authenticated role)",
            description = "Changes the authenticated user's password. "
                        + "Requires the current password for verification. "
                        + "Clears the mustChangePassword flag on success."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Password changed successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or current password does not match.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully."));
    }
}
