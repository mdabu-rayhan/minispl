package com.bookhaven.util;

import com.bookhaven.model.User;

/**
 * Singleton context holding the currently logged-in user and session state.
 */
public class SessionContext {

    private static volatile SessionContext instance;
    private User currentUser;

    private SessionContext() {
        // Private constructor for Singleton
    }

    /**
     * Retrieves the Singleton instance of SessionContext.
     *
     * @return the singleton instance
     */
    public static SessionContext getInstance() {
        if (instance == null) {
            synchronized (SessionContext.class) {
                if (instance == null) {
                    instance = new SessionContext();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves the active authenticated user.
     *
     * @return the current User or null if not logged in
     */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the active authenticated user for the session.
     *
     * @param user the logged in User
     */
    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if logged in, false otherwise
     */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the currently authenticated user has an ADMIN role.
     *
     * @return true if admin, false otherwise
     */
    public synchronized boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Checks if the currently authenticated user has a READER role.
     *
     * @return true if reader, false otherwise
     */
    public synchronized boolean isReader() {
        return currentUser != null && !currentUser.isAdmin();
    }

    /**
     * Clears the current user session (Log out).
     */
    public synchronized void clearSession() {
        this.currentUser = null;
    }
}
