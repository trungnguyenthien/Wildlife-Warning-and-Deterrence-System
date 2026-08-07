import fs from 'fs';
import path from 'path';
import yaml from 'js-yaml';

export interface AlertSound {
  id: string;
  name: string;
  file: string;
}

const YAML_PATH = path.join(__dirname, '../../hard-config/alert-sound.yaml');

// Cache tĩnh: chỉ đọc file một lần, tránh đọc lại liên tục mỗi request
let cachedSounds: AlertSound[] | null = null;

/**
 * Nạp danh sách âm thanh cảnh báo từ hard-config/alert-sound.yaml.
 * Nguồn duy nhất cho cả GET /alertSounds và citizenAlertSounds trong GET /audio-samples.
 */
export function loadAlertSounds(): AlertSound[] {
  if (cachedSounds) {
    return cachedSounds;
  }

  if (!fs.existsSync(YAML_PATH)) {
    console.warn('[AlertSound] Không tìm thấy file hard-config/alert-sound.yaml.');
    return [];
  }

  try {
    const fileContent = fs.readFileSync(YAML_PATH, 'utf8');
    const parsed = yaml.load(fileContent) as AlertSound[] | null;
    cachedSounds = Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error('[AlertSound] Lỗi khi load file alert-sound.yaml:', error);
    return [];
  }

  return cachedSounds;
}
