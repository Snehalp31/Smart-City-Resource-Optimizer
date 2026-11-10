-- MySQL 8+ schema. The Java application can initialize equivalent tables on startup.
CREATE DATABASE IF NOT EXISTS smartcity CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartcity;

CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(120) PRIMARY KEY,
  salt VARCHAR(200) NOT NULL,
  password_hash VARCHAR(300) NOT NULL,
  role VARCHAR(20) NOT NULL
);
CREATE TABLE IF NOT EXISTS areas (
  name VARCHAR(200) PRIMARY KEY,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL
);
CREATE TABLE IF NOT EXISTS roads (
  area_a VARCHAR(200) NOT NULL,
  area_b VARCHAR(200) NOT NULL,
  distance_km INT NOT NULL CHECK (distance_km > 0),
  PRIMARY KEY (area_a, area_b),
  FOREIGN KEY (area_a) REFERENCES areas(name),
  FOREIGN KEY (area_b) REFERENCES areas(name)
);
CREATE TABLE IF NOT EXISTS resource_centers (
  area_name VARCHAR(200) PRIMARY KEY,
  FOREIGN KEY (area_name) REFERENCES areas(name)
);
CREATE TABLE IF NOT EXISTS resources (
  resource_id VARCHAR(120) PRIMARY KEY,
  area_name VARCHAR(200) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  driver_name VARCHAR(200) NOT NULL,
  available BOOLEAN NOT NULL,
  FOREIGN KEY (area_name) REFERENCES resource_centers(area_name)
);
CREATE TABLE IF NOT EXISTS emergency_requests (
  request_id VARCHAR(80) PRIMARY KEY,
  requester_id VARCHAR(120) NOT NULL,
  emergency_type VARCHAR(100) NOT NULL,
  description TEXT,
  location_area VARCHAR(200) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  priority INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  resource_id VARCHAR(120),
  route_text TEXT,
  created_at VARCHAR(50) NOT NULL,
  FOREIGN KEY (requester_id) REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS activity_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurred_at VARCHAR(50) NOT NULL,
  actor_id VARCHAR(120),
  action_text TEXT NOT NULL
);
