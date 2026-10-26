-- =============================================================================
-- AUTOMATIC TIMETABLE GENERATOR - COMPLETE DATABASE & DATA SETUP SCRIPT
-- =============================================================================
-- Run this single script in MySQL to create the database, tables, and sample data.
-- After running this script:
-- 1. Configure your db.properties with your MySQL connection credentials.
-- 2. Run PasswordSeeder once to set initial BCrypt password hashes.
-- 3. Launch the application via Main.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS timetable_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE timetable_db;

-- -----------------------------------------------------------------------------
-- 1. Drop existing tables in reverse order of foreign keys
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS timetable_entries;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS teacher_unavailability;
DROP TABLE IF EXISTS time_slots;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS users;

-- -----------------------------------------------------------------------------
-- 2. Table Schemas
-- -----------------------------------------------------------------------------

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'TEACHER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE teachers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    max_weekly_hours DECIMAL(4, 1) NOT NULL DEFAULT 18.0,
    CONSTRAINT fk_teachers_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    requires_lab BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE classes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT UNSIGNED NOT NULL DEFAULT 40,
    room_type ENUM('CLASSROOM', 'LAB') NOT NULL DEFAULT 'CLASSROOM'
) ENGINE=InnoDB;

CREATE TABLE time_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL,
    period_number TINYINT UNSIGNED NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    UNIQUE KEY uq_day_period (day_of_week, period_number)
) ENGINE=InnoDB;

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

CREATE TABLE timetable_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT DEFAULT NULL,
    class_id INT NOT NULL,
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    room_id INT NOT NULL,
    slot_id INT NOT NULL,

    UNIQUE KEY uq_teacher_slot (teacher_id, slot_id),
    UNIQUE KEY uq_class_slot (class_id, slot_id),
    UNIQUE KEY uq_room_slot (room_id, slot_id),

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

-- -----------------------------------------------------------------------------
-- 3. Standard Time Slots (30 slots: Monday - Saturday, Periods 1 to 5)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 4. Admin User Record
-- -----------------------------------------------------------------------------
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin', 'placeholder_seed_with_password_seeder', 'System Administrator', 'ADMIN');

-- -----------------------------------------------------------------------------
-- 5. College Sample Data
-- -----------------------------------------------------------------------------

-- 9 Classes
INSERT INTO classes (name) VALUES
('CSE-A'),
('CSE-B'),
('CSE-C'),
('AIDS-A'),
('AIDS-B'),
('AIML-A'),
('AIML-B'),
('BLOCKCHAIN-A'),
('IT-A');

-- 14 Theory Subjects
INSERT INTO subjects (code, name, requires_lab) VALUES
('CS301', 'Data Structures', FALSE),
('CS302', 'DBMS', FALSE),
('CS303', 'Operating Systems', FALSE),
('CS304', 'Java OOPs', FALSE),
('CS305', 'Discrete Maths', FALSE),
('CS306', 'Computer Networks', FALSE),
('AI301', 'Machine Learning', FALSE),
('AI302', 'Deep Learning', FALSE),
('AI303', 'Data Visualization', FALSE),
('AI304', 'Statistics for Data Science', FALSE),
('BC301', 'Blockchain Basics', FALSE),
('BC302', 'Smart Contracts', FALSE),
('IT301', 'Web Technologies', FALSE),
('CS307', 'Software Engineering', FALSE);

-- 6 Lab Subjects
INSERT INTO subjects (code, name, requires_lab) VALUES
('CS311', 'DSA Lab', TRUE),
('CS312', 'DBMS Lab', TRUE),
('CS313', 'Java Lab', TRUE),
('AI311', 'ML Lab', TRUE),
('BC311', 'Blockchain Lab', TRUE),
('IT311', 'Web Tech Lab', TRUE);

-- 16 Rooms (12 Classrooms + 4 Labs)
INSERT INTO rooms (name, capacity, room_type) VALUES
('Classroom 101', 60, 'CLASSROOM'),
('Classroom 102', 60, 'CLASSROOM'),
('Classroom 103', 60, 'CLASSROOM'),
('Classroom 104', 60, 'CLASSROOM'),
('Classroom 105', 60, 'CLASSROOM'),
('Classroom 106', 60, 'CLASSROOM'),
('Classroom 107', 60, 'CLASSROOM'),
('Classroom 108', 60, 'CLASSROOM'),
('Classroom 109', 60, 'CLASSROOM'),
('Classroom 110', 60, 'CLASSROOM'),
('Classroom 111', 60, 'CLASSROOM'),
('Classroom 112', 60, 'CLASSROOM'),
('Computer Lab 1', 40, 'LAB'),
('Computer Lab 2', 40, 'LAB'),
('Computer Lab 3', 40, 'LAB'),
('Computer Lab 4', 40, 'LAB');

-- 16 Teacher Users
INSERT INTO users (username, password_hash, full_name, role) VALUES
('rkumar',     'placeholder_seed_with_password_seeder', 'Dr. Rajesh Kumar', 'TEACHER'),
('sverma',     'placeholder_seed_with_password_seeder', 'Dr. Sunita Verma', 'TEACHER'),
('apatel',     'placeholder_seed_with_password_seeder', 'Prof. Amit Patel', 'TEACHER'),
('asen',       'placeholder_seed_with_password_seeder', 'Dr. Ananya Sen', 'TEACHER'),
('vmalhotra',  'placeholder_seed_with_password_seeder', 'Prof. Vikram Malhotra', 'TEACHER'),
('piyer',      'placeholder_seed_with_password_seeder', 'Dr. Pooja Iyer', 'TEACHER'),
('sreddy',     'placeholder_seed_with_password_seeder', 'Prof. Suresh Reddy', 'TEACHER'),
('knair',      'placeholder_seed_with_password_seeder', 'Dr. Kavita Nair', 'TEACHER'),
('msharma',    'placeholder_seed_with_password_seeder', 'Prof. Manoj Sharma', 'TEACHER'),
('djoshi',     'placeholder_seed_with_password_seeder', 'Dr. Deepa Joshi', 'TEACHER'),
('rgupta',     'placeholder_seed_with_password_seeder', 'Prof. Rohan Gupta', 'TEACHER'),
('mrao',       'placeholder_seed_with_password_seeder', 'Dr. Meenakshi Rao', 'TEACHER'),
('ssingh',     'placeholder_seed_with_password_seeder', 'Prof. Sandeep Singh', 'TEACHER'),
('dchawla',    'placeholder_seed_with_password_seeder', 'Dr. Divya Chawla', 'TEACHER'),
('hbhat',      'placeholder_seed_with_password_seeder', 'Prof. Harish Bhat', 'TEACHER'),
('naggarwal',  'placeholder_seed_with_password_seeder', 'Dr. Nidhi Aggarwal', 'TEACHER');

-- 16 Teachers
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'rkumar';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'sverma';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 17.0 FROM users WHERE username = 'apatel';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 16.0 FROM users WHERE username = 'asen';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'vmalhotra';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 17.0 FROM users WHERE username = 'piyer';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 19.0 FROM users WHERE username = 'sreddy';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 16.0 FROM users WHERE username = 'knair';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'msharma';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 17.0 FROM users WHERE username = 'djoshi';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'rgupta';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 16.0 FROM users WHERE username = 'mrao';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 19.0 FROM users WHERE username = 'ssingh';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 17.0 FROM users WHERE username = 'dchawla';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 18.0 FROM users WHERE username = 'hbhat';
INSERT INTO teachers (user_id, max_weekly_hours) SELECT id, 16.0 FROM users WHERE username = 'naggarwal';

-- Class Subject Assignments (26 periods/week per class)
INSERT INTO assignments (teacher_id, subject_id, class_id, periods_per_week) VALUES
-- CSE-A
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rkumar'),
 (SELECT id FROM subjects WHERE code = 'CS301'), (SELECT id FROM classes WHERE name = 'CSE-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'apatel'),
 (SELECT id FROM subjects WHERE code = 'CS302'), (SELECT id FROM classes WHERE name = 'CSE-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'vmalhotra'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'CSE-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS304'), (SELECT id FROM classes WHERE name = 'CSE-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'naggarwal'),
 (SELECT id FROM subjects WHERE code = 'CS305'), (SELECT id FROM classes WHERE name = 'CSE-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'msharma'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'CSE-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rkumar'),
 (SELECT id FROM subjects WHERE code = 'CS311'), (SELECT id FROM classes WHERE name = 'CSE-A'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS313'), (SELECT id FROM classes WHERE name = 'CSE-A'), 2),

-- CSE-B
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rkumar'),
 (SELECT id FROM subjects WHERE code = 'CS301'), (SELECT id FROM classes WHERE name = 'CSE-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'apatel'),
 (SELECT id FROM subjects WHERE code = 'CS302'), (SELECT id FROM classes WHERE name = 'CSE-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'vmalhotra'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'CSE-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS304'), (SELECT id FROM classes WHERE name = 'CSE-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'naggarwal'),
 (SELECT id FROM subjects WHERE code = 'CS305'), (SELECT id FROM classes WHERE name = 'CSE-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'msharma'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'CSE-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rkumar'),
 (SELECT id FROM subjects WHERE code = 'CS311'), (SELECT id FROM classes WHERE name = 'CSE-B'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'apatel'),
 (SELECT id FROM subjects WHERE code = 'CS312'), (SELECT id FROM classes WHERE name = 'CSE-B'), 2),

-- CSE-C
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sverma'),
 (SELECT id FROM subjects WHERE code = 'CS301'), (SELECT id FROM classes WHERE name = 'CSE-C'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'apatel'),
 (SELECT id FROM subjects WHERE code = 'CS302'), (SELECT id FROM classes WHERE name = 'CSE-C'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'vmalhotra'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'CSE-C'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS304'), (SELECT id FROM classes WHERE name = 'CSE-C'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'piyer'),
 (SELECT id FROM subjects WHERE code = 'CS307'), (SELECT id FROM classes WHERE name = 'CSE-C'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'msharma'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'CSE-C'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rkumar'),
 (SELECT id FROM subjects WHERE code = 'CS311'), (SELECT id FROM classes WHERE name = 'CSE-C'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'apatel'),
 (SELECT id FROM subjects WHERE code = 'CS312'), (SELECT id FROM classes WHERE name = 'CSE-C'), 2),

-- AIDS-A
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rgupta'),
 (SELECT id FROM subjects WHERE code = 'AI301'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'mrao'),
 (SELECT id FROM subjects WHERE code = 'AI302'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'ssingh'),
 (SELECT id FROM subjects WHERE code = 'AI303'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'dchawla'),
 (SELECT id FROM subjects WHERE code = 'AI304'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sverma'),
 (SELECT id FROM subjects WHERE code = 'CS301'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'piyer'),
 (SELECT id FROM subjects WHERE code = 'CS305'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rgupta'),
 (SELECT id FROM subjects WHERE code = 'AI311'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sverma'),
 (SELECT id FROM subjects WHERE code = 'CS311'), (SELECT id FROM classes WHERE name = 'AIDS-A'), 2),

-- AIDS-B
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rgupta'),
 (SELECT id FROM subjects WHERE code = 'AI301'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'mrao'),
 (SELECT id FROM subjects WHERE code = 'AI302'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'ssingh'),
 (SELECT id FROM subjects WHERE code = 'AI303'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'dchawla'),
 (SELECT id FROM subjects WHERE code = 'AI304'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'asen'),
 (SELECT id FROM subjects WHERE code = 'CS302'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'piyer'),
 (SELECT id FROM subjects WHERE code = 'CS305'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rgupta'),
 (SELECT id FROM subjects WHERE code = 'AI311'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'asen'),
 (SELECT id FROM subjects WHERE code = 'CS312'), (SELECT id FROM classes WHERE name = 'AIDS-B'), 2),

-- AIML-A
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'rgupta'),
 (SELECT id FROM subjects WHERE code = 'AI301'), (SELECT id FROM classes WHERE name = 'AIML-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'ssingh'),
 (SELECT id FROM subjects WHERE code = 'AI302'), (SELECT id FROM classes WHERE name = 'AIML-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'ssingh'),
 (SELECT id FROM subjects WHERE code = 'AI303'), (SELECT id FROM classes WHERE name = 'AIML-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'dchawla'),
 (SELECT id FROM subjects WHERE code = 'AI304'), (SELECT id FROM classes WHERE name = 'AIML-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'vmalhotra'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'AIML-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'knair'),
 (SELECT id FROM subjects WHERE code = 'CS304'), (SELECT id FROM classes WHERE name = 'AIML-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'mrao'),
 (SELECT id FROM subjects WHERE code = 'AI311'), (SELECT id FROM classes WHERE name = 'AIML-A'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS313'), (SELECT id FROM classes WHERE name = 'AIML-A'), 2),

-- AIML-B
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'mrao'),
 (SELECT id FROM subjects WHERE code = 'AI301'), (SELECT id FROM classes WHERE name = 'AIML-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'ssingh'),
 (SELECT id FROM subjects WHERE code = 'AI302'), (SELECT id FROM classes WHERE name = 'AIML-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'dchawla'),
 (SELECT id FROM subjects WHERE code = 'AI303'), (SELECT id FROM classes WHERE name = 'AIML-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'hbhat'),
 (SELECT id FROM subjects WHERE code = 'AI304'), (SELECT id FROM classes WHERE name = 'AIML-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'vmalhotra'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'AIML-B'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'msharma'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'AIML-B'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'mrao'),
 (SELECT id FROM subjects WHERE code = 'AI311'), (SELECT id FROM classes WHERE name = 'AIML-B'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sreddy'),
 (SELECT id FROM subjects WHERE code = 'CS313'), (SELECT id FROM classes WHERE name = 'AIML-B'), 2),

-- BLOCKCHAIN-A
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'hbhat'),
 (SELECT id FROM subjects WHERE code = 'BC301'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'hbhat'),
 (SELECT id FROM subjects WHERE code = 'BC302'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'djoshi'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'sverma'),
 (SELECT id FROM subjects WHERE code = 'CS301'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'piyer'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'djoshi'),
 (SELECT id FROM subjects WHERE code = 'IT301'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'hbhat'),
 (SELECT id FROM subjects WHERE code = 'BC311'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'djoshi'),
 (SELECT id FROM subjects WHERE code = 'IT311'), (SELECT id FROM classes WHERE name = 'BLOCKCHAIN-A'), 2),

-- IT-A
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'djoshi'),
 (SELECT id FROM subjects WHERE code = 'IT301'), (SELECT id FROM classes WHERE name = 'IT-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'asen'),
 (SELECT id FROM subjects WHERE code = 'CS302'), (SELECT id FROM classes WHERE name = 'IT-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'knair'),
 (SELECT id FROM subjects WHERE code = 'CS304'), (SELECT id FROM classes WHERE name = 'IT-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'msharma'),
 (SELECT id FROM subjects WHERE code = 'CS306'), (SELECT id FROM classes WHERE name = 'IT-A'), 4),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'naggarwal'),
 (SELECT id FROM subjects WHERE code = 'CS307'), (SELECT id FROM classes WHERE name = 'IT-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'piyer'),
 (SELECT id FROM subjects WHERE code = 'CS303'), (SELECT id FROM classes WHERE name = 'IT-A'), 3),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'djoshi'),
 (SELECT id FROM subjects WHERE code = 'IT311'), (SELECT id FROM classes WHERE name = 'IT-A'), 2),
((SELECT t.id FROM teachers t JOIN users u ON t.user_id = u.id WHERE u.username = 'knair'),
 (SELECT id FROM subjects WHERE code = 'CS313'), (SELECT id FROM classes WHERE name = 'IT-A'), 2);
