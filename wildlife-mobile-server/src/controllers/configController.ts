import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';
import { PRESET_SCENARIOS, getRecommendedPresetForSpecies } from '../config/presets';
import { loadAlertSounds } from '../config/alertSounds';

const prisma = new PrismaClient();

// 1. GET /species - Danh mục các loài động vật
export async function listSpecies(_req: AuthenticatedRequest, res: Response) {
  try {
    const species = await prisma.species.findMany({
      orderBy: { id: 'asc' }
    });
    return res.status(200).json(species);
  } catch (error) {
    console.error('Lỗi khi tải danh sách loài:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 2. GET /response-configs - Danh sách cấu hình của người dùng hiện tại
export async function listConfigs(req: AuthenticatedRequest, res: Response) {
  const userId = req.user!.id;

  try {
    const configs = await prisma.responseConfig.findMany({
      where: { userId }
    });

    const mapped = configs.map((c) => ({
      id: c.id,
      userId: c.userId,
      speciesId: c.speciesId,
      ledFlash: c.ledFlashRate ? true : false,
      ledColor: c.ledColor,
      ledIntensity: c.ledDurationSeconds,
      ledFlashRate: c.ledFlashRate,
      speakerWarn: Boolean(c.audioSampleId || (c.audioIntensity ?? 0) > 0 || c.speakerSampleId),
      audioSampleId: c.audioSampleId,
      audioIntensity: c.audioIntensity,
      silentAlert: c.silentAlert,
      updatedAt: c.updatedAt.toISOString()
    }));

    return res.status(200).json(mapped);
  } catch (error) {
    console.error('Lỗi khi tải cấu hình người dùng:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 3. GET /response-configs - Lấy cấu hình chi tiết (Cấu hình tùy chỉnh hoặc Cấu hình mặc định Preset)
export async function getConfigDetail(req: AuthenticatedRequest, res: Response) {
  const userId = req.user!.id;
  const { speciesId } = req.query;

  if (!speciesId) {
    return res.status(400).json({ error: 'missed_species_id', message: 'Thiếu tham số bắt buộc: speciesId.' });
  }

  try {
    const species = await prisma.species.findUnique({
      where: { id: speciesId as string }
    });
    if (!species) {
      return res.status(404).json({ error: 'not_found_species', message: 'Không tìm thấy loài yêu cầu.' });
    }

    // Tìm cấu hình tùy chọn
    const customConfig = await prisma.responseConfig.findUnique({
      where: {
        userId_speciesId: {
          userId,
          speciesId: speciesId as string
        }
      }
    });

    if (customConfig) {
      return res.status(200).json({
        id: customConfig.id,
        userId: customConfig.userId,
        speciesId: customConfig.speciesId,
        ledFlash: customConfig.ledFlashRate ? true : false,
        ledColor: customConfig.ledColor,
        ledIntensity: customConfig.ledDurationSeconds, // Ánh xạ trường
        ledFlashRate: customConfig.ledFlashRate,
        speakerWarn: Boolean(customConfig.audioSampleId || (customConfig.audioIntensity ?? 0) > 0 || customConfig.speakerSampleId),
        audioSampleId: customConfig.audioSampleId,
        audioIntensity: customConfig.audioIntensity,
        silentAlert: customConfig.silentAlert
      });
    }

    // Nếu không có tùy chọn, lấy cấu hình mặc định (Danger Preset)
    const fallback = getRecommendedPresetForSpecies(species.id, species.dangerLevel);
    return res.status(200).json({
      userId,
      speciesId,
      ...fallback
    });
  } catch (error) {
    console.error('Lỗi khi lấy chi tiết cấu hình:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 4. PUT /response-configs/{speciesId} - Lưu cấu hình phòng vệ tùy chọn
export async function saveConfig(req: AuthenticatedRequest, res: Response) {
  const userId = req.user!.id;
  const { speciesId } = req.params;
  const {
    ledFlash,
    ledColor,
    ledIntensity,
    ledFlashRate,
    speakerWarn,
    audioSampleId,
    audioIntensity,
    silentAlert
  } = req.body;

  // Validation: Thiếu trường required
  if (ledFlash === undefined) {
    return res.status(400).json({ error: 'missed_led_flash', message: 'Thiếu thông tin bắt buộc: ledFlash.' });
  }
  if (speakerWarn === undefined) {
    return res.status(400).json({ error: 'missed_speaker_warn', message: 'Thiếu thông tin bắt buộc: speakerWarn.' });
  }
  if (silentAlert === undefined) {
    return res.status(400).json({ error: 'missed_silent_alert', message: 'Thiếu thông tin bắt buộc: silentAlert.' });
  }

  // Validation: Sai cấu trúc Led Color Enum (RED, YELLOW, WHITE, STROBE)
  const validColors = ['RED', 'YELLOW', 'WHITE', 'STROBE'];
  if (ledColor && !validColors.includes(ledColor)) {
    return res.status(400).json({ error: 'invalid_led_color', message: `Màu LED không hợp lệ. Phải thuộc: ${validColors.join(', ')}.` });
  }

  // Validation: Sai cấu trúc Led Flash Rate Enum (2_per_sec, 4_per_sec, random)
  const validRates = ['2_per_sec', '4_per_sec', 'random'];
  if (ledFlashRate && !validRates.includes(ledFlashRate)) {
    return res.status(400).json({ error: 'invalid_led_flash_rate', message: `Tần suất LED không hợp lệ. Phải thuộc: ${validRates.join(', ')}.` });
  }

  // Validation: Sai khoảng giá trị (intensity: 0-100, duration >= 0)
  if (ledIntensity !== undefined && (ledIntensity < 0 || ledIntensity > 100)) {
    return res.status(400).json({ error: 'invalid_led_intensity', message: 'Cường độ LED (ledIntensity) phải nằm trong khoảng từ 0 đến 100.' });
  }
  if (audioIntensity !== undefined && (audioIntensity < 0 || audioIntensity > 100)) {
    return res.status(400).json({ error: 'invalid_audio_intensity', message: 'Cường độ âm lượng loa (audioIntensity) phải nằm trong khoảng từ 0 đến 100.' });
  }

  try {
    const species = await prisma.species.findUnique({ where: { id: speciesId } });
    if (!species) {
      return res.status(404).json({ error: 'not_found_species', message: 'Không tìm thấy loài động vật.' });
    }

    // Upsert cấu hình tùy chỉnh vào database
    const configData = {
      audioSampleId: audioSampleId || null,
      audioIntensity: audioIntensity || 0,
      ledFlashRate: ledFlashRate !== undefined ? ledFlashRate : (ledFlash ? 'FAST' : null),
      ledColor: ledColor || null,
      ledDurationSeconds: ledIntensity || 0, // Ánh xạ trường
      speakerSampleId: null,
      silentAlert,
      lastModifiedBy: userId
    };

    const savedConfig = await prisma.responseConfig.upsert({
      where: {
        userId_speciesId: { userId, speciesId }
      },
      create: {
        userId,
        speciesId,
        ...configData
      },
      update: configData
    });

    return res.status(200).json({
      userId: savedConfig.userId,
      speciesId: savedConfig.speciesId,
      ledFlash,
      ledColor: savedConfig.ledColor,
      ledIntensity: savedConfig.ledDurationSeconds,
      ledFlashRate: savedConfig.ledFlashRate,
      speakerWarn,
      audioSampleId: savedConfig.audioSampleId,
      audioIntensity: savedConfig.audioIntensity,
      silentAlert: savedConfig.silentAlert
    });
  } catch (error) {
    console.error('Lỗi khi lưu cấu hình:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 5. DELETE /response-configs/{speciesId} - Xóa cấu hình tùy chỉnh để quay về mặc định
export async function resetConfig(req: AuthenticatedRequest, res: Response) {
  const userId = req.user!.id;
  const { speciesId } = req.params;

  try {
    const existingConfig = await prisma.responseConfig.findUnique({
      where: {
        userId_speciesId: { userId, speciesId }
      }
    });

    if (!existingConfig) {
      return res.status(404).json({ error: 'not_found_config', message: 'Không tìm thấy cấu hình tùy chỉnh để xóa.' });
    }

    await prisma.responseConfig.delete({
      where: {
        userId_speciesId: { userId, speciesId }
      }
    });

    return res.status(204).send();
  } catch (error) {
    console.error('Lỗi khi đặt lại cấu hình mặc định:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 6. POST /response-configs/{speciesId}/apply-preset/{presetId} - Áp nhanh preset
export async function applyPreset(req: AuthenticatedRequest, res: Response) {
  const userId = req.user!.id;
  const { speciesId, presetId } = req.params;

  const preset = PRESET_SCENARIOS[presetId];
  if (!preset) {
    return res.status(404).json({ error: 'not_found_preset', message: 'Không tìm thấy preset mẫu yêu cầu.' });
  }

  try {
    const species = await prisma.species.findUnique({ where: { id: speciesId } });
    if (!species) {
      return res.status(404).json({ error: 'not_found_species', message: 'Không tìm thấy loài động vật.' });
    }

    // Ghi cấu hình áp preset
    const configData = {
      audioSampleId: (preset.audioSampleId as string | null) || null,
      audioIntensity: typeof preset.audioIntensity === 'number' ? preset.audioIntensity : null,
      ledFlashRate: preset.ledFlash ? 'FAST' : null,
      ledColor: (preset.ledColor as string | null) || null,
      ledDurationSeconds: typeof preset.ledIntensity === 'number' ? preset.ledIntensity : null,
      silentAlert: typeof preset.silentAlert === 'boolean' ? preset.silentAlert : false,
      lastModifiedBy: userId
    };

    const updatedConfig = await prisma.responseConfig.upsert({
      where: {
        userId_speciesId: { userId, speciesId }
      },
      create: {
        userId,
        speciesId,
        ...configData
      },
      update: configData
    });

    return res.status(200).json({
      userId: updatedConfig.userId,
      speciesId: updatedConfig.speciesId,
      ...preset
    });
  } catch (error) {
    console.error('Lỗi khi áp preset:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}

// 7. GET /control/presets - Tải danh sách Preset kịch bản phòng vệ
export async function listPresets(_req: AuthenticatedRequest, res: Response) {
  const list = Object.entries(PRESET_SCENARIOS).map(([id, val]) => ({
    id,
    name: id === 'critical_danger' ? 'Xua đuổi khẩn cấp' : id === 'medium_danger' ? 'Xua đuổi tầm trung' : 'Cảnh báo đột nhập',
    config: val
  }));
  return res.status(200).json(list);
}

// 8. GET /audio-samples - Tải danh mục âm thanh mẫu (công khai, không cần xác thực)
export async function listAudioSamples(req: Request, res: Response) {
  const protocol = req.protocol;
  const host = req.get('host');
  const baseUrl = `${protocol}://${host}`;

  const animalDeterrentSounds = [
    { id: 'A_gunshot', name: 'Tiếng súng nổ đe dọa', file: `${baseUrl}/assets/alert/A_gunshot.mp3` },
    { id: 'A_growl', name: 'Tiếng gầm đe dọa', file: `${baseUrl}/assets/alert/A_growl.mp3` },
    { id: 'A_dog_bark', name: 'Tiếng chó sủa dữ dội', file: `${baseUrl}/assets/alert/A_dog_bark.mp3` },
    { id: 'A_explosion', name: 'Tiếng nổ giả lập', file: `${baseUrl}/assets/alert/A_explosion.mp3` },
    { id: 'A_human', name: 'Tiếng người xua đuổi', file: `${baseUrl}/assets/alert/A_human.mp3` },
    { id: 'A_fish', name: 'Tiếng sóng dưới nước (Đuổi cá)', file: `${baseUrl}/assets/alert/A_fish.mp3` }
  ];

  const citizenAlertSounds = loadAlertSounds().map(sound => {
    const filename = sound.file.substring(sound.file.lastIndexOf('/') + 1);
    return {
      ...sound,
      file: `${baseUrl}/assets/alert/${filename}`
    };
  });

  return res.status(200).json({
    animalDeterrentSounds,
    citizenAlertSounds
  });
}

// 9. GET /alertSounds - Tải danh sách file âm thanh cảnh báo (công khai, không cần xác thực)
export async function listAlertSounds(req: Request, res: Response) {
  try {
    const protocol = req.protocol;
    const host = req.get('host');
    const baseUrl = `${protocol}://${host}`;
    
    const sounds = loadAlertSounds().map(sound => {
      const filename = sound.file.substring(sound.file.lastIndexOf('/') + 1);
      return {
        ...sound,
        file: `${baseUrl}/assets/alert/${filename}`
      };
    });
    return res.status(200).json(sounds);
  } catch (error) {
    console.error('[AlertSound] Lỗi khi load file alert-sound.yaml:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}
