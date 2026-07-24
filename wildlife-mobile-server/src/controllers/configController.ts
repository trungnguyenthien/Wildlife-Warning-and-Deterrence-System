import { Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';
import { PRESET_SCENARIOS, getRecommendedPresetForSpecies } from '../config/presets';

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

// 2. GET /response-configs/{cameraId} - Danh sách cấu hình tại camera
export async function listConfigs(req: AuthenticatedRequest, res: Response) {
  const { cameraId } = req.params;

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId }
    });

    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera yêu cầu.' });
    }

    const configs = await prisma.responseConfig.findMany({
      where: { cameraId }
    });

    return res.status(200).json(configs);
  } catch (error) {
    console.error('Lỗi khi tải cấu hình camera:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 3. GET /response-configs - Lấy cấu hình chi tiết (Cấu hình tùy chỉnh hoặc Cấu hình mặc định Preset)
export async function getConfigDetail(req: AuthenticatedRequest, res: Response) {
  const { cameraId, speciesId } = req.query;

  if (!cameraId) {
    return res.status(400).json({ error: 'missed_camera_id', message: 'Thiếu tham số bắt buộc: cameraId.' });
  }
  if (!speciesId) {
    return res.status(400).json({ error: 'missed_species_id', message: 'Thiếu tham số bắt buộc: speciesId.' });
  }

  try {
    const camera = await prisma.camera.findUnique({
      where: { id: cameraId as string }
    });
    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera yêu cầu.' });
    }

    const species = await prisma.species.findUnique({
      where: { id: speciesId as string }
    });
    if (!species) {
      return res.status(404).json({ error: 'not_found_species', message: 'Không tìm thấy loài yêu cầu.' });
    }

    // Tìm cấu hình tùy chọn
    const customConfig = await prisma.responseConfig.findUnique({
      where: {
        cameraId_speciesId: {
          cameraId: cameraId as string,
          speciesId: speciesId as string
        }
      }
    });

    if (customConfig) {
      return res.status(200).json({
        id: customConfig.id,
        cameraId: customConfig.cameraId,
        speciesId: customConfig.speciesId,
        ledFlash: customConfig.ledFlashRate ? true : false,
        ledColor: customConfig.ledColor,
        ledIntensity: customConfig.ledDurationSeconds, // Ánh xạ trường
        speakerWarn: customConfig.speakerSampleId ? true : false,
        audioSampleId: customConfig.audioSampleId,
        audioIntensity: customConfig.audioIntensity,
        electricFence: customConfig.fenceLevel ? true : false,
        electricFenceDuration: customConfig.ledDurationSeconds, // Giả định trường tương đương
        silentAlert: customConfig.silentAlert
      });
    }

    // Nếu không có tùy chọn, lấy cấu hình mặc định (Danger Preset)
    const fallback = getRecommendedPresetForSpecies(species.id, species.dangerLevel);
    return res.status(200).json({
      cameraId,
      speciesId,
      ...fallback
    });
  } catch (error) {
    console.error('Lỗi khi lấy chi tiết cấu hình:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 4. PUT /response-configs/{cameraId}/{speciesId} - Lưu cấu hình phòng vệ tùy chọn
export async function saveConfig(req: AuthenticatedRequest, res: Response) {
  const { cameraId, speciesId } = req.params;
  const {
    ledFlash,
    ledColor,
    ledIntensity,
    speakerWarn,
    audioSampleId,
    audioIntensity,
    electricFence,
    electricFenceDuration,
    silentAlert
  } = req.body;

  // Validation: Thiếu trường required
  if (ledFlash === undefined) {
    return res.status(400).json({ error: 'missed_led_flash', message: 'Thiếu thông tin bắt buộc: ledFlash.' });
  }
  if (speakerWarn === undefined) {
    return res.status(400).json({ error: 'missed_speaker_warn', message: 'Thiếu thông tin bắt buộc: speakerWarn.' });
  }
  if (electricFence === undefined) {
    return res.status(400).json({ error: 'missed_electric_fence', message: 'Thiếu thông tin bắt buộc: electricFence.' });
  }
  if (silentAlert === undefined) {
    return res.status(400).json({ error: 'missed_silent_alert', message: 'Thiếu thông tin bắt buộc: silentAlert.' });
  }

  // Validation: Sai cấu trúc Led Color Enum (RED, YELLOW, WHITE, STROBE)
  const validColors = ['RED', 'YELLOW', 'WHITE', 'STROBE'];
  if (ledColor && !validColors.includes(ledColor)) {
    return res.status(400).json({ error: 'invalid_led_color', message: `Màu LED không hợp lệ. Phải thuộc: ${validColors.join(', ')}.` });
  }

  // Validation: Sai khoảng giá trị (intensity: 0-100, duration >= 0)
  if (ledIntensity !== undefined && (ledIntensity < 0 || ledIntensity > 100)) {
    return res.status(400).json({ error: 'invalid_led_intensity', message: 'Cường độ LED (ledIntensity) phải nằm trong khoảng từ 0 đến 100.' });
  }
  if (audioIntensity !== undefined && (audioIntensity < 0 || audioIntensity > 100)) {
    return res.status(400).json({ error: 'invalid_audio_intensity', message: 'Cường độ âm lượng loa (audioIntensity) phải nằm trong khoảng từ 0 đến 100.' });
  }
  if (electricFenceDuration !== undefined && electricFenceDuration < 0) {
    return res.status(400).json({ error: 'invalid_electric_fence_duration', message: 'Thời gian hàng rào điện (electricFenceDuration) phải lớn hơn hoặc bằng 0.' });
  }

  try {
    const camera = await prisma.camera.findUnique({ where: { id: cameraId } });
    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera.' });
    }

    const species = await prisma.species.findUnique({ where: { id: speciesId } });
    if (!species) {
      return res.status(404).json({ error: 'not_found_species', message: 'Không tìm thấy loài động vật.' });
    }

    // Upsert cấu hình tùy chỉnh vào database
    const configData = {
      audioSampleId: audioSampleId || null,
      audioIntensity: audioIntensity || 0,
      ledFlashRate: ledFlash ? 'FAST' : null,
      ledColor: ledColor || null,
      ledDurationSeconds: ledIntensity || 0, // Ánh xạ trường
      fenceLevel: electricFence ? 'HIGH' : null,
      silentAlert,
      lastModifiedBy: req.user!.id
    };

    const savedConfig = await prisma.responseConfig.upsert({
      where: {
        cameraId_speciesId: { cameraId, speciesId }
      },
      create: {
        cameraId,
        speciesId,
        ...configData
      },
      update: configData
    });

    return res.status(200).json({
      cameraId: savedConfig.cameraId,
      speciesId: savedConfig.speciesId,
      ledFlash,
      ledColor: savedConfig.ledColor,
      ledIntensity: savedConfig.ledDurationSeconds,
      speakerWarn,
      audioSampleId: savedConfig.audioSampleId,
      audioIntensity: savedConfig.audioIntensity,
      electricFence,
      electricFenceDuration,
      silentAlert: savedConfig.silentAlert
    });
  } catch (error) {
    console.error('Lỗi khi lưu cấu hình:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 5. DELETE /response-configs/{cameraId}/{speciesId} - Xóa cấu hình tùy chỉnh để quay về mặc định
export async function resetConfig(req: AuthenticatedRequest, res: Response) {
  const { cameraId, speciesId } = req.params;

  try {
    const existingConfig = await prisma.responseConfig.findUnique({
      where: {
        cameraId_speciesId: { cameraId, speciesId }
      }
    });

    if (!existingConfig) {
      return res.status(404).json({ error: 'not_found_config', message: 'Không tìm thấy cấu hình tùy chỉnh để xóa.' });
    }

    await prisma.responseConfig.delete({
      where: {
        cameraId_speciesId: { cameraId, speciesId }
      }
    });

    return res.status(204).send();
  } catch (error) {
    console.error('Lỗi khi đặt lại cấu hình mặc định:', error);
    return res.status(500).json({ error: 'server_error', message: 'Lỗi máy chủ nội bộ.' });
  }
}

// 6. POST /response-configs/{cameraId}/{speciesId}/apply-preset/{presetId} - Áp nhanh preset
export async function applyPreset(req: AuthenticatedRequest, res: Response) {
  const { cameraId, speciesId, presetId } = req.params;

  const preset = PRESET_SCENARIOS[presetId];
  if (!preset) {
    return res.status(404).json({ error: 'not_found_preset', message: 'Không tìm thấy preset mẫu yêu cầu.' });
  }

  try {
    const camera = await prisma.camera.findUnique({ where: { id: cameraId } });
    if (!camera) {
      return res.status(404).json({ error: 'not_found_camera', message: 'Không tìm thấy trạm camera.' });
    }

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
      fenceLevel: preset.electricFence ? 'HIGH' : null,
      silentAlert: typeof preset.silentAlert === 'boolean' ? preset.silentAlert : false,
      lastModifiedBy: req.user!.id
    };

    const updatedConfig = await prisma.responseConfig.upsert({
      where: {
        cameraId_speciesId: { cameraId, speciesId }
      },
      create: {
        cameraId,
        speciesId,
        ...configData
      },
      update: configData
    });

    return res.status(200).json({
      cameraId: updatedConfig.cameraId,
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
    name: id === 'critical_danger' ? 'Xua đuổi khẩn cấp (Voi)' : id === 'medium_danger' ? 'Xua đuổi tầm trung' : 'Cảnh báo đột nhập',
    config: val
  }));
  return res.status(200).json(list);
}

// 8. GET /audio-samples - Tải danh mục âm thanh mẫu
export async function listAudioSamples(_req: AuthenticatedRequest, res: Response) {
  const result = {
    audio_samples: [
      { id: 'A_gunshot', name: 'Tiếng súng nổ đe dọa', durationSeconds: 3 },
      { id: 'A_dog_bark', name: 'Tiếng chó sủa dữ dội', durationSeconds: 5 },
      { id: 'A_alarm_siren', name: 'Tiếng còi hú khẩn cấp', durationSeconds: 6 }
    ],
    speaker_samples: [
      { id: 'S_warn_citizen', name: 'Phát loa cảnh báo người dân có thú dữ', durationSeconds: 8 }
    ]
  };
  return res.status(200).json(result);
}
