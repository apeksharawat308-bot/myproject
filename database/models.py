CREATE DATABASE road_db;
USE road_db;

-- USER TABLE
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(255)
);

-- COMPLAINT TABLE
CREATE TABLE complaint (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    description TEXT,
    location VARCHAR(100),
    image VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',

    FOREIGN KEY (user_id) REFERENCES user(id)
);