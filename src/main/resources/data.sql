-- Sample admin user (password: Admin@123)
INSERT INTO users (username, email, password, role, created_at, updated_at)
VALUES ('admin', 'admin@benchmarkiq.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4oTKZ.l8/i',
        'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample regular user (password: User@123)
INSERT INTO users (username, email, password, role, created_at, updated_at)
VALUES ('testuser', 'testuser@benchmarkiq.com',
        '$2a$12$8kzaGKoR1p8VTGCKQMzBEuBj5z2LX9oRsNJvAjBuqTH4I4yLfJb7q',
        'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
