# Remaining Deployment Steps (Manual)

These are the only remaining tasks that need your account login in Neon/Render/Vercel.

## 1) Neon (Database)

1. Sign in to Neon and create a project.
2. Create/select database.
3. Copy:
   - host
   - database name
   - username
   - password

JDBC format you will use in Render:

`jdbc:postgresql://<neon-host>/<db-name>?sslmode=require`

## 2) Render (Backend)

1. Sign in to Render.
2. New -> Web Service -> Connect your GitHub repo.
3. Configure:
   - Root Directory: `backend`
   - Runtime: Docker
   - Branch: `main`
4. Add environment variables (copy from `backend/.env.render.example`):
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET`
   - `JWT_EXPIRATION=3600000`
   - `JWT_REFRESH_EXPIRATION=86400000`
   - `APP_CORS_ALLOWED_ORIGINS` (include Vercel URL + capacitor origins)
5. Deploy and copy backend URL:
   - `https://<render-service>.onrender.com`

## 3) Vercel (Frontend)

1. Sign in to Vercel.
2. Import same GitHub repo.
3. Set project root directory to `frontend`.
4. Add env var from `frontend/.env.vercel.example`:
   - `VITE_API_BASE_URL=https://<render-service>.onrender.com/api`
5. Deploy and copy Vercel URL:
   - `https://<project>.vercel.app`

## 4) Final CORS Update

In Render backend environment, set:

`APP_CORS_ALLOWED_ORIGINS=https://<project>.vercel.app,capacitor://localhost,ionic://localhost,http://localhost:5173,http://localhost:3000`

Redeploy backend once.

## 5) Verify Web App

1. Open Vercel URL.
2. Login with:
   - `admin@aims.com`
   - `Admin@123`
3. Confirm Dashboard, Assets, Interviews load.

## 6) Build APK (Android Studio)

1. In `frontend/.env` set:
   - `VITE_API_BASE_URL=https://<render-service>.onrender.com/api`
2. Run:
   - `cd frontend`
   - `npm run cap:android`
3. In Android Studio:
   - Build -> Build Bundle(s) / APK(s) -> Build APK(s)
4. Share generated APK on WhatsApp and install on phone.

---

If any step fails, send screenshot/error text and continue from that exact step.
