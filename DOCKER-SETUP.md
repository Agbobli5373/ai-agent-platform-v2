# Docker Setup Guide

This guide explains how to set up and run the required services (PostgreSQL and Redis) using Docker Compose.

## Prerequisites

- Docker installed and running
- Docker Compose installed

## Services

The `docker-compose.yml` file defines the following services:

### PostgreSQL with pgvector
- **Image**: `ankane/pgvector:latest`
- **Container Name**: `ai-agent-postgres`
- **External Port**: `5433` (mapped to internal port 5432)
- **Database**: `ai_agent_platform`
- **Username**: `postgres`
- **Password**: `password`
- **Features**: Includes pgvector extension for vector embeddings

### Redis
- **Image**: `redis:7-alpine`
- **Container Name**: `ai-agent-redis`
- **External Port**: `6380` (mapped to internal port 6379)
- **Persistence**: Enabled with AOF (Append Only File)

## Port Configuration

The services use non-standard ports to avoid conflicts with existing installations:

| Service    | Standard Port | Docker Port | Reason                          |
|------------|---------------|-------------|---------------------------------|
| PostgreSQL | 5432          | 5433        | Avoid conflict with local PG    |
| Redis      | 6379          | 6380        | Avoid conflict with local Redis |

## Quick Start

### 1. Start All Services

```bash
cd ai-agent-platform
docker-compose up -d
```

This will:
- Download the required images (first time only)
- Create and start both PostgreSQL and Redis containers
- Create persistent volumes for data storage
- Set up a dedicated network for the services

### 2. Verify Services are Running

```bash
docker-compose ps
```

You should see both services with status "Up" and healthy.

### 3. Check Service Health

```bash
# Check PostgreSQL
docker exec -it ai-agent-postgres pg_isready -U postgres

# Check Redis
docker exec -it ai-agent-redis redis-cli ping
```

### 4. View Logs

```bash
# All services
docker-compose logs -f

# PostgreSQL only
docker-compose logs -f postgres

# Redis only
docker-compose logs -f redis
```

## Managing Services

### Stop Services

```bash
docker-compose stop
```

### Start Stopped Services

```bash
docker-compose start
```

### Restart Services

```bash
docker-compose restart
```

### Stop and Remove Containers

```bash
docker-compose down
```

### Stop and Remove Containers + Volumes (⚠️ Deletes all data)

```bash
docker-compose down -v
```

## Database Management

### Connect to PostgreSQL

```bash
# Using docker exec
docker exec -it ai-agent-postgres psql -U postgres -d ai_agent_platform

# Using psql from host (if installed)
psql -h localhost -p 5433 -U postgres -d ai_agent_platform
```

### Common PostgreSQL Commands

```sql
-- List databases
\l

-- Connect to database
\c ai_agent_platform

-- List tables
\dt

-- List extensions
\dx

-- Verify pgvector extension
SELECT * FROM pg_extension WHERE extname = 'vector';

-- Exit
\q
```

### Create Test Database

```bash
docker exec -it ai-agent-postgres psql -U postgres -c "CREATE DATABASE ai_agent_platform_test;"
```

## Redis Management

### Connect to Redis CLI

```bash
docker exec -it ai-agent-redis redis-cli
```

### Common Redis Commands

```bash
# Ping server
PING

# Get all keys
KEYS *

# Get server info
INFO

# Monitor commands in real-time
MONITOR

# Exit
EXIT
```

## Troubleshooting

### Port Already in Use

If you get a port conflict error:

1. Check what's using the port:
   ```bash
   # Windows
   netstat -ano | findstr :5433
   netstat -ano | findstr :6380
   
   # Linux/Mac
   lsof -i :5433
   lsof -i :6380
   ```

2. Either stop the conflicting service or change the port in `docker-compose.yml`

### Container Won't Start

```bash
# Check container logs
docker-compose logs postgres
docker-compose logs redis

# Remove and recreate containers
docker-compose down
docker-compose up -d
```

### Database Connection Issues

1. Verify PostgreSQL is running:
   ```bash
   docker-compose ps postgres
   ```

2. Check if database exists:
   ```bash
   docker exec -it ai-agent-postgres psql -U postgres -l
   ```

3. Verify application.properties has correct port (5433)

### Reset Everything

```bash
# Stop and remove everything
docker-compose down -v

# Remove images (optional)
docker rmi ankane/pgvector:latest redis:7-alpine

# Start fresh
docker-compose up -d
```

## Data Persistence

Data is persisted in Docker volumes:

- **postgres_data**: PostgreSQL database files
- **redis_data**: Redis persistence files

### Backup Database

```bash
# Backup
docker exec ai-agent-postgres pg_dump -U postgres ai_agent_platform > backup.sql

# Restore
docker exec -i ai-agent-postgres psql -U postgres ai_agent_platform < backup.sql
```

### View Volumes

```bash
docker volume ls
docker volume inspect ai-agent-platform_postgres_data
docker volume inspect ai-agent-platform_redis_data
```

## Running the Application

Once the services are running:

1. Ensure Docker services are up:
   ```bash
   docker-compose ps
   ```

2. Set your Mistral AI API key:
   ```bash
   # Windows (PowerShell)
   $env:MISTRAL_API_KEY="your-api-key-here"
   
   # Windows (CMD)
   set MISTRAL_API_KEY=your-api-key-here
   
   # Linux/Mac
   export MISTRAL_API_KEY=your-api-key-here
   ```

3. Start the Quarkus application:
   ```bash
   cd ai-agent-platform
   ./mvnw quarkus:dev
   ```

4. Access the application:
   - Web UI: http://localhost:8080
   - Dev UI: http://localhost:8080/q/dev

## Production Considerations

For production deployments:

1. **Change default passwords** in `docker-compose.yml`
2. **Use environment variables** for sensitive data
3. **Configure proper backup strategy**
4. **Set up monitoring and alerting**
5. **Use Docker secrets** for credentials
6. **Configure resource limits**
7. **Enable SSL/TLS** for database connections
8. **Use managed services** (AWS RDS, Azure Database, etc.) instead of Docker containers

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [Redis Documentation](https://redis.io/documentation)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
