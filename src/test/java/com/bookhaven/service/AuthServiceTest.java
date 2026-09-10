package com.bookhaven.service;

import com.bookhaven.dao.UserDAO;
import com.bookhaven.model.AdminUser;
import com.bookhaven.model.ReaderUser;
import com.bookhaven.model.User;
import com.bookhaven.util.PasswordHasher;
import com.bookhaven.util.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying AuthService registration, authentication, and session controls.
 */
public class AuthServiceTest {

    private AuthService authService;
    private List<User> memoryUsers;

    @BeforeEach
    public void setUp() {
        memoryUsers = new ArrayList<>();
        UserDAO mockUserDao = new UserDAO() {
            private int idGen = 1;
            @Override public Optional<User> findById(int id) { return memoryUsers.stream().filter(u -> u.getId() == id).findFirst(); }
            @Override public Optional<User> findByUsername(String username) { return memoryUsers.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst(); }
            @Override public List<User> findAll() { return new ArrayList<>(memoryUsers); }
            @Override public User save(User user) {
                user.setId(idGen++);
                memoryUsers.add(user);
                return user;
            }
            @Override public boolean update(User user) { return true; }
            @Override public boolean delete(int id) { return memoryUsers.removeIf(u -> u.getId() == id); }
            @Override public long count() { return memoryUsers.size(); }
        };

        // Seed initial admin
        mockUserDao.save(new AdminUser(0, "admin", PasswordHasher.hashPassword("admin123"), LocalDateTime.now()));

        authService = new AuthService(mockUserDao);
        SessionContext.getInstance().clearSession();
    }

    @Test
    public void testSuccessfulAuthentication() {
        Optional<User> userOpt = authService.authenticate("admin", "admin123");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals("admin", user.getUsername());
        assertTrue(user.isAdmin());
        assertTrue(SessionContext.getInstance().isLoggedIn());
        assertTrue(SessionContext.getInstance().isAdmin());
    }

    @Test
    public void testFailedAuthentication() {
        Optional<User> userOpt = authService.authenticate("admin", "wrongpassword");
        assertFalse(userOpt.isPresent());
        assertFalse(SessionContext.getInstance().isLoggedIn());

        Optional<User> nonExistent = authService.authenticate("nobody", "admin123");
        assertFalse(nonExistent.isPresent());
    }

    @Test
    public void testReaderRegistration() {
        User registered = authService.registerReader("john_reader", "securePass2026");
        assertNotNull(registered);
        assertTrue(registered instanceof ReaderUser);
        assertFalse(registered.isAdmin());
        assertEquals("john_reader", registered.getUsername());

        // Attempting duplicate registration should fail
        assertThrows(IllegalArgumentException.class, () -> {
            authService.registerReader("john_reader", "anotherPass");
        });
    }

    @Test
    public void testLogoutClearsSession() {
        authService.authenticate("admin", "admin123");
        assertTrue(SessionContext.getInstance().isLoggedIn());

        authService.logout();
        assertFalse(SessionContext.getInstance().isLoggedIn());
        assertNull(SessionContext.getInstance().getCurrentUser());
    }
}
