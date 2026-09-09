package com.bookhaven.dao;

import com.bookhaven.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for User entities.
 */
public interface UserDAO {
    Optional<User> findById(int id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    User save(User user);
    boolean update(User user);
    boolean delete(int id);
    long count();
}
