package com.govtech.platform.servicerequest.service;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.CitizenStatus;
import com.govtech.platform.common.enums.Role;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.InvalidStatusException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.common.exception.UnauthorizedActionException;
import com.govtech.platform.notification.repository.NotificationRepository;
import com.govtech.platform.servicerequest.dto.CreateServiceRequestRequest;
import com.govtech.platform.servicerequest.dto.ServiceRequestResponse;
import com.govtech.platform.servicerequest.dto.StatusUpdateResponse;
import com.govtech.platform.servicerequest.dto.UpdateServiceRequestStatusRequest;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import com.govtech.platform.servicerequest.repository.ServiceRequestRepository;
import com.govtech.platform.statushistory.repository.StatusHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ServiceRequestService}.
 *
 * <p>Uses {@link MockitoExtension} (STRICT_STUBS). All stubs are scoped to the test
 * that needs them to avoid unnecessary-stubbing failures. The Spring Security context
 * is set up manually per test and cleared in {@link #tearDown()}.</p>
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private CitizenRepository        citizenRepository;
    @Mock private StatusHistoryRepository  statusHistoryRepository;
    @Mock private NotificationRepository   notificationRepository;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    // Shared test fixtures — rebuilt fresh for every test
    private User    citizenUser;
    private Citizen citizen;

    @BeforeEach
    void setUp() {
        citizenUser = User.builder()
                .username("citizen@gov.lk")
                .password("encoded_password")
                .role(Role.CITIZEN)
                .active(true)
                .mustChangePassword(false)
                .build();

        citizen = Citizen.builder()
                .citizenReference("CIT-ABCD1234")
                .user(citizenUser)
                .name("John Citizen")
                .nic("S1234567A")
                .email("citizen@gov.lk")
                .mobileNumber("91234567")
                .address("123 Main St, Singapore")
                .status(CitizenStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Security context helper ───────────────────────────────────────────

    private void authenticateAs(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // ── ServiceRequest entity helper ──────────────────────────────────────

    private ServiceRequest buildSr(String ref, ServiceRequestStatus status) {
        return ServiceRequest.builder()
                .requestReference(ref)
                .citizen(citizen)
                .serviceType("PASSPORT_RENEWAL")
                .description("Passport renewal request.")
                .status(status)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // createServiceRequest — CITIZEN caller
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void createServiceRequest_shouldCreateRequest_whenCalledByCitizen() {
        authenticateAs("citizen@gov.lk", "CITIZEN");

        CreateServiceRequestRequest request = CreateServiceRequestRequest.builder()
                .serviceType("PASSPORT_RENEWAL")
                .description("Renewing expired passport.")
                .build();

        when(citizenRepository.findByUser_Username("citizen@gov.lk"))
                .thenReturn(Optional.of(citizen));
        when(serviceRequestRepository.existsByRequestReference(anyString()))
                .thenReturn(false);
        // Return value of save IS used: serviceRequest = serviceRequestRepository.save(serviceRequest)
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.createServiceRequest(request);

        assertThat(response.getServiceType()).isEqualTo("PASSPORT_RENEWAL");
        assertThat(response.getDescription()).isEqualTo("Renewing expired passport.");
        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(response.getCitizenReference()).isEqualTo("CIT-ABCD1234");
        assertThat(response.getCitizenName()).isEqualTo("John Citizen");
        assertThat(response.getRequestReference()).isNotNull();

        verify(serviceRequestRepository).save(any(ServiceRequest.class));
        verify(statusHistoryRepository).save(any());  // initial SUBMITTED history
        verify(notificationRepository, never()).save(any()); // no notification on creation
    }

    @Test
    void createServiceRequest_shouldThrowResourceNotFound_whenNoCitizenProfileLinkedToAccount() {
        authenticateAs("ghost@gov.lk", "CITIZEN");

        CreateServiceRequestRequest request = CreateServiceRequestRequest.builder()
                .serviceType("PASSPORT_RENEWAL")
                .description("Renewal request.")
                .build();

        when(citizenRepository.findByUser_Username("ghost@gov.lk"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.createServiceRequest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No citizen profile linked to account");

        verify(serviceRequestRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════
    // createServiceRequest — ADMIN caller
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void createServiceRequest_shouldCreateRequest_whenCalledByAdmin() {
        authenticateAs("admin@gov.lk", "ADMIN");

        CreateServiceRequestRequest request = CreateServiceRequestRequest.builder()
                .serviceType("ID_REPLACEMENT")
                .description("Lost ID card replacement.")
                .citizenReference("CIT-ABCD1234")
                .build();

        when(citizenRepository.findByCitizenReference("CIT-ABCD1234"))
                .thenReturn(Optional.of(citizen));
        when(serviceRequestRepository.existsByRequestReference(anyString()))
                .thenReturn(false);
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.createServiceRequest(request);

        assertThat(response.getServiceType()).isEqualTo("ID_REPLACEMENT");
        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(response.getCitizenReference()).isEqualTo("CIT-ABCD1234");

        verify(citizenRepository).findByCitizenReference("CIT-ABCD1234");
        verify(serviceRequestRepository).save(any(ServiceRequest.class));
    }

    @Test
    void createServiceRequest_shouldThrowBusinessException_whenAdminOmitsCitizenReference() {
        authenticateAs("admin@gov.lk", "ADMIN");

        CreateServiceRequestRequest request = CreateServiceRequestRequest.builder()
                .serviceType("ID_REPLACEMENT")
                .description("Lost ID card.")
                .build(); // no citizenReference

        assertThatThrownBy(() -> serviceRequestService.createServiceRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("citizenReference is required");

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void createServiceRequest_shouldThrowResourceNotFound_whenAdminProvidesBadCitizenReference() {
        authenticateAs("admin@gov.lk", "ADMIN");

        CreateServiceRequestRequest request = CreateServiceRequestRequest.builder()
                .serviceType("ID_REPLACEMENT")
                .description("Lost ID card.")
                .citizenReference("CIT-UNKNOWN99")
                .build();

        when(citizenRepository.findByCitizenReference("CIT-UNKNOWN99"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.createServiceRequest(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CIT-UNKNOWN99");

        verify(serviceRequestRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════
    // updateServiceRequestStatus
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void updateServiceRequestStatus_shouldTransitionStatus_whenSubmittedToInReview() {
        authenticateAs("agent@gov.lk", "SERVICE_AGENT");

        ServiceRequest sr = buildSr("REQ-ABCD1234", ServiceRequestStatus.SUBMITTED);

        UpdateServiceRequestStatusRequest request = UpdateServiceRequestStatusRequest.builder()
                .status(ServiceRequestStatus.IN_REVIEW)
                .remarks("Starting review.")
                .build();

        when(serviceRequestRepository.findByRequestReference("REQ-ABCD1234"))
                .thenReturn(Optional.of(sr));

        StatusUpdateResponse response = serviceRequestService
                .updateServiceRequestStatus("REQ-ABCD1234", request);

        assertThat(response.getRequestReference()).isEqualTo("REQ-ABCD1234");
        assertThat(response.getPreviousStatus()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(response.getNewStatus()).isEqualTo(ServiceRequestStatus.IN_REVIEW);
        assertThat(response.getRemarks()).isEqualTo("Starting review.");

        // Status mutated on entity
        assertThat(sr.getStatus()).isEqualTo(ServiceRequestStatus.IN_REVIEW);

        verify(serviceRequestRepository).save(sr);
        verify(statusHistoryRepository).save(any());
        verify(notificationRepository).save(any());
    }

    @Test
    void updateServiceRequestStatus_shouldThrowResourceNotFound_whenRequestNotFound() {
        authenticateAs("agent@gov.lk", "SERVICE_AGENT");

        UpdateServiceRequestStatusRequest request = UpdateServiceRequestStatusRequest.builder()
                .status(ServiceRequestStatus.IN_REVIEW)
                .build();

        when(serviceRequestRepository.findByRequestReference("REQ-MISSING0"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService
                .updateServiceRequestStatus("REQ-MISSING0", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("REQ-MISSING0");

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void updateServiceRequestStatus_shouldThrowInvalidStatus_whenCurrentStatusIsTerminal() {
        authenticateAs("agent@gov.lk", "SERVICE_AGENT");

        ServiceRequest sr = buildSr("REQ-DONE1234", ServiceRequestStatus.APPROVED);

        UpdateServiceRequestStatusRequest request = UpdateServiceRequestStatusRequest.builder()
                .status(ServiceRequestStatus.IN_REVIEW)
                .build();

        when(serviceRequestRepository.findByRequestReference("REQ-DONE1234"))
                .thenReturn(Optional.of(sr));

        assertThatThrownBy(() -> serviceRequestService
                .updateServiceRequestStatus("REQ-DONE1234", request))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("APPROVED");

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void updateServiceRequestStatus_shouldThrowInvalidStatus_whenTransitionSkipsStep() {
        // SUBMITTED → APPROVED is not a valid transition (must go via IN_REVIEW)
        authenticateAs("agent@gov.lk", "SERVICE_AGENT");

        ServiceRequest sr = buildSr("REQ-SKIP1234", ServiceRequestStatus.SUBMITTED);

        UpdateServiceRequestStatusRequest request = UpdateServiceRequestStatusRequest.builder()
                .status(ServiceRequestStatus.APPROVED)
                .build();

        when(serviceRequestRepository.findByRequestReference("REQ-SKIP1234"))
                .thenReturn(Optional.of(sr));

        assertThatThrownBy(() -> serviceRequestService
                .updateServiceRequestStatus("REQ-SKIP1234", request))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("SUBMITTED")
                .hasMessageContaining("APPROVED");

        verify(serviceRequestRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════
    // cancelServiceRequest
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void cancelServiceRequest_shouldSetStatusCancelled_whenRequestIsInReview() {
        authenticateAs("admin@gov.lk", "ADMIN");

        ServiceRequest sr = buildSr("REQ-CANC1234", ServiceRequestStatus.IN_REVIEW);

        when(serviceRequestRepository.findByRequestReference("REQ-CANC1234"))
                .thenReturn(Optional.of(sr));

        serviceRequestService.cancelServiceRequest("REQ-CANC1234");

        assertThat(sr.getStatus()).isEqualTo(ServiceRequestStatus.CANCELLED);
        verify(serviceRequestRepository).save(sr);
        verify(statusHistoryRepository).save(any());
        verify(notificationRepository).save(any());
    }

    @Test
    void cancelServiceRequest_shouldThrowResourceNotFound_whenRequestNotFound() {
        authenticateAs("admin@gov.lk", "ADMIN");

        when(serviceRequestRepository.findByRequestReference("REQ-GONE1234"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.cancelServiceRequest("REQ-GONE1234"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("REQ-GONE1234");

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void cancelServiceRequest_shouldThrowInvalidStatus_whenAlreadyCancelled() {
        authenticateAs("admin@gov.lk", "ADMIN");

        ServiceRequest sr = buildSr("REQ-ALRDY123", ServiceRequestStatus.CANCELLED);

        when(serviceRequestRepository.findByRequestReference("REQ-ALRDY123"))
                .thenReturn(Optional.of(sr));

        assertThatThrownBy(() -> serviceRequestService.cancelServiceRequest("REQ-ALRDY123"))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("CANCELLED");

        verify(serviceRequestRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════
    // getMyCitizenServiceRequests — ownership check
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void getMyCitizenServiceRequests_shouldThrowUnauthorized_whenCitizenRefBelongsToAnotherUser() {
        // "other@gov.lk" is authenticated but "CIT-ABCD1234" belongs to "citizen@gov.lk"
        authenticateAs("other@gov.lk", "CITIZEN");

        when(citizenRepository.findByCitizenReference("CIT-ABCD1234"))
                .thenReturn(Optional.of(citizen)); // citizen linked to "citizen@gov.lk"

        assertThatThrownBy(() ->
                serviceRequestService.getMyCitizenServiceRequests("CIT-ABCD1234", Pageable.unpaged()))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("CIT-ABCD1234");
    }
}
