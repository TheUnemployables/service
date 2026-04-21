package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.exception.InvalidCredentialsException;
import ro.unibuc.prodeng.model.AppUserEntity;
import ro.unibuc.prodeng.repository.AppUserRepository;
import ro.unibuc.prodeng.request.ForgotPasswordRequest;
import ro.unibuc.prodeng.request.LoginRequest;
import ro.unibuc.prodeng.request.RegisterRequest;
import ro.unibuc.prodeng.response.LoginResponse;
import ro.unibuc.prodeng.response.UserProfileResponse;
import ro.unibuc.prodeng.utils.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AppUserService appUserService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void testRegister_newUser_returnsLoginResponseWithToken() {
        // Arrange
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret", "dev", false);
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        AppUserEntity saved = new AppUserEntity();
        saved.setId("user-1");
        saved.setName("Alice");
        saved.setEmail("alice@example.com");
        saved.setIsAdmin(false);
        when(appUserRepository.save(any(AppUserEntity.class))).thenReturn(saved);
        when(jwtUtil.generateToken("user-1", "alice@example.com", false)).thenReturn("jwt-token");

        // Act
        LoginResponse response = appUserService.register(req);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Alice", response.name());
        assertEquals("alice@example.com", response.email());
        assertFalse(response.isAdmin());
        verify(appUserRepository).save(any(AppUserEntity.class));
    }

    @Test
    void testRegister_adminUser_setsAdminFlagInToken() {
        // Arrange
        RegisterRequest req = new RegisterRequest("Bob", "bob@example.com", "secret", "ops", true);
        when(appUserRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        AppUserEntity saved = new AppUserEntity();
        saved.setId("user-2");
        saved.setName("Bob");
        saved.setEmail("bob@example.com");
        saved.setIsAdmin(true);
        when(appUserRepository.save(any(AppUserEntity.class))).thenReturn(saved);
        when(jwtUtil.generateToken("user-2", "bob@example.com", true)).thenReturn("admin-token");

        // Act
        LoginResponse response = appUserService.register(req);

        // Assert
        assertEquals("admin-token", response.token());
        assertTrue(response.isAdmin());
        verify(jwtUtil).generateToken("user-2", "bob@example.com", true);
    }

    @Test
    void testRegister_duplicateEmail_throwsIllegalArgumentException() {
        // Arrange
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret", "dev", false);
        AppUserEntity existing = new AppUserEntity();
        existing.setEmail("alice@example.com");
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> appUserService.register(req));
        verify(appUserRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void testLogin_validCredentials_returnsLoginResponse() {
        // Arrange
        LoginRequest req = new LoginRequest("alice@example.com", "secret");
        AppUserEntity user = new AppUserEntity();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        user.setIsAdmin(false);
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user-1", "alice@example.com", false)).thenReturn("jwt-token");

        // Act
        LoginResponse response = appUserService.login(req);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Alice", response.name());
        assertEquals("alice@example.com", response.email());
    }

    @Test
    void testLogin_emailNotFound_throwsInvalidCredentialsException() {
        // Arrange
        LoginRequest req = new LoginRequest("nobody@example.com", "secret");
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> appUserService.login(req));
        verify(jwtUtil, never()).generateToken(any(), any(), anyBoolean());
    }

    @Test
    void testLogin_wrongPassword_throwsInvalidCredentialsException() {
        // Arrange
        LoginRequest req = new LoginRequest("alice@example.com", "wrong");
        AppUserEntity user = new AppUserEntity();
        user.setPasswordHash("hashed");
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> appUserService.login(req));
        verify(jwtUtil, never()).generateToken(any(), any(), anyBoolean());
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Test
    void testForgotPassword_existingEmail_updatesPasswordHash() {
        // Arrange
        ForgotPasswordRequest req = new ForgotPasswordRequest("alice@example.com", "newSecret");
        AppUserEntity user = new AppUserEntity();
        user.setEmail("alice@example.com");
        user.setPasswordHash("old-hash");
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newSecret")).thenReturn("new-hash");
        when(appUserRepository.save(any(AppUserEntity.class))).thenReturn(user);

        // Act
        appUserService.forgotPassword(req);

        // Assert
        assertEquals("new-hash", user.getPasswordHash());
        verify(appUserRepository).save(user);
    }

    @Test
    void testForgotPassword_emailNotFound_throwsEntityNotFoundException() {
        // Arrange
        ForgotPasswordRequest req = new ForgotPasswordRequest("nobody@example.com", "newSecret");
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> appUserService.forgotPassword(req));
        verify(appUserRepository, never()).save(any());
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    void testGetProfile_existingUser_returnsUserProfileResponse() throws EntityNotFoundException {
        // Arrange
        AppUserEntity user = new AppUserEntity();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setGroup("dev");
        user.setIsAdmin(false);
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse profile = appUserService.getProfile("user-1");

        // Assert
        assertNotNull(profile);
        assertEquals("user-1", profile.id());
        assertEquals("Alice", profile.name());
        assertEquals("alice@example.com", profile.email());
        assertEquals("dev", profile.group());
        assertFalse(profile.isAdmin());
    }

    @Test
    void testGetProfile_nonExistingUser_throwsEntityNotFoundException() {
        // Arrange
        when(appUserRepository.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> appUserService.getProfile("unknown"));
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void testDeleteUser_existingUser_deletesSuccessfully() throws EntityNotFoundException {
        // Arrange
        when(appUserRepository.existsById("user-1")).thenReturn(true);

        // Act
        appUserService.deleteUser("user-1");

        // Assert
        verify(appUserRepository).deleteById("user-1");
    }

    @Test
    void testDeleteUser_nonExistingUser_throwsEntityNotFoundException() {
        // Arrange
        when(appUserRepository.existsById("unknown")).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> appUserService.deleteUser("unknown"));
        verify(appUserRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteUser_nullId_throwsEntityNotFoundException() {
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> appUserService.deleteUser(null));
        verify(appUserRepository, never()).deleteById(any());
    }
}
