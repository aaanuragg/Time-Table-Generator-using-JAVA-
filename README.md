# Automatic Teacher Workload and Timetable Generator

An automated desktop application built with Java Swing and MySQL that generates clash-free college timetables respecting teacher workloads, room capacities, consecutive lab blocks, and home classrooms.

---

## Features

- **Admin and Teacher Roles**: Role-based access control where administrators manage master data and generate timetables, while teachers can log in, view personal weekly schedules, and declare unavailable time slots.
- **Automatic Clash-Free Scheduling**: Backtracking algorithm that guarantees zero conflicts across teachers, class sections, and rooms.
- **Consecutive Lab Blocks**: Automatically schedules 2-period practical lab sessions back-to-back on the same day in designated computer lab rooms.
- **Home Classrooms**: Assigns a consistent dedicated classroom to each class section to minimize unnecessary room transitions throughout the week.
- **Constraint Verification**: Built-in 9-point audit system validating hard constraints (teacher/class/room clashes, availability, workload limits, room types, lab block continuity) and soft quality criteria.
- **PDF and Excel Export**: Exports clean, presentation-ready timetables with formatted 3-line cells (Subject, Teacher/Class, Room), distinct light-blue shading for lab sessions, and date-stamped footers. Export individual timetables or batch export all classes and teachers.

---

## Tech Stack

- **Java 17**: Core language runtime.
- **Java Swing**: Desktop GUI components.
- **FlatLaf (v3.4.1)**: Modern, clean flat look-and-feel theme for Swing.
- **JDBC & MySQL (v8.3.0)**: Relational database persistence with relational integrity constraints.
- **jBCrypt (v0.4)**: Secure salted password hashing for user authentication.
- **OpenPDF (v1.3.30)**: Landscape A4 timetable PDF generation.
- **Apache POI (v5.2.5)**: Spreadsheet generation and cell styling for Excel (.xlsx) export.

---

## How the Scheduling Algorithm Works

1. The scheduling engine models timetable generation as a Constraint Satisfaction Problem (CSP) solved using recursive backtracking search.
2. At each step, it selects the most-constrained assignment first using the Minimum Remaining Values (MRV) heuristic, prioritizing 2-period lab blocks before theory periods.
3. For the selected assignment, the generator identifies valid time slots and rooms where all hard constraints are satisfied, ensuring no teacher, class, or room is double-booked.
4. Candidate slots are scored using soft penalties that discourage scheduling the same subject multiple times on the same day and penalize long unbroken teaching stretches.
5. The algorithm explores the lowest-penalty candidate first, assigning each class's home classroom to theory lessons whenever possible to minimize room switching.
6. If a branch reaches a dead end, the algorithm undoes recent assignments and backtracks to explore alternative placements, capturing the highest placement count as a best partial attempt if time runs out.
7. Once a complete clash-free schedule is found, an iterative improvement pass runs up to 500 rounds to eliminate any remaining duplicate subjects on the same day by moving or swapping periods of the same class.

---

## Folder Structure

```
TimeTable/
├── database/
│   ├── schema.sql              # Database creation, tables, constraints, and time slots
│   ├── college_data.sql        # Sample classes, subjects, rooms, teachers, and assignments
│   └── setup_all.sql           # Unified script to set up database and seed sample data
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── timetable/
│       │           ├── Main.java                # Application launcher and DB check
│       │           ├── dao/                     # Data Access Objects (CRUD operations)
│       │           │   ├── AssignmentDAO.java
│       │           │   ├── ClassDAO.java
│       │           │   ├── RoomDAO.java
│       │           │   ├── SubjectDAO.java
│       │           │   ├── TeacherDAO.java
│       │           │   ├── TimeSlotDAO.java
│       │           │   ├── TimetableDAO.java
│       │           │   └── UserDAO.java
│       │           ├── db/                      # Database connection and password seeder
│       │           │   ├── DBConnection.java
│       │           │   └── PasswordSeeder.java
│       │           ├── model/                   # Domain models and entities
│       │           │   ├── Assignment.java
│       │           │   ├── ClassSection.java
│       │           │   ├── DayOfWeek.java
│       │           │   ├── Role.java
│       │           │   ├── Room.java
│       │           │   ├── Subject.java
│       │           │   ├── Teacher.java
│       │           │   ├── TeacherUnavailability.java
│       │           │   ├── TimeSlot.java
│       │           │   ├── TimetableEntry.java
│       │           │   └── User.java
│       │           ├── service/                 # Core scheduling and export services
│       │           │   ├── GenerationResult.java
│       │           │   ├── Session.java
│       │           │   ├── TimetableExporter.java
│       │           │   ├── TimetableGenerator.java
│       │           │   └── TimetableVerifier.java
│       │           └── ui/                      # Swing GUI frames and panels
│       │               ├── AdminDashboardFrame.java
│       │               ├── AssignmentPanel.java
│       │               ├── ClassPanel.java
│       │               ├── GeneratePanel.java
│       │               ├── LoginFrame.java
│       │               ├── RoomPanel.java
│       │               ├── SubjectPanel.java
│       │               ├── TeacherDashboardFrame.java
│       │               ├── TeacherPanel.java
│       │               └── ViewTimetablePanel.java
│       └── resources/
│           ├── db.properties.example            # Template database connection file
│           └── db.properties                    # Local connection config (git-ignored)
├── .gitignore
├── pom.xml
└── README.md
```

---

## Step-by-Step Setup

### 1. Prerequisites
- **Java Development Kit (JDK 17 or higher)**
- **MySQL Server (v8.0 or higher)**
- **Apache Maven (optional, or build using IDE)**

### 2. Set Up Database
Open MySQL command-line or MySQL Workbench and run `database/setup_all.sql`:
```bash
mysql -u root -p < database/setup_all.sql
```
This script creates the `timetable_db` database, all necessary tables, constraints, standard time slots (Monday–Saturday, 09:00–14:00), and sample college data.

### 3. Configure Database Credentials
Copy the example properties file:
```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```
Edit `src/main/resources/db.properties` with your MySQL connection credentials:
```properties
db.url=jdbc:mysql://localhost:3306/timetable_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_mysql_password
```

### 4. Run Password Seeder (Run Once)
Run `PasswordSeeder` to hash and store BCrypt credentials for default users:
```bash
# Using Maven:
mvn compile exec:java -Dexec.mainClass="com.timetable.db.PasswordSeeder"

# Or directly in your IDE:
# Run com.timetable.db.PasswordSeeder.main()
```

### 5. Launch Application
Run `Main` to open the application login screen:
```bash
# Using Maven:
mvn exec:java -Dexec.mainClass="com.timetable.Main"

# Or directly in your IDE:
# Run com.timetable.Main.main()
```

---

## Default Logins

| Role | Username | Default Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | Full access: classes, subjects, rooms, teachers, assignments, generation, export |
| **Teacher** | `rkumar` | `teacher123` | View assigned weekly timetable, declare slot unavailability, export personal PDF |
| **Teacher** | `apatel` | `teacher123` | (All seeded teacher accounts use `teacher123` as default password) |

> **Security Note**: Default passwords should be changed immediately after initial deployment.

---

## Screenshots

*Screenshots will be placed here.*

- **Login Screen**: `![Login Screen](docs/screenshots/login.png)`
- **Admin Dashboard**: `![Admin Dashboard](docs/screenshots/admin_dashboard.png)`
- **Timetable Generator & Verification**: `![Generation Diagnostics](docs/screenshots/generator.png)`
- **Class Timetable View**: `![Timetable Grid](docs/screenshots/timetable_view.png)`
- **Teacher Dashboard**: `![Teacher Dashboard](docs/screenshots/teacher_view.png)`
- **Exported PDF Sample**: `![PDF Sample](docs/screenshots/pdf_export.png)`

---

## Known Limitations

1. **Fixed Period Grid**: The timetable template is configured for 6 days (Monday through Saturday) and 5 periods per day (30 weekly slots).
2. **Homogeneous Lab Blocks**: Practical sessions are assumed to require 2 consecutive periods. 3-period lab sessions are not currently supported.
3. **Single Shift Operation**: Operates on a single institutional shift schedule (09:00 to 14:00).
4. **Local Swing Architecture**: Runs as a desktop client application requiring local database network accessibility.

---

## Future Improvements

1. **Flexible Time Grid**: Custom period durations, variable break times (e.g. lunch periods), and adjustable working days.
2. **Electives & Parallel Batches**: Scheduling elective buckets where multiple subjects run simultaneously for sub-groups in the same class.
3. **Web / Mobile Interface**: REST API backend using Spring Boot with a modern web interface.
4. **Automated Teacher Substitution**: Dynamic recommendation system to find available substitute teachers when a faculty member reports leave.
