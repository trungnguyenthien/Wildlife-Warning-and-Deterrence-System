import app from './app';
import dotenv from 'dotenv';
import http from 'http';
import { setupWebSocket } from './websocket';

dotenv.config({ path: '.env.local' });
dotenv.config();

const PORT = process.env.PORT || 3000;

const server = http.createServer(app);
setupWebSocket(server);

server.listen(PORT, () => {
  console.log(`Server is running locally at http://localhost:${PORT}`);
});
