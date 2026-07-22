import express from 'express';
import cors from 'cors';
import { authenticateToken } from './middlewares/auth';

// Import các controller phẳng
import { register, login, logout, me, updateMe } from './controllers/authController';
import { listCameras, getCamera, renameCamera, streamCameras, testDevice } from './controllers/cameraController';
import { listSpecies, listConfigs, getConfigDetail, saveConfig, resetConfig, applyPreset, listPresets, listAudioSamples } from './controllers/configController';
import { listSmsRecipients, addSmsRecipient, deleteSmsRecipient } from './controllers/smsController';
import { listEvents, listAlertFeed, readAlert, processDetection } from './controllers/eventController';
import { getSummary } from './controllers/statsController';
import { uploadSnapshot } from './controllers/snapshotController';
import multer from 'multer';

const app = express();
const upload = multer({ dest: 'tmp/' });

app.use(cors());
app.use(express.json());

// ==========================================
// 1. ENDPOINTS HỆ THỐNG & HEALTH CHECK
// ==========================================
app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'OK', timestamp: new Date().toISOString() });
});

// ==========================================
// 2. ENDPOINTS XÁC THỰC (AUTHENTICATION)
// ==========================================
app.post('/auth/register', register);
app.post('/auth/login', login);
app.post('/auth/logout', authenticateToken, logout);

// ==========================================
// 3. ENDPOINTS THÔNG TIN NGƯỜI DÙNG
// ==========================================
app.get('/users/me', authenticateToken, me);
app.patch('/users/me', authenticateToken, updateMe); // Chỉ dùng để test

// ==========================================
// 4. ENDPOINTS TRẠM CAMERA
// ==========================================
app.get('/cameras', authenticateToken, listCameras);
app.get('/cameras/stream', authenticateToken, streamCameras);
app.get('/cameras/:cameraId', authenticateToken, getCamera);
app.patch('/cameras/:cameraId', authenticateToken, renameCamera);
app.post('/cameras/:cameraId/devices/:deviceKey/test', authenticateToken, testDevice);
app.post('/cameras/:cameraId/snapshots', authenticateToken, upload.single('image'), uploadSnapshot);

// ==========================================
// 5. ENDPOINTS CẤU HÌNH PHÒNG VỆ & LOÀI
// ==========================================
app.get('/species', authenticateToken, listSpecies);
app.get('/response-configs', authenticateToken, getConfigDetail);
app.get('/response-configs/:cameraId', authenticateToken, listConfigs);
app.put('/response-configs/:cameraId/:speciesId', authenticateToken, saveConfig);
app.delete('/response-configs/:cameraId/:speciesId', authenticateToken, resetConfig);
app.post('/response-configs/:cameraId/:speciesId/apply-preset/:presetId', authenticateToken, applyPreset);
app.get('/control/presets', authenticateToken, listPresets);
app.get('/audio-samples', authenticateToken, listAudioSamples);

// ==========================================
// 6. ENDPOINTS QUẢN LÝ SĐT SMS PHỤ
// ==========================================
app.get('/users/me/sms-recipients', authenticateToken, listSmsRecipients);
app.post('/users/me/sms-recipients', authenticateToken, addSmsRecipient);
app.delete('/users/me/sms-recipients/:recipientId', authenticateToken, deleteSmsRecipient);

// ==========================================
// 7. ENDPOINTS NHẬT KÝ SỰ KIỆN & CẢNH BÁO
// ==========================================
app.get('/events', authenticateToken, listEvents);
app.get('/alerts/feed', authenticateToken, listAlertFeed);
app.post('/alerts/feed/:alertId/read', authenticateToken, readAlert);

// ==========================================
// 8. API TÍCH HỢP THIẾT BỊ / AI SERVER
// ==========================================
app.post('/cameras/:cameraId/detections', processDetection); // Webhook không cần JWT Token

// ==========================================
// 9. ENDPOINTS THỐNG KÊ
// ==========================================
app.get('/stats/summary', authenticateToken, getSummary);

export default app;
