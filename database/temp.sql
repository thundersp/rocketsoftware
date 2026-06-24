CREATE DATABASE IF NOT EXISTS meeting_scheduler
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE TABLE time_zones
(
    time_zone_id        INT AUTO_INCREMENT PRIMARY KEY,
    zone_name           VARCHAR(64) NOT NULL UNIQUE,
    gmt_offset_minutes  INT NOT NULL,
    is_dst_supported    BOOLEAN NOT NULL DEFAULT TRUE
);

