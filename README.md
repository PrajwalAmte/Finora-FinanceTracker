# Finance Tracker

A full-stack personal finance management application with real-time market data integration, automated portfolio tracking, and comprehensive financial analytics.

## Screenshots

### Dashboard - Light Mode
![Dashboard Light Mode](docs/Dashboard-Light.png)

### Dashboard - Dark Mode
![Dashboard Dark Mode](docs/Dasboard-Dark.png)

The dashboard provides a comprehensive overview of your financial portfolio with:
- Net Worth Tracking: Real-time portfolio valuation
- Investment Performance: Overall return percentage with automated price updates
- Loan Management: Outstanding loan balance tracking
- Expense Monitoring: Monthly spending summary
- Asset Allocation Chart: Visual breakdown of investment distribution
- Expense Analytics: Categorized spending visualization (Utilities, Groceries, Shopping)
- Quick Navigation: Direct links to detailed views for investments and expenses

## Quick Start with Docker (Recommended)

The easiest way to run the entire application is using Docker Compose. All services (database, backend, frontend) will be started automatically.

### Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)

### Starting the Application

**Linux/macOS:**
```bash
./run.sh
```

**Windows:**
```cmd
run.bat
```

**Manual Start:**
```bash
docker compose up --build
```

### Access Points

Once started, access the application at:

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8082/api
- **API Documentation (Swagger)**: http://localhost:8082/swagger-ui.html
- **Health Check**: http://localhost:8082/actuator/health
- **JWT Token Generation**: http://localhost:8082/auth/token
- **Database**: localhost:5433 (for external tools)

### What Gets Started

1. **PostgreSQL Database** (port 5433→5432)
   - Database: `finance_tracker`
   - User: `postgres`
   - Password: `postgres`
   - Data persisted in Docker volume
   - External port mapped to 5433 to avoid conflicts

2. **Spring Boot Backend** (port 8082)
   - REST API endpoints with JWT authentication
   - Auto-token generation endpoint at `/auth/token`
   - Automated schedulers for price updates
   - Swagger UI for API documentation
   - CORS configured for frontend access
   - Environment variables loaded from `.env` file

3. **React Frontend** (port 80)
   - Served via NGINX
   - Modern UI with real-time data
   - Connects to backend at `http://localhost:8082/api`

### Stopping Services

Press `Ctrl+C` in the terminal, or run:
```bash
docker compose down
```

To remove volumes (WARNING: deletes all data):
```bash
docker compose down -v
```

## System Architecture

```mermaid
graph TB
    %% Frontend Layer
    UI[React Frontend<br/>TypeScript + Tailwind CSS]
    
    %% API Gateway
    API[Spring Boot API<br/>REST Endpoints + JWT Auth]
    
    %% Service Layer
    subgraph Services["Service Layer"]
        IS[Investment Service]
        SS[SIP Service]  
        ES[Expense Service]
        LS[Loan Service]
        PS[Price Service]
    end
    
    %% Data Layer
    DB[(PostgreSQL<br/>Database)]
    
    %% Scheduler Layer
    subgraph Schedulers["Automated Schedulers"]
        PU[Price Updates<br/>4x Daily]
        NU[NAV Updates<br/>4x Daily]
        SI[SIP Processing<br/>Monthly]
        LU[Loan Updates<br/>Daily]
    end
    
    %% External APIs
    subgraph External["External APIs"]
        YF[Yahoo Finance<br/>Stock Prices]
        AM[AMFI<br/>Mutual Fund NAV]
        TD[Twelve Data<br/>Backup API]
    end
    
    %% Connections
    UI -->|HTTP/REST + JWT| API
    API --> Services
    Services --> DB
    
    %% Scheduler connections
    PU --> PS
    NU --> SS
    SI --> SS
    LU --> LS
    
    %% External API connections
    PS -->|Primary| YF
    PS -->|Fallback| TD
    SS --> AM
    
    %% Styling
    classDef frontend fill:#e1f5fe,color:#000000
    classDef backend fill:#f3e5f5,color:#000000
    classDef database fill:#e8f5e8,color:#000000
    classDef scheduler fill:#fff3e0,color:#000000
    classDef external fill:#fce4ec,color:#000000
    
    class UI frontend
    class API,Services backend
    class DB database
    class Schedulers,PU,NU,SI,LU scheduler
    class External,YF,AM,TD external
```

## Key Features

- Real-time Portfolio Tracking: Automated price updates 4 times daily
- JWT Authentication: Secure API access with auto-token generation
- SIP Management: Monthly automated investment processing with NAV tracking
- Expense Analytics: Categorized spending analysis with visualizations
- Loan Calculator: EMI tracking with automatic balance updates
- Data Export: PDF/Excel report generation
- API Failover: Dual API integration for high reliability
- Smart Scheduling: Configurable cron jobs for different market timings
- Environment-based Configuration: Secure credential management via `.env` file

## Tech Stack

**Backend**: 
- Spring Boot 3.4.5
- Java 21
- PostgreSQL
- Spring Data JPA
- Spring Security with JWT
- Maven

**Frontend**: 
- React 18
- TypeScript
- Vite
- Tailwind CSS
- Recharts

**External APIs**: 
- Yahoo Finance (stocks)
- AMFI (mutual funds)
- Twelve Data (backup)

## Local Development Setup

If you prefer to run the application without Docker:

### Prerequisites

- Java 21+
- Node.js (LTS version)
- Maven 3.6+
- PostgreSQL

### Backend Setup

```bash
git clone <repository-url>
cd Finance_Tracker-API

# Create .env file with required variables
echo "JWT_SECRET=your_jwt_secret_key_here" > .env
echo "TWELVEDATA_API_KEY=your_api_key_here" >> .env  # Optional

# Configure application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_tracker
spring.datasource.username=your_username
spring.datasource.password=your_password

# Run the application
./mvnw spring-boot:run  # Starts on port 8082
```

### Frontend Setup

```bash
cd Finance_Tracker-UI

# Create .env file
echo "VITE_API_BASE_URL=http://localhost:8082/api" > .env

npm install
npm run dev  # Starts on port 5173
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/token` | GET | Auto-generate JWT token |
| `/api/investments` | GET/POST | Portfolio management |
| `/api/investments/summary` | GET | Portfolio overview |
| `/api/sips` | GET/POST | SIP tracking |
| `/api/expenses` | GET/POST | Expense management |
| `/api/loans` | GET/POST | Loan calculations |

**Full API Documentation**: Interactive API documentation is available via Swagger UI:
- http://localhost:8082/swagger-ui.html

**Authentication**: All API endpoints (except `/auth/token`) require JWT token in Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

## Automated Schedulers

The application includes automated background tasks that run as long as the backend container is running:

```java
@Scheduled(cron = "0 0 9,12,15,18 * * *")  // Price Updates (9 AM, 12 PM, 3 PM, 6 PM)
@Scheduled(cron = "0 0 9 1 * *")           // Monthly SIP Processing (1st of month, 9 AM)
@Scheduled(cron = "0 0 0 * * *")           // Daily Loan Updates (Midnight)
```

These schedulers automatically execute their tasks in the background without manual intervention.

## External API Integration

**Yahoo Finance**: 
- Real-time stock prices with automatic symbol formatting
- Example: RELIANCE → RELIANCE.NS (NSE) or RELIANCE.BO (BSE)
- 10-second rate limiting between requests

**AMFI**: 
- Official mutual fund NAV data
- Daily CSV parsing for accurate pricing
- Caching for improved performance

**Twelve Data**: 
- Backup API with NSE/BSE support
- 8-second rate limiting
- Exponential backoff for failures

## Docker Configuration

### Environment Variables

The application uses `.env` files for sensitive configuration, loaded via `env_file` in `docker-compose.yml`:

**Backend `.env`:**
```env
JWT_SECRET=your_jwt_secret_key_here
TWELVEDATA_API_KEY=your_api_key_here
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/finance_tracker
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
SERVER_PORT=8082
```

**Frontend `.env`:**
```env
VITE_API_BASE_URL=http://localhost:8082/api
```

### Port Mappings

- **Frontend**: 80 (host) → 80 (container)
- **Backend**: 8082 (host) → 8082 (container)
- **PostgreSQL**: 5433 (host) → 5432 (container)

### Data Persistence

PostgreSQL data is stored in a Docker volume named `postgres_data`. This persists even after containers are stopped.

**Backup Database:**
```bash
docker compose exec postgres pg_dump -U postgres finance_tracker > backup.sql
```

**Restore Database:**
```bash
docker compose exec -T postgres psql -U postgres finance_tracker < backup.sql
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

### Rebuild After Code Changes

```bash
docker compose up --build
```

## Troubleshooting

### Port Already in Use

If ports 80, 8082, or 5433 are already in use, modify the port mappings in `docker-compose.yml`:

```yaml
services:
  frontend:
    ports:
      - "3000:80"  # Change 80 to your preferred port
  backend:
    ports:
      - "8081:8082"  # Change 8082 to your preferred port
  postgres:
    ports:
      - "5434:5432"  # Change 5433 to your preferred port
```

### Database Connection Issues

Ensure PostgreSQL container is healthy before backend starts. Health checks are configured automatically in `docker-compose.yml`. The backend will wait for the database to be ready before starting.

### Frontend Can't Connect to Backend

1. Verify backend is running: http://localhost:8082/actuator/health
2. Check browser console for CORS errors (CORS is configured in SecurityConfig)
3. Verify `VITE_API_BASE_URL=http://localhost:8082/api` in frontend `.env` file
4. Ensure JWT token is present in request headers

### JWT Authentication Issues

1. Generate a token: http://localhost:8082/auth/token
2. Copy the token and include it in API requests
3. Check token expiration settings in backend configuration
4. Verify `JWT_SECRET` is set in backend `.env` file

### Application Not Starting

```bash
# Check container status
docker compose ps

# View detailed logs
docker compose logs

# Restart services
docker compose restart

# Clean restart
docker compose down && docker compose up --build
```

## Production Build

### Using Docker (Recommended)

```bash
docker compose up --build -d
```

**Important for Production:**
1. Change default database credentials in `.env`
2. Generate a strong JWT secret (minimum 32 characters)
3. Update CORS allowed origins in SecurityConfig
4. Use proper SSL/TLS certificates for HTTPS
5. Configure firewall rules for ports 80, 8082, 5433

### Manual Build

**Backend:**
```bash
cd Finance_Tracker-API
./mvnw clean package
java -jar target/finance-tracker-0.0.1-SNAPSHOT.jar
```

**Frontend:**
```bash
cd Finance_Tracker-UI
npm run build
# Serve dist/ folder with a web server (nginx, apache, etc.)
```

## Configuration

**Development**: H2 in-memory database  
**Production**: PostgreSQL with connection pooling  
**Authentication**: JWT-based with auto-token generation  
**CORS**: Configured for frontend access (port 80)  
**Scheduling**: Configurable cron expressions for different market timings  
**Rate Limiting**: 10s delays for Yahoo Finance, 8s for Twelve Data  
**Environment Variables**: Managed via `.env` files loaded by Docker Compose

## Performance Features

- API response caching for NAV data
- Batch processing for portfolio updates
- Database indexing for optimized queries
- Connection pooling for efficient database access
- Exponential backoff for API failures
- Automatic retry mechanisms for failed requests
- JWT token-based stateless authentication

## Database Schema

The application uses PostgreSQL with the following main entities:

- **Investments**: Stock and mutual fund holdings
- **SIPs**: Systematic Investment Plans
- **Expenses**: Categorized spending records
- **Loans**: EMI and loan tracking
- **Price History**: Historical price data for analytics

## Security Considerations

- JWT-based authentication for secure API access
- Environment variables for sensitive configuration via `.env` files
- CORS configuration for controlled API access
- Database credentials stored in `.env` (change for production)
- API rate limiting to prevent abuse
- Auto-token generation endpoint for development convenience
- Spring Security integration with custom SecurityConfig

## License

MIT License - Built for better financial management and investment tracking

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or feature requests, please open an issue on the repository.