-- Initial Seed Data for BookHaven
-- Passwords will be verified with BCrypt by PasswordHasher

-- Users (admin / admin123, reader1 / reader123, jane_doe / reader123)
-- BCrypt hash for 'admin123' and 'reader123'
INSERT OR IGNORE INTO users (id, username, password_hash, role) VALUES
(1, 'admin', '$2a$10$wK6n2yJ/4tFzTf1i9jF70eQoD.yF7E79Bup4C5y.PvhN4m2sKzYyW', 'ADMIN'),
(2, 'reader1', '$2a$10$mB5OszmP8C4bZf4X3pC2v.fG7Z0Rz7Yh0m0mE1m9m8j2h1e0g3Kqu', 'READER'),
(3, 'jane_doe', '$2a$10$mB5OszmP8C4bZf4X3pC2v.fG7Z0Rz7Yh0m0mE1m9m8j2h1e0g3Kqu', 'READER');
