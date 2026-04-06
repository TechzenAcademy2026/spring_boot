SET search_path TO app;

-- 1. Add new security columns to existing users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Rename password_hash → password (to match Spring Security convention)
ALTER TABLE users RENAME COLUMN password_hash TO password;

-- 3. Seed admin user (password: 123456 hashed with BCrypt)
INSERT INTO users (username, email, password, role, is_active)
VALUES
    ('admin', 'admin@techzen.vn',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'ADMIN', TRUE),
    ('teacher01', 'teacher01@techzen.vn',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'USER', TRUE)
ON CONFLICT (username) DO NOTHING;
