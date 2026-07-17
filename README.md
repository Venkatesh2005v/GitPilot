# GitPilot

GitPilot is a Spring Boot application designed to manage and sync selected GitHub repositories for authenticated users.

## Features

- **GitHub OAuth2 Login**: Secure user authentication via GitHub.
- **Repository Selection**: Users can select which repositories they want to track and persist in PostgreSQL.
- **Optimized Persistence**: Efficient batched updates and O(1) lookups to minimize database overhead.

## Technologies

- **Java 21 / Spring Boot 4.x**
- **Spring Security (OAuth2 Client)**
- **PostgreSQL**
- **Spring Data JPA**
- **RestClient**
