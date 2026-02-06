# Ktor-r2dbc

A Ktor application demonstrating reactive database access using R2DBC with an H2 in-memory database.

## Features

- **Ktor Framework**: Lightweight and flexible Kotlin web framework
- **R2DBC**: Reactive Relational Database Connectivity for non-blocking database access
- **H2 In-Memory Database**: Fast, lightweight database perfect for development and testing
- **RESTful API**: CRUD operations for user management
- **Connection Pooling**: Efficient database connection management
- **Kotlin Coroutines**: Seamless integration with reactive streams

## Prerequisites

- JDK 17 or higher
- Gradle (wrapper included)

## Getting Started

### Build the Project

```bash
./gradlew build
```

### Run the Application

```bash
./gradlew run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Get All Users
```bash
GET http://localhost:8080/users
```

### Get User by ID
```bash
GET http://localhost:8080/users/{id}
```

### Create User
```bash
POST http://localhost:8080/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com"
}
```

### Delete User
```bash
DELETE http://localhost:8080/users/{id}
```

## Database Schema

The application automatically creates a `users` table with the following structure:

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
)
```

Sample data is automatically inserted on startup.

## Technology Stack

- **Ktor**: 2.3.7
- **Kotlin**: 1.9.22
- **R2DBC H2**: 1.0.0.RELEASE
- **R2DBC Pool**: 1.0.1.RELEASE
- **Kotlin Serialization**: 1.6.2
- **Kotlinx Coroutines**: 1.7.3

## Project Structure

```
.
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts       # Gradle settings
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/example/
│       │       └── Application.kt  # Main application file
│       └── resources/
│           ├── application.conf    # Ktor configuration
│           └── logback.xml        # Logging configuration
└── README.md
```

## Development

The application uses an H2 in-memory database, which means:
- Data is stored in memory only
- All data is lost when the application stops
- Perfect for development and testing
- No external database setup required

## License

MIT