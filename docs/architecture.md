# Architecture

## Overview

The Realtime Matchmaking System is a Spring Boot-based backend service designed to match players for multiplayer games in real-time. The architecture follows a layered approach with separation of concerns.

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│                  (Game Client / Web UI)                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTP / WebSocket
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      API Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Player     │  │    Queue     │  │    Match     │      │
│  │ Controller   │  │ Controller   │  │ Controller   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Player     │  │   Queue      │  │  Matchmaking │      │
│  │   Service    │  │   Service    │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Data Access Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Player     │  │   Queue      │  │    Match     │      │
│  │ Repository   │  │ Repository   │  │ Repository   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
┌──────────────────┐          ┌──────────────────┐
│   PostgreSQL    │          │      Redis       │
│   (Persistent)  │          │   (Queue Cache)  │
└──────────────────┘          └──────────────────┘
```

## Technology Stack

### Backend Framework
- **Spring Boot 4.1.0** - Application framework
- **Spring Data JPA** - ORM and database access
- **Spring Data Redis** - Redis integration for queue management
- **Spring Validation** - Request validation
- **Spring WebSocket** - Real-time communication (planned)

### Database
- **PostgreSQL 17** - Primary database for persistent data
- **Redis 8** - In-memory data store for queue management

### Database Migration
- **Flyway** - Version-controlled database schema migrations

### API Documentation
- **SpringDoc OpenAPI** - Swagger UI for API testing

### Build Tool
- **Maven** - Dependency management and build automation

### Java Version
- **Java 21** - Latest LTS Java version

## Package Structure

```
com.matchmaking
├── config/              # Configuration classes (OpenAPI, etc.)
├── constants/           # Enums (PlayerStatus, Region)
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects
│   ├── request/         # Request DTOs
│   └── response/        # Response DTOs
├── entity/              # JPA entities
├── exceptions/          # Custom exceptions and global handler
├── mapper/              # Entity-DTO mappers
├── repository/          # Spring Data JPA repositories
├── service/             # Service interfaces
│   └── impl/            # Service implementations
├── scheduler/           # Scheduled tasks (matchmaking worker)
├── websocket/           # WebSocket handlers (planned)
├── security/            # Security configuration (planned)
├── util/                # Utility classes
└── model/               # Domain models
```

## Data Flow

### Player Registration Flow
```
Client → PlayerController → PlayerService → PlayerRepository → PostgreSQL
```

### Queue Join Flow
```
Client → QueueController → QueueService → Redis (Sorted Set)
```

### Matchmaking Flow
```
Scheduled Task → MatchmakingService → Redis (Read Queue) → Match Algorithm
→ MatchRepository → PostgreSQL → WebSocket Notification → Client
```

## Key Design Decisions

### 1. Layered Architecture
- Clear separation between presentation, business logic, and data access
- Easy to test and maintain individual components

### 2. DTO Pattern
- Entities never exposed to API layer
- Prevents over-fetching and under-fetching data
- Enables API evolution without breaking database schema

### 3. Repository Pattern
- Spring Data JPA generates CRUD operations automatically
- Custom query methods defined by convention

### 4. Redis for Queue Management
- Extremely fast read/write operations
- Sorted sets enable efficient range queries by Elo
- Temporary state doesn't need persistence

### 5. Scheduled Matchmaking
- Decouples matching from HTTP requests
- Consistent matching regardless of client activity
- Enables complex matching algorithms without blocking

### 6. Flyway Migrations
- Version-controlled database schema
- Reproducible deployments across environments
- Team collaboration on schema changes

## Scalability Considerations

### Current Architecture
- Single-node deployment
- Suitable for development and small-scale production

### Future Scaling Options
- **Horizontal Scaling**: Deploy multiple instances behind a load balancer
- **Database Sharding**: Shard players by region
- **Redis Cluster**: Distribute queue data across multiple nodes
- **Message Queue**: Use RabbitMQ/Kafka for distributed matchmaking
- **Caching Layer**: Add Redis caching for frequently accessed player data

## Security Considerations (Planned)
- JWT-based authentication
- Role-based access control
- Rate limiting on API endpoints
- Input validation and sanitization
- SQL injection prevention (via JPA)
- CORS configuration for frontend integration
