-- =============================================================================
-- COLLEGE SAMPLE DATA SEEDING SCRIPT
-- =============================================================================

USE timetable_db;

-- Clean existing data while preserving admin user and time_slots
DELETE FROM timetable_entries;
DELETE FROM assignments;
DELETE FROM teacher_unavailability;
DELETE FROM classes;
DELETE FROM subjects;
DELETE FROM rooms;
DELETE FROM teachers;
DELETE FROM users WHERE role = 'TEACHER';

-- 1. Insert 9 College Classes
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

-- 2. Insert 14 Theory Subjects (requires_lab = FALSE)
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

-- 3. Insert 6 Lab Subjects (requires_lab = TRUE)
INSERT INTO subjects (code, name, requires_lab) VALUES
('CS311', 'DSA Lab', TRUE),
('CS312', 'DBMS Lab', TRUE),
('CS313', 'Java Lab', TRUE),
('AI311', 'ML Lab', TRUE),
('BC311', 'Blockchain Lab', TRUE),
('IT311', 'Web Tech Lab', TRUE);

-- 4. Insert 16 Rooms (12 Classrooms + 4 Computer Labs)
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

-- 5. Insert 16 Teacher Users
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

-- 6. Insert 16 Teachers with Max Weekly Hours
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'rkumar';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'sverma';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 17.0 FROM users WHERE username = 'apatel';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 16.0 FROM users WHERE username = 'asen';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'vmalhotra';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 17.0 FROM users WHERE username = 'piyer';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 19.0 FROM users WHERE username = 'sreddy';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 16.0 FROM users WHERE username = 'knair';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'msharma';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 17.0 FROM users WHERE username = 'djoshi';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'rgupta';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 16.0 FROM users WHERE username = 'mrao';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 19.0 FROM users WHERE username = 'ssingh';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 17.0 FROM users WHERE username = 'dchawla';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 18.0 FROM users WHERE username = 'hbhat';
INSERT INTO teachers (user_id, max_weekly_hours)
SELECT id, 16.0 FROM users WHERE username = 'naggarwal';

-- 7. Insert Class Subject Assignments (26 periods/week per class)
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
