CREATE TABLE IF NOT EXISTS users (username VARCHAR(50) PRIMARY KEY, password VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL);
CREATE TABLE IF NOT EXISTS authorities (username VARCHAR(50) NOT NULL, authority VARCHAR(50) NOT NULL, CONSTRAINT fk_auth FOREIGN KEY(username) REFERENCES users(username), UNIQUE(username, authority));

-- Only Sarvam-supplied names; survives Docker restart through the existing H2 volume.
CREATE TABLE IF NOT EXISTS sarvam_name_cache (
 language VARCHAR(40) NOT NULL,
 gender VARCHAR(20) NOT NULL,
 prefix VARCHAR(30) NOT NULL,
 name_key VARCHAR(80) NOT NULL,
 given_name VARCHAR(80) NOT NULL,
 meaning VARCHAR(400) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
 PRIMARY KEY (language, gender, prefix, name_key)
);
