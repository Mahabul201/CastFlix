import os

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass


class Config:
    # Server
    PORT = int(os.environ.get("PORT", 8000))
    NODE_ENV = os.environ.get("NODE_ENV", "production")

    # Database (MongoDB Atlas / local)
    MONGODB_URL = os.environ.get("MONGODB_URL", "mongodb://localhost:27017/urlstream")

    # Admin panel & auth
    ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "admin123")
    JWT_SECRET = os.environ.get("JWT_SECRET", "url_stream_super_jwt_secret_2026")


config = Config()
