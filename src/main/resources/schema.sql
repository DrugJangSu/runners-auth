CREATE DATABASE IF NOT EXISTS runners_auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE runners_auth;

CREATE TABLE crew_runner (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             login_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
                             password_hash CHAR(60) NOT NULL,
                             nickname VARCHAR(20) NOT NULL,
                             role VARCHAR(10) NOT NULL,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_crew_runner_login_id (login_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;