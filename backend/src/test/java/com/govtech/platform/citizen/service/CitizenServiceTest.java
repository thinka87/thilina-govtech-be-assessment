package com.govtech.platform.citizen.service;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.auth.repository.UserRepository;
import com.govtech.platform.citizen.dto.CitizenCreatedResponse;
import com.govtech.platform.citizen.dto.CreateCitizenRequest;
import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.CitizenStatus;
import com.govtech.platform.common.enums.Role;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.DuplicateResourceException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CitizenService}.
 *
 * <p>Uses {@link MockitoExtension} (STRICT_STUBS). No Spring context is loaded;
 * all collaborators are mocked. {@code ReferenceGenerator.generateCitizenReference()}
 * is a static utility that runs for real — the test only asserts the reference is non-null.</p>
 */
@ExtendWith(MockitoExtension.class)
class CitizenServiceTest {

    @Mock private CitizenRepository citizenRepository;
    @Mock private UserRepository    userRepository;
    @Mock private PasswordEncoder   passwordEncoder;

    @InjectMocks
    private CitizenService citizenService;

    // ── Fixture helpers ───────────────────────────────────────────────────

    private CreateCitizenRequest validCreateRequest() {
        return CreateCitizenRequest.builder()
                .name("Kamal Perera")
                .nic("199012345678")
                .email("kamal@email.com")
                .mobileNumber("+94771234567")
                .address("123 Main Street, Colombo 03")
                .temporaryPassword("TempPass@123")
                .build();
    }

    private Citizen buildCitizen(String citizenRef, String email, CitizenStatus status) {
        User user = User.builder()
                .username(email)
                .password("encoded")
                .role(Role.CITIZEN)
                .active(status == CitizenStatus.ACTIVE)
                .mustChangePassword(true)
                .build();
        return Citizen.builder()
                .citizenReference(citizenRef)
                .user(user)
                .name("Kamal Perera")
                .nic("199012345678")
                .email(email)
                .mobileNumber("+94771234567")
                .address("123 Main Street, Colombo 03")
                .status(status)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // createCitizen
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void createCitizen_shouldCreateUserAndCitizen_whenRequestIsValid() {
        CreateCitizenRequest request = validCreateRequest();

        when(citizenRepository.existsByNic("199012345678")).thenReturn(false);
        when(userRepository.existsByUsername("kamal@email.com")).thenReturn(false);
        when(passwordEncoder.encode("TempPass@123")).thenReturn("$2a$10$hashedPassword");

        CitizenCreatedResponse response = citizenService.createCitizen(request);

        // Response fields
        assertThat(response.getName()).isEqualTo("Kamal Perera");
        assertThat(response.getNic()).isEqualTo("199012345678");
        assertThat(response.getEmail()).isEqualTo("kamal@email.com");
        assertThat(response.getStatus()).isEqualTo(CitizenStatus.ACTIVE);
        assertThat(response.isMustChangePassword()).isTrue();
        assertThat(response.getCitizenReference()).isNotNull();
        assertThat(response.getTemporaryPasswordNote()).isNotNull();

        // User saved with encoded password, correct role, mustChangePassword=true
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("kamal@email.com");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.CITIZEN);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.isMustChangePassword()).isTrue();

        // Citizen saved
        verify(citizenRepository).save(any(Citizen.class));
    }

    @Test
    void createCitizen_shouldThrowDuplicateResource_whenNicAlreadyExists() {
        CreateCitizenRequest request = validCreateRequest();

        when(citizenRepository.existsByNic("199012345678")).thenReturn(true);

        assertThatThrownBy(() -> citizenService.createCitizen(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("199012345678");

        verify(userRepository, never()).save(any());
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void createCitizen_shouldThrowDuplicateResource_whenEmailAlreadyRegistered() {
        CreateCitizenRequest request = validCreateRequest();

        when(citizenRepository.existsByNic("199012345678")).thenReturn(false);
        when(userRepository.existsByUsername("kamal@email.com")).thenReturn(true);

        assertThatThrownBy(() -> citizenService.createCitizen(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("kamal@email.com");

        verify(userRepository, never()).save(any());
        verify(citizenRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════
    // deactivateCitizen
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void deactivateCitizen_shouldSetCitizenInactiveAndDisableUserAccount() {
        Citizen citizen = buildCitizen("CIT-ABCD1234", "kamal@email.com", CitizenStatus.ACTIVE);

        when(citizenRepository.findByCitizenReference("CIT-ABCD1234"))
                .thenReturn(Optional.of(citizen));

        citizenService.deactivateCitizen("CIT-ABCD1234");

        assertThat(citizen.getStatus()).isEqualTo(CitizenStatus.INACTIVE);
        assertThat(citizen.getUser().isActive()).isFalse();

        verify(citizenRepository).save(citizen);
        verify(userRepository).save(citizen.getUser());
    }

    @Test
    void deactivateCitizen_shouldThrowBusinessException_whenCitizenIsAlreadyInactive() {
        Citizen citizen = buildCitizen("CIT-ABCD1234", "kamal@email.com", CitizenStatus.INACTIVE);

        when(citizenRepository.findByCitizenReference("CIT-ABCD1234"))
                .thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> citizenService.deactivateCitizen("CIT-ABCD1234"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CIT-ABCD1234")
                .hasMessageContaining("already inactive");

        verify(citizenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateCitizen_shouldThrowResourceNotFound_whenCitizenDoesNotExist() {
        when(citizenRepository.findByCitizenReference("CIT-UNKNOWN1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenService.deactivateCitizen("CIT-UNKNOWN1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CIT-UNKNOWN1");

        verify(citizenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
