# AEGIS AI Core — Backend Service

Military-grade AI intelligence backend for Private Vault Ω, built with Node.js, TypeScript, PostgreSQL, and Google Gemini 3.5 API with Google Search Grounding.

## Architecture & Security Boundary
- **Zero-Exposure Invariant:** The vault master PIN/password, hardware keystore KEK, and recovery secrets are NEVER transmitted to this backend.
- **Level 0-4 Permission Model:** Only user-consented, sanitized content is processed.
- **Real-Time Web Grounding:** Grounded directly through Gemini's Google Search tool with verified web citation cards.
- **Multi-Lingual:** Native support for English and Bengali (বাংলা).

## Prerequisites
- Node.js 20+
- PostgreSQL 14+
- `GEMINI_API_KEY` (Gemini Developer API Key)

## Setup & Running

```bash
cd backend
npm install

# Setup database
psql -U postgres -d postgres -f schema.sql

# Configure environment
cp .env.example .env
# Edit .env and set GEMINI_API_KEY, DATABASE_URL, and JWT_SECRET

# Run in development mode
npm run dev

# Build and start in production
npm run build
npm start
```

## Environment Variables (.env)
```env
PORT=4000
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/aegis_ai_db
GEMINI_API_KEY=your_gemini_api_key_here
JWT_SECRET=your_jwt_secret_here
JWT_REFRESH_SECRET=your_refresh_secret_here
```
