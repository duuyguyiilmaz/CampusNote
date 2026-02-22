# CampusNote

The system is based on contribution fairness:  
Students must upload at least one academic note in order to access other shared notes.
The main purpose of the application is to encourage academic collaboration, structured note sharing, and quality-based ranking within departments.
## Target Users
Akdeniz University undergraduate students.
## Core Features (MVP)

- University e-mail login
- Department selection
- Course selection
- Notes feed (course-based filtering)
- Note upload (PDF or image format)
- Note rating system
- Leaderboard / points system
- Report mechanism for inappropriate or empty uploads

- ## Database & Network Tasks

As required by the course guidelines, the project includes database and network connection operations.

### Planned Database Structure

- Users (id, name, email, departmentId, points)
- Departments (id, name)
- Courses (id, departmentId, name)
- Notes (id, courseId, uploaderId, title, fileUrl, createdAt, avgRating)
- Ratings (id, noteId, userId, value)
- Reports (id, noteId, userId, reason, createdAt)

### Network Operations

- Authentication (user login)
- Uploading notes (PDF/Image)
- Downloading and displaying notes
- Submitting ratings
- Reporting inappropriate content
- Real-time note listing per course


## Technical Stack (Planned)

- Mobile Platform: Kotlin (Android)
- Backend & Database: To be finalized (Firebase or Spring Boot + SQL)
- File Storage: Cloud-based storage for uploaded notes


