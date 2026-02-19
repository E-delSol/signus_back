CREATE TABLE users (
                       id VARCHAR(36) PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name VARCHAR(255),
                       created_at BIGINT NOT NULL
);

CREATE TABLE semaphores (
                            id VARCHAR(36) PRIMARY KEY,
                            user_id VARCHAR(36) REFERENCES users(id),
                            status VARCHAR(20) NOT NULL,
                            expiration BIGINT,
                            duration BIGINT
);
