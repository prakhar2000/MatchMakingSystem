# API Documentation

## Base URL
```
http://localhost:8080
```

## API Documentation (Swagger UI)
```
http://localhost:8080/swagger-ui/index.html
```

## Authentication
Currently, the API does not require authentication. JWT-based authentication will be added in future iterations.

---

## Player API

### Create Player
**POST** `/players`

Creates a new player account.

**Request Body:**
```json
{
  "username": "gamer123",
  "email": "gamer@example.com",
  "region": "US_EAST"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "username": "gamer123",
  "email": "gamer@example.com",
  "elo": 1000,
  "wins": 0,
  "losses": 0,
  "region": "US_EAST",
  "status": "OFFLINE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Validation Rules:**
- `username`: Required, 3-20 characters
- `email`: Required, must be valid email format
- `region`: Required, must be one of: ASIA, EUROPE, US_EAST, US_WEST, SOUTH_AMERICA

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Username or email already exists

---

### Get All Players
**GET** `/players`

Retrieves all players in the system.

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "username": "gamer123",
    "email": "gamer@example.com",
    "elo": 1000,
    "wins": 0,
    "losses": 0,
    "region": "US_EAST",
    "status": "OFFLINE",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

---

### Get Player by ID
**GET** `/players/{id}`

Retrieves a specific player by their ID.

**Path Parameters:**
- `id` (Long) - Player ID

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "gamer123",
  "email": "gamer@example.com",
  "elo": 1000,
  "wins": 0,
  "losses": 0,
  "region": "US_EAST",
  "status": "OFFLINE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Error Responses:**
- `404 Not Found` - Player not found

---

### Update Player
**PUT** `/players/{id}`

Updates an existing player's information.

**Path Parameters:**
- `id` (Long) - Player ID

**Request Body:**
```json
{
  "username": "gamer456",
  "email": "newemail@example.com",
  "region": "EUROPE"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "gamer456",
  "email": "newemail@example.com",
  "elo": 1000,
  "wins": 0,
  "losses": 0,
  "region": "EUROPE",
  "status": "OFFLINE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**Note:** All fields are optional. Only provided fields will be updated.

**Error Responses:**
- `400 Bad Request` - Validation failed
- `404 Not Found` - Player not found
- `409 Conflict` - Username or email already exists

---

### Delete Player
**DELETE** `/players/{id}`

Deletes a player from the system.

**Path Parameters:**
- `id` (Long) - Player ID

**Response (204 No Content)**

**Error Responses:**
- `404 Not Found` - Player not found

---

## Queue API (Planned)

### Join Queue
**POST** `/queue/join`

Adds a player to the matchmaking queue.

**Request Body:**
```json
{
  "playerId": 1,
  "gameMode": "RANKED"
}
```

**Response (200 OK):**
```json
{
  "playerId": 1,
  "status": "IN_QUEUE",
  "queuePosition": 5,
  "estimatedWaitTime": 120
}
```

---

### Leave Queue
**POST** `/queue/leave`

Removes a player from the matchmaking queue.

**Request Body:**
```json
{
  "playerId": 1
}
```

**Response (200 OK):**
```json
{
  "playerId": 1,
  "status": "OFFLINE"
}
```

---

### Get Queue Status
**GET** `/queue/status/{playerId}`

Retrieves the current queue status for a player.

**Path Parameters:**
- `playerId` (Long) - Player ID

**Response (200 OK):**
```json
{
  "playerId": 1,
  "status": "IN_QUEUE",
  "queuePosition": 3,
  "estimatedWaitTime": 60
}
```

---

## Match API (Planned)

### Get Match History
**GET** `/matches`

Retrieves match history for a player.

**Query Parameters:**
- `playerId` (Long) - Player ID (optional)
- `limit` (Integer) - Maximum number of results (default: 10)

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "createdAt": "2024-01-15T12:00:00",
    "averageElo": 1450,
    "region": "US_EAST",
    "status": "COMPLETED",
    "players": [
      {
        "playerId": 1,
        "team": "TEAM_A"
      },
      {
        "playerId": 2,
        "team": "TEAM_B"
      }
    ]
  }
]
```

---

## Leaderboard API (Planned)

### Get Leaderboard
**GET** `/leaderboard`

Retrieves the global leaderboard.

**Query Parameters:**
- `region` (String) - Filter by region (optional)
- `limit` (Integer) - Maximum number of results (default: 50)

**Response (200 OK):**
```json
[
  {
    "rank": 1,
    "playerId": 1,
    "username": "pro_gamer",
    "elo": 1850,
    "wins": 45,
    "losses": 10,
    "winRate": 0.82
  }
]
```

---

## Error Response Format

All error responses follow this format:

```json
{
  "status": 404,
  "message": "Player not found with id: 999",
  "timestamp": "2024-01-15T10:30:00"
}
```

For validation errors:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2024-01-15T10:30:00",
  "errors": {
    "username": "Username must be between 3 and 20 characters",
    "email": "Email must be valid"
  }
}
```

---

## HTTP Status Codes

| Code | Description |
|------|-------------|
| 200  | OK |
| 201  | Created |
| 204  | No Content |
| 400  | Bad Request |
| 404  | Not Found |
| 409  | Conflict |
| 500  | Internal Server Error |

---

## Enums

### PlayerStatus
- `ONLINE` - Player is online and available
- `OFFLINE` - Player is offline
- `IN_QUEUE` - Player is in matchmaking queue
- `IN_MATCH` - Player is currently in a match

### Region
- `ASIA` - Asia region
- `EUROPE` - Europe region
- `US_EAST` - US East region
- `US_WEST` - US West region
- `SOUTH_AMERICA` - South America region

### GameMode (Planned)
- `RANKED` - Competitive ranked matches
- `CASUAL` - Casual matches
- `TOURNAMENT` - Tournament matches
