# Deployment Guide

## Prerequisites

- Docker and Docker Compose
- Java 21
- Maven 3.8+
- PostgreSQL 17 (if not using Docker)
- Redis 8 (if not using Docker)

---

## Local Development

### 1. Clone the Repository

```bash
git clone <repository-url>
cd matchmakingsystem
```

### 2. Start Infrastructure

Using Docker Compose:

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379

### 3. Run Database Migrations

Flyway migrations run automatically on application startup.

To run manually:

```bash
./mvnw flyway:migrate
```

### 4. Start the Application

```bash
./mvnw spring-boot:run
```

The application will be available at:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### 5. Verify Deployment

```bash
# Check PostgreSQL
docker exec -it matchmakingsystem-postgres-1 psql -U postgres -d matchmaking -c "\dt"

# Check Redis
docker exec -it matchmakingsystem-redis-1 redis-cli ping

# Check Application
curl http://localhost:8080/actuator/health
```

---

## Docker Deployment

### Build the Application

```bash
./mvnw clean package -DskipTests
```

### Create Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/realtime-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build Docker Image

```bash
docker build -t matchmaking-system:latest .
```

### Run with Docker Compose

Update `docker-compose.yml`:

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: matchmaking
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:8
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  app:
    image: matchmaking-system:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/matchmaking
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
  redis_data:
```

Start all services:

```bash
docker-compose up -d
```

---

## AWS Deployment

### Option 1: EC2 with Docker Compose

1. **Launch EC2 Instance**
   - AMI: Ubuntu 22.04 LTS
   - Instance type: t3.medium or larger
   - Security groups: Allow 8080, 22, 5432, 6379

2. **Install Docker**

```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo usermod -aG docker $USER
```

3. **Deploy Application**

```bash
# Clone repository
git clone <repository-url>
cd matchmakingsystem

# Build and start
docker-compose up -d
```

4. **Configure Domain and SSL**

Use Nginx as reverse proxy with Let's Encrypt:

```nginx
server {
    listen 80;
    server_name matchmaking.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name matchmaking.example.com;

    ssl_certificate /etc/letsencrypt/live/matchmaking.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/matchmaking.example.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Option 2: AWS ECS (Elastic Container Service)

1. **Push Image to ECR**

```bash
aws ecr create-repository --repository-name matchmaking-system

docker tag matchmaking-system:latest <account-id>.dkr.ecr.<region>.amazonaws.com/matchmaking-system:latest

aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com

docker push <account-id>.dkr.ecr.<region>.amazonaws.com/matchmaking-system:latest
```

2. **Create ECS Task Definition**

```json
{
  "family": "matchmaking-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "matchmaking-app",
      "image": "<account-id>.dkr.ecr.<region>.amazonaws.com/matchmaking-system:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://<rds-endpoint>:5432/matchmaking"
        },
        {
          "name": "SPRING_DATASOURCE_USERNAME",
          "value": "postgres"
        },
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "value": "${DB_PASSWORD}"
        },
        {
          "name": "SPRING_DATA_REDIS_HOST",
          "value": "<elasticache-endpoint>"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:<region>:<account-id>:secret:matchmaking/db-password"
        }
      ]
    }
  ]
}
```

3. **Create ECS Service**

```bash
aws ecs create-service \
  --cluster matchmaking-cluster \
  --service-name matchmaking-service \
  --task-definition matchmaking-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-12345],securityGroups=[sg-12345],assignPublicIp=ENABLED}"
```

### Option 3: AWS Elastic Beanstalk

1. **Create Application**

```bash
eb init -p java matchmaking-system
eb create matchmaking-env
```

2. **Configure Environment Variables**

In the Elastic Beanstalk console:
- Configuration → Software → Environment properties
- Add database and Redis connection strings

---

## Database Migration in Production

### Flyway Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### Migration Strategy

1. **Development**: Create migration files in `src/main/resources/db/migration`
2. **Testing**: Run migrations against test database
3. **Staging**: Run migrations to verify
4. **Production**: Migrations run automatically on deployment

### Rollback Strategy

Flyway doesn't support automatic rollbacks. For rollbacks:

1. Create a new migration that reverses the change
2. Deploy the rollback migration
3. Verify data integrity

---

## Monitoring and Logging

### Application Logs

```yaml
logging:
  level:
    com.matchmaking: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/matchmaking.log
```

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Spring Boot Actuator)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### CloudWatch (AWS)

```bash
# Install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i ./amazon-cloudwatch-agent.deb

# Configure
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

---

## Security Considerations

### Environment Variables

Never commit secrets. Use environment variables:

```bash
export SPRING_DATASOURCE_PASSWORD=secure_password
export SPRING_DATA_REDIS_PASSWORD=secure_redis_password
```

Or use AWS Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name matchmaking/db-password \
  --secret-string "secure_password"
```

### Firewall Rules

- Allow only necessary ports
- Restrict database access to application servers only
- Use security groups in AWS

### SSL/TLS

- Enable HTTPS in production
- Use valid SSL certificates
- Redirect HTTP to HTTPS

---

## Backup and Recovery

### PostgreSQL Backup

```bash
# Backup
docker exec matchmakingsystem-postgres-1 pg_dump -U postgres matchmaking > backup.sql

# Restore
docker exec -i matchmakingsystem-postgres-1 psql -U postgres matchmaking < backup.sql
```

### Automated Backups (AWS RDS)

Enable automated backups in RDS:
- Retention period: 7 days
- Backup window: Low-traffic hours

### Redis Backup

Redis is ephemeral. For persistence:

```yaml
# In redis.conf
save 900 1
save 300 10
save 60 10000
```

Or use AWS ElastiCache with automatic backups.

---

## Scaling

### Horizontal Scaling

Deploy multiple instances behind a load balancer:

```yaml
# docker-compose.yml
services:
  app:
    image: matchmaking-system:latest
    deploy:
      replicas: 3
```

### Database Scaling

- Read replicas for PostgreSQL
- Redis Cluster for distributed caching

### Load Balancer

Use AWS ALB or Nginx:

```nginx
upstream matchmaking {
    server app1:8080;
    server app2:8080;
    server app3:8080;
}

server {
    location / {
        proxy_pass http://matchmaking;
    }
}
```

---

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check PostgreSQL is running
   - Verify connection string
   - Check firewall rules

2. **Redis Connection Failed**
   - Check Redis is running
   - Verify host and port
   - Check Redis password

3. **Migration Failed**
   - Check Flyway schema history table
   - Verify migration file naming
   - Check for schema conflicts

4. **Out of Memory**
   - Increase JVM heap size: `-Xmx2g`
   - Check for memory leaks
   - Scale horizontally

### Logs

```bash
# Application logs
docker logs matchmakingsystem-app-1

# Database logs
docker logs matchmakingsystem-postgres-1

# Redis logs
docker logs matchmakingsystem-redis-1
```

---

## CI/CD Pipeline

### GitHub Actions Example

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Build with Maven
        run: ./mvnw clean package
      - name: Build Docker image
        run: docker build -t matchmaking-system:${{ github.sha }} .
      - name: Push to ECR
        run: |
          aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
          docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/matchmaking-system:${{ github.sha }}
      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster matchmaking-cluster --service matchmaking-service --force-new-deployment
```
