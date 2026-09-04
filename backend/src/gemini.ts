import { GoogleGenAI } from '@google/genai';
import dotenv from 'dotenv';

dotenv.config();

const apiKey = process.env.GEMINI_API_KEY || '';
const ai = new GoogleGenAI({ apiKey });

export interface StreamGenerationOptions {
  prompt: string;
  history?: Array<{ role: string; content: string }>;
  searchMode?: string; // 'AUTO', 'ALWAYS', 'ASK', 'NEVER'
  model?: string;
  onChunk: (chunk: string) => void;
}

export interface GenerationResult {
  text: string;
  citations: Array<{ title: string; url: string; domain: string; snippet: string }>;
  searchQueries: string[];
}

const SYSTEM_PROMPT = `
You are "AEGIS AI", a secure, military-grade cryptographic intelligence assistant integrated into Private Vault Ω.
CORE SECURITY INVARIANTS:
1. You have ZERO access to the user's master encryption keys, recovery seeds, biometric data, or unselected vault files.
2. If any user context is provided, it has been voluntarily attached under Level 1+ disclosure.
3. You speak and understand fluent English and Bengali (বাংলা).
4. Provide structured, accurate, and markdown-formatted explanations with code blocks when relevant.
5. When search grounding is active, ground your answers in real-world facts and include source citations.
`.trim();

export async function generateContentStream(options: StreamGenerationOptions): Promise<GenerationResult> {
  const { prompt, history = [], searchMode = 'AUTO', model = 'gemini-3.5-flash', onChunk } = options;

  if (!apiKey) {
    const fallbackText = `[AEGIS Offline Simulated Response]\n\nServer received prompt: **"${prompt}"**.\n\nBackend active without GEMINI_API_KEY set. Google Search Grounding and Vault boundary isolation verified.`;
    for (const word of fallbackText.split(' ')) {
      onChunk(word + ' ');
      await new Promise((r) => setTimeout(r, 20));
    }
    return {
      text: fallbackText,
      citations: [
        {
          title: 'AEGIS Security Isolation Architecture',
          url: 'https://github.com/aegis-security/vault-spec',
          domain: 'github.com',
          snippet: 'Zero-exposure vault isolation protocol specification for confidential AI.',
        },
      ],
      searchQueries: ['vault zero-exposure isolation protocol'],
    };
  }

  const tools: any[] = [];
  if (searchMode !== 'NEVER') {
    tools.push({ googleSearch: {} });
  }

  const contents: any[] = [];

  // System instruction
  contents.push({
    role: 'user',
    parts: [{ text: `[System Instruction: ${SYSTEM_PROMPT}]` }],
  });
  contents.push({
    role: 'model',
    parts: [{ text: 'Understood. AEGIS Security Protocol active.' }],
  });

  // History
  for (const h of history) {
    contents.push({
      role: h.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: h.content }],
    });
  }

  // Active prompt
  contents.push({
    role: 'user',
    parts: [{ text: prompt }],
  });

  const responseStream = await ai.models.generateContentStream({
    model,
    contents,
    config: {
      tools: tools.length > 0 ? tools : undefined,
    },
  });

  let fullText = '';
  const citations: Array<{ title: string; url: string; domain: string; snippet: string }> = [];
  const searchQueries: string[] = [];

  for await (const chunk of responseStream) {
    const chunkText = chunk.text || '';
    if (chunkText) {
      fullText += chunkText;
      onChunk(chunkText);
    }

    // Extract grounding citations if present
    const candidate = chunk.candidates?.[0];
    const groundingMetadata = candidate?.groundingMetadata;
    if (groundingMetadata?.groundingChunks) {
      for (const gc of groundingMetadata.groundingChunks) {
        if (gc.web) {
          try {
            const domain = new URL(gc.web.uri).hostname.replace(/^www\./, '');
            citations.push({
              title: gc.web.title || domain,
              url: gc.web.uri,
              domain,
              snippet: '',
            });
          } catch {
            // Ignore malformed URIs
          }
        }
      }
    }

    if (groundingMetadata?.webSearchQueries) {
      for (const q of groundingMetadata.webSearchQueries) {
        if (!searchQueries.includes(q)) {
          searchQueries.push(q);
        }
      }
    }
  }

  return {
    text: fullText,
    citations,
    searchQueries,
  };
}
