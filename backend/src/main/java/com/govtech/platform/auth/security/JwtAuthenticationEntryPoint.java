package com.govtech.platform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govtech.platform.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Returns a structured JSON 401 response when an unauthenticated user attempts
 * to access a protected endpoint.
 *
 * <p>Spring Security's {@code ExceptionTranslationFilter} invokes this entry point
 * when no {@code Authentication} is present in the {@link org.springframework.security.core.context.SecurityContext}
 * after all filters have run (i.e. the JWT filter found no valid token).</p>
 *
 * <p>The response body follows the platform's standard {@link ErrorResponse} format
 * to ensure consistent error handling across all clients (Postman, React frontend).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Unauthenticated access attempt to [{}]: {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication is required to access this resource. "
                       + "Please login and include a valid Bearer token.")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
