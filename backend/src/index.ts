import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import rateLimit from 'express-rate-limit';
import { v4 as uuidv4 } from 'uuid';
import { pool } from './db';
import {
  authenticateJwt,
  AuthenticatedRequest,
  generateTokens,
  hashPassword,
  verifyPassword,
} from './auth';
import { generateContentStream } from './gemini';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Global rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// Health Check
app.get('/api/health', (req: Request, res: Response) => {
  res.json({
    status: 'HEALTHY',
    service: 'AEGIS AI Core Server',
    timestamp: new Date().toISOString(),
    securityBoundary: 'ISOLATED_ZERO_KEY_STORAGE',
  });
});

// --- AUTHENTICATION ROUTES ---

app.post('/api/auth/register', async (req: Request, res: Response) => {
  try {
    const { email, password, displayName } = req.body;
    if (!email || !password || password.length < 8) {
      return res.status(400).json({ error: 'Valid email and password (min 8 chars) required' });
    }

    const check = await pool.query('SELECT id FROM users WHERE email = $1', [email.toLowerCase()]);
    if (check.rows.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }

    const passwordHash = await hashPassword(password);
    const userId = uuidv4();
    await pool.query(
      'INSERT INTO users (id, email, password_hash, display_name) VALUES ($1, $2, $3, $4)',
      [userId, email.toLowerCase(), passwordHash, displayName || 'Security Officer']
    );

    const user = { id: userId, email: email.toLowerCase(), displayName: displayName || 'Security Officer', role: 'USER' };
    const tokens = generateTokens(user);

    res.status(201).json({ user, ...tokens });
  } catch (err: any) {
    console.error('[Register Error]', err);
    res.status(500).json({ error: 'Registration failed: ' + err.message });
  }
});

app.post('/api/auth/login', async (req: Request, res: Response) => {
  try {
    const { email, password } = req.body;
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const userRow = result.rows[0];
    const match = await verifyPassword(password, userRow.password_hash);
    if (!match) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const user = { id: userRow.id, email: userRow.email, displayName: userRow.display_name, role: userRow.role };
    const tokens = generateTokens(user);

    res.json({ user, ...tokens });
  } catch (err: any) {
    console.error('[Login Error]', err);
    res.status(500).json({ error: 'Login failed: ' + err.message });
  }
});

// --- CONVERSATION ROUTES ---

app.get('/api/conversations', authenticateJwt, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const result = await pool.query(
      'SELECT * FROM conversations WHERE user_id = $1 ORDER BY updated_at DESC',
      [req.user!.id]
    );
    res.json(result.rows);
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/conversations', authenticateJwt, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { title, mode, isTemporary, searchMode } = req.body;
    const id = uuidv4();
    const result = await pool.query(
      `INSERT INTO conversations (id, user_id, title, mode, is_temporary, search_mode)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [id, req.user!.id, title || 'New Conversation', mode || 'CHAT', !!isTemporary, searchMode || 'AUTO']
    );
    res.status(201).json(result.rows[0]);
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/conversations/:id', authenticateJwt, async (req: AuthenticatedRequest, res: Response) => {
  try {
    await pool.query('DELETE FROM conversations WHERE id = $1 AND user_id = $2', [req.params.id, req.user!.id]);
    res.json({ success: true });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// --- SSE STREAMING CHAT ROUTE ---

app.post('/api/chat/stream', authenticateJwt, async (req: AuthenticatedRequest, res: Response) => {
  const { conversationId, prompt, history, searchMode, model } = req.body;

  if (!prompt || typeof prompt !== 'string') {
    return res.status(400).json({ error: 'Prompt is required' });
  }

  // Set headers for Server-Sent Events (SSE)
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.flushHeaders();

  try {
    const result = await generateContentStream({
      prompt,
      history: history || [],
      searchMode: searchMode || 'AUTO',
      model: model || 'gemini-3.5-flash',
      onChunk: (chunk: string) => {
        res.write(`data: ${JSON.stringify({ type: 'chunk', content: chunk })}\n\n`);
      },
    });

    // Send completion event with citations and search metadata
    res.write(
      `data: ${JSON.stringify({
        type: 'done',
        text: result.text,
        citations: result.citations,
        searchQueries: result.searchQueries,
      })}\n\n`
    );
    res.end();
  } catch (err: any) {
    res.write(`data: ${JSON.stringify({ type: 'error', error: err.message })}\n\n`);
    res.end();
  }
});

app.listen(PORT, () => {
  console.log(`[AEGIS AI Core Server] running on port ${PORT}`);
  console.log(`[AEGIS Security] Zero-exposure key boundary active.`);
});
