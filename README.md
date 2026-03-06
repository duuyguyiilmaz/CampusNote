# CampusNote

CampusNote is a mobile application designed to support academic collaboration among university students.  
The application encourages students to share academic notes within their departments and access notes uploaded by other students.

The system is based on contribution fairness: students must upload at least one academic note in order to access other shared notes.

The main purpose of the application is to promote:

- Academic collaboration
- Structured note sharing
- Quality-based ranking within departments


# Target Users

Akdeniz University undergraduate students.

The application is designed specifically for university students who want to share and access course notes within their department.


# Core Features (MVP)

- University e-mail login
- Department selection
- Notes feed with department-based filtering
- Note upload (PDF or image format)
- Note rating system
- Leaderboard / points system
- Report mechanism for inappropriate or empty uploads


# Database & Network Operations

As required by the course guidelines, the project includes database and network connection operations.

The application uses Firebase services to manage authentication and real-time database operations.


# Database Structure (Cloud Firestore)

The application stores data using Firebase Cloud Firestore, a NoSQL cloud database.

## Users
- id (Firebase UID)
- email
- department
- points

## Notes
- id
- title
- description
- uploaderUid
- uploaderEmail
- department
- createdAt
- avgRating

## Ratings
- id
- noteId
- userId
- value

## Reports
- id
- noteId
- userId
- reason
- createdAt


# Network Operations

The application performs several network-based operations including:

- User authentication
- Uploading notes
- Fetching notes from the database
- Submitting ratings
- Reporting inappropriate content
- Real-time note listing using Firestore listeners


# Technical Stack

## Mobile Platform
Kotlin (Android)

## Authentication
Firebase Authentication

## Database
Firebase Cloud Firestore

## File Storage
Cloud-based storage for uploaded notes (planned feature)


# Purpose of the Project

CampusNote was developed as part of a Mobile Programming course project.  
The project demonstrates the implementation of mobile UI design, database integration, and network communication using modern Android development tools.

