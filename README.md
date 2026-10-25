# Configuration

## 1. Backend environment variables (Koyeb)
| Variable | Example | Purpose |
|---|---|---|
| `PORT` | `8000` | Server port |
| `MONGODB_URL` | `mongodb+srv://user:pass@cluster0.xxxx.mongodb.net/urlstream` | Stores links |
| `ADMIN_PASSWORD` | `my-strong-password` | Admin panel login |
| `JWT_SECRET` | long random string | Signs login tokens |

Do not use the default admin password (`admin123`) in production.

## 2. Deploy on Koyeb
1. Koyeb → **Create Service** → **GitHub** → pick your repo.
2. Builder: **Dockerfile**. Work directory: `backend` (Dockerfile path: `backend/Dockerfile`).
3. Add the environment variables above.
4. Port: `8000`. Deploy.
5. Your URL looks like `https://your-app-yourname.koyeb.app`.

## 3. MongoDB Atlas
1. Create a free cluster, then a database user.
2. Network Access: allow `0.0.0.0/0`, since Koyeb IPs change.
3. Copy the connection string into `MONGODB_URL`.

## 4. Connect the app
Edit `app/src/main/java/com/example/data/remote/ApiConstants.kt`:
```kotlin
const val DEFAULT_KOYEB_FQDN = "https://your-app-yourname.koyeb.app"
```

## 5. GitHub Actions secrets (optional, for a stable signed release APK)
Repo → Settings → Secrets and variables → Actions:
| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 my-upload-key.jks` |
| `STORE_PASSWORD` | keystore password |
| `KEY_PASSWORD` | key password (key alias must be `upload`) |

Create a keystore once with:
```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

## 6. Before publishing
- Change `applicationId` and `namespace` in `app/build.gradle.kts` from `com.example`.
- Never commit `.env`, `*.jks` or `debug.keystore`.
