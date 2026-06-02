# Mis-Using Developer

Enterprise-grade web application for managing company assets assigned to employees and interview scheduling/tracking. Built for internal use at large IT services organizations.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| Frontend | React 18, TypeScript, Vite, Material UI, React Query, React Hook Form, Axios, React Router |
| Backend | Spring Boot 3, Java 21, Spring Security, JWT, Spring Data JPA, PostgreSQL |
| DevOps | Docker, Docker Compose |
| API Docs | Swagger/OpenAPI |

## Features

- **Authentication**: JWT login, logout, token refresh, BCrypt password encryption
- **RBAC**: ADMIN, HR, EMPLOYEE roles with protected routes
- **Asset Management**: CRUD, search, filter, pagination, assign/return, history, CSV/Excel export
- **Interview Management**: Schedule, reschedule, cancel, complete, calendar views, feedback
- **Dashboard**: Real-time stats, charts, activity feed
- **User Management**: Admin CRUD, activate/deactivate, password reset
- **Notifications**: In-app + email notifications
- **Audit Logging**: Track all entity changes
- **Reports**: PDF-ready exports (CSV, Excel)
- **UI**: Responsive, dark/light mode, enterprise layout

## Project Structure

```
├── backend/                 # Spring Boot API
│   └── src/main/java/com/aims/
│       ├── config/          # Security, OpenAPI, Data init
│       ├── controller/      # REST controllers
│       ├── dto/             # Request/Response DTOs
│       ├── entity/          # JPA entities
│       ├── repository/      # Spring Data repositories
│       ├── security/        # JWT, filters
│       └── service/         # Business logic
├── frontend/                # React SPA
│   └── src/
│       ├── api/             # Axios client & services
│       ├── components/      # Reusable UI components
│       ├── context/         # Auth & Theme providers
│       └── pages/           # Application pages
├── database/
│   └── schema.sql           # PostgreSQL schema + seed data
└── docker-compose.yml
```

## Quick Start (Docker)

```bash
docker-compose up -d --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

## Local Development

### Prerequisites

- Java 21
- Node.js 20+
- Maven 3.9+
- PostgreSQL 16

### Database Setup

```bash
psql -U postgres -c "CREATE USER aims_user WITH PASSWORD 'aims_password';"
psql -U postgres -c "CREATE DATABASE aims_db OWNER aims_user;"
psql -U aims_user -d aims_db -f database/schema.sql
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at http://localhost:5173 with API proxy to http://localhost:8080.

## Install On Phone (PWA)

1. Connect phone and laptop to the same Wi-Fi.
2. Start backend and frontend on laptop.
3. Run frontend with network host:
   ```bash
   cd frontend
   npm run dev -- --host 0.0.0.0 --port 5173
   ```
4. Find laptop IPv4 (`ipconfig` on Windows), then open on phone:
   `http://<laptop-ip>:5173`
5. In phone browser menu, choose **Add to Home Screen** / **Install app**.

For full install behavior in production, build and serve `frontend/dist` over HTTPS.

## Native App Build (APK / iOS)

You can build installable app files directly (no Play Store required) using Capacitor.

### One-time setup

```bash
cd frontend
npm install
```

Set backend URL for mobile app:

```bash
copy .env.mobile.example .env
```

Then edit `.env`:

```bash
VITE_API_BASE_URL=https://your-backend-domain.com/api
```

### Android APK (share on WhatsApp)

```bash
cd frontend
npm run cap:android
```

In Android Studio:
1. Wait for Gradle sync.
2. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Use generated `.apk` file from Android Studio output and share it.

Phone install steps:
- Download APK from WhatsApp
- Allow "Install unknown apps" for WhatsApp/File Manager
- Install app

### iOS app

```bash
cd frontend
npx cap add ios
npm run cap:ios
```

Notes:
- iOS build requires macOS + Xcode (cannot build iOS app on Windows).
- You can prepare project on Windows, but final iOS build/signing must be done on a Mac.

## Free Hosting Setup (Render + Neon + Vercel)

Use this if you do not have your own domain.

### 1) Create PostgreSQL on Neon (free)

- Create a Neon project and database.
- Copy connection values:
  - host, database, username, password

### 2) Deploy backend on Render (free)

- Push this repo to GitHub.
- In Render, create new **Web Service** from the repo.
- Use Docker deploy from `backend/` (or use `backend/render.yaml` blueprint).
- Set environment variables (template: `backend/.env.render.example`):
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/<db-name>?sslmode=require`
  - `SPRING_DATASOURCE_USERNAME=<neon-user>`
  - `SPRING_DATASOURCE_PASSWORD=<neon-password>`
  - `JWT_SECRET=<long-random-secret>`
  - `APP_CORS_ALLOWED_ORIGINS=https://<your-vercel-app>.vercel.app,capacitor://localhost,ionic://localhost,http://localhost:5173,http://localhost:3000`
- After deploy, note backend URL:
  - `https://<your-render-backend>.onrender.com`

### 3) Deploy frontend on Vercel (free)

- Import `frontend/` as Vercel project.
- Build command: `npm run build`
- Output directory: `dist`
- Set environment variable (template: `frontend/.env.vercel.example`):
  - `VITE_API_BASE_URL=https://<your-render-backend>.onrender.com/api`
- Redeploy and note URL:
  - `https://<your-vercel-app>.vercel.app`

### 4) Update backend CORS with final Vercel URL

- In Render env vars, ensure `APP_CORS_ALLOWED_ORIGINS` contains your real Vercel URL.
- Redeploy backend.

### 5) Build APK connected to hosted backend

- In `frontend/.env`:
  - `VITE_API_BASE_URL=https://<your-render-backend>.onrender.com/api`
- Build and open Android:
  - `npm run cap:android`
- Generate APK in Android Studio and share on WhatsApp.

## Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@aims.com | Admin@123 |
| HR | hr@aims.com | Admin@123 |
| Employee | employee@aims.com | Admin@123 |

## API Endpoints

| Module | Base Path |
|--------|-----------|
| Auth | `/api/auth` |
| Users | `/api/users` |
| Assets | `/api/assets` |
| Interviews | `/api/interviews` |
| Dashboard | `/api/dashboard` |
| Notifications | `/api/notifications` |
| Audit | `/api/audit` |

## Environment Variables

| Variable | Default |
|----------|---------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/aims_db |
| SPRING_DATASOURCE_USERNAME | aims_user |
| SPRING_DATASOURCE_PASSWORD | aims_password |
| JWT_SECRET | (see application.yml) |
| MAIL_HOST | smtp.gmail.com |

## License

Proprietary - Internal Enterprise Use
