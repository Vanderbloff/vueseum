// src/lib/config.ts
const DEV_API_URL = 'http://localhost:3001';
const PROD_API_URL = 'https://vueseum.io';

export const API_BASE_URL = import.meta.env.DEV ? DEV_API_URL : PROD_API_URL;