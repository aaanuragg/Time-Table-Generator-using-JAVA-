-- =============================================================================
-- AUTOMATIC TIMETABLE GENERATOR - DATABASE SCHEMA
-- =============================================================================

CREATE DATABASE IF NOT EXISTS timetable_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE timetable_db;

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS timetable_entries;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS teacher_unavailability;
DROP TABLE IF EXISTS time_slots;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS users;

-- 1. Users Table (Authentication & Role: ADMIN or TEACHER)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'TEACHER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Teachers Table (Linked 1:1 with users)
CREATE TABLE teachers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    max_weekly_hours DECIMAL(4, 1) NOT NULL DEFAULT 18.0,
    CONSTRAINT fk_teachers_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 3. Subjects Table
CREATE TABLE subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    requires_lab BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

-- 4. Classes / Sections Table
CREATE TABLE classes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 5. Rooms Table (CLASSROOM or LAB)
CREATE TABLE rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT UNSIGNED NOT NULL DEFAULT 40,
    room_type ENUM('CLASSROOM', 'LAB') NOT NULL DEFAULT 'CLASSROOM'
) ENGINE=InnoDB;

-- 6. Time Slots Table (Mon-Sat x 5 periods = 30 slots)
CREATE TABLE time_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL,
    period_number TINYINT UNSIGNED NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    UNIQUE KEY uq_day_period (day_of_week, period_number)
) ENGINE=InnoDB;

-- 7. Teacher Unavailability Table
CREATE TABLE teacher_unavailability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    slot_id INT NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    UNIQUE KEY uq_teacher_slot_unavail (teacher_id, slot_id),
    CONSTRAINT fk_unavail_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_unavail_slot
        FOREIGN KEY (slot_id) REFERENCES time_slots(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 8. Assignments Table (Teacher + Subject + Class + Weekly Periods)
CREATE TABLE assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    class_id INT NOT NULL,
    periods_per_week TINYINT UNSIGNED NOT NULL CHECK (periods_per_week > 0),
    UNIQUE KEY uq_teacher_subject_class (teacher_id, subject_id, class_id),
    CONSTRAINT fk_assign_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_assign_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_assign_class
        FOREIGN KEY (class_id) REFERENCES classes(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 9. Generated Timetable Entries (With Clash Prevention Constraints)
CREATE TABLE timetable_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT DEFAULT NULL,
    class_id INT NOT NULL,
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    room_id INT NOT NULL,
    slot_id INT NOT NULL,

    -- Hard clash-free uniqueness guarantees
    UNIQUE KEY uq_teacher_slot (teacher_id, slot_id),  -- No teacher double-booked
    UNIQUE KEY uq_class_slot (class_id, slot_id),      -- No class double-booked
    UNIQUE KEY uq_room_slot (room_id, slot_id),        -- No room double-booked

    CONSTRAINT fk_tt_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_tt_class
        FOREIGN KEY (class_id) REFERENCES classes(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_tt_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_tt_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_tt_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_tt_slot
        FOREIGN KEY (slot_id) REFERENCES time_slots(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Seed 30 Standard Time Slots (6 days x 5 periods)
INSERT INTO time_slots (day_of_week, period_number, start_time, end_time) VALUES
('MONDAY', 1, '09:00:00', '10:00:00'),
('MONDAY', 2, '10:00:00', '11:00:00'),
('MONDAY', 3, '11:00:00', '12:00:00'),
('MONDAY', 4, '12:00:00', '13:00:00'),
('MONDAY', 5, '13:00:00', '14:00:00'),

('TUESDAY', 1, '09:00:00', '10:00:00'),
('TUESDAY', 2, '10:00:00', '11:00:00'),
('TUESDAY', 3, '11:00:00', '12:00:00'),
('TUESDAY', 4, '12:00:00', '13:00:00'),
('TUESDAY', 5, '13:00:00', '14:00:00'),

('WEDNESDAY', 1, '09:00:00', '10:00:00'),
('WEDNESDAY', 2, '10:00:00', '11:00:00'),
('WEDNESDAY', 3, '11:00:00', '12:00:00'),
('WEDNESDAY', 4, '12:00:00', '13:00:00'),
('WEDNESDAY', 5, '13:00:00', '14:00:00'),

('THURSDAY', 1, '09:00:00', '10:00:00'),
('THURSDAY', 2, '10:00:00', '11:00:00'),
('THURSDAY', 3, '11:00:00', '12:00:00'),
('THURSDAY', 4, '12:00:00', '13:00:00'),
('THURSDAY', 5, '13:00:00', '14:00:00'),

('FRIDAY', 1, '09:00:00', '10:00:00'),
('FRIDAY', 2, '10:00:00', '11:00:00'),
('FRIDAY', 3, '11:00:00', '12:00:00'),
('FRIDAY', 4, '12:00:00', '13:00:00'),
('FRIDAY', 5, '13:00:00', '14:00:00'),

('SATURDAY', 1, '09:00:00', '10:00:00'),
('SATURDAY', 2, '10:00:00', '11:00:00'),
('SATURDAY', 3, '11:00:00', '12:00:00'),
('SATURDAY', 4, '12:00:00', '13:00:00'),
('SATURDAY', 5, '13:00:00', '14:00:00');

-- Seed Default Admin User (Password hash will be set by PasswordSeeder to 'admin123')
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin', 'placeholder_seed_with_password_seeder', 'System Administrator', 'ADMIN');
