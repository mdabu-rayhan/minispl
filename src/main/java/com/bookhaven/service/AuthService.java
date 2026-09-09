package com.bookhaven.service;

import com.bookhaven.dao.UserDAO;
import com.bookhaven.dao.impl.SQLiteUserDAO;
import com.bookhaven.model.User;
import com.bookhaven.model.UserFactory;
import com.bookhaven.util.PasswordHasher;
import com.bookhaven.util.SessionContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Business service managing user authentication, registration, and session lifecycle.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new SQLiteUserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates credentials and initializes the global SessionContext upon success.
     *
     * @param username the username
     * @param password plain text password
     * @return Optional containing the authenticated User, or empty if invalid
     */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHasher.checkPassword(password, user.getPasswordHash())) {
                SessionContext.getInstance().setCurrentUser(user);
                LOGGER.info("Authentication successful for user: " + user.getUsername() + " (" + user.getRole() + ")");
                return Optional.of(user);
            }
        }
        LOGGER.warning("Authentication failed for username: " + username);
        return Optional.empty();
    }

    /**
     * Registers a new Reader account with hashed credentials.
     *
     * @param username      desired username
     * @param plainPassword plain text password
     * @return the created ReaderUser
     * @throws IllegalArgumentException if username is taken or validation fails
     */
    public User registerReader(String username, String plainPassword) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (plainPassword == null || plainPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long");
        }

        String sanitizedUser = username.trim();
        if (userDAO.findByUsername(sanitizedUser).isPresent()) {
            throw new IllegalArgumentException("Username '" + sanitizedUser + "' is already registered");
        }

        String hashedPassword = PasswordHasher.hashPassword(plainPassword);
        User newReader = UserFactory.createReader(sanitizedUser, hashedPassword);
        User saved = userDAO.save(newReader);
        LOGGER.info("Registered new reader: " + saved.getUsername());
        return saved;
    }

    /**
     * Retrieves all registered users (for admin user monitoring).
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /**
     * Deletes a user by ID.
     */
    public boolean deleteUser(int userId) {
        return userDAO.delete(userId);
    }

    /**
     * Logs out the current active session.
     */
    public void logout() {
        SessionContext.getInstance().clearSession();
    }
}
