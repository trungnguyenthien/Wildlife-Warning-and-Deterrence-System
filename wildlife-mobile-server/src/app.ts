import express from 'express';
import cors from 'cors';

const app = express();

app.use(cors());
app.use(express.json());

// Endpoint kiểm tra sức khỏe hệ thống (Health Check)
app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'OK', timestamp: new Date().toISOString() });
});

export default app;
