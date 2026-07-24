import fs from 'fs';
import path from 'path';
import yaml from 'js-yaml';
import { DangerLevel } from '@prisma/client';

export interface DefenseConfig {
  ledFlash: boolean;
  ledColor: string | null;
  ledIntensity: number;
  speakerWarn: boolean;
  audioSampleId: string | null;
  audioIntensity: number;
  electricFence: boolean;
  electricFenceDuration: number;
  silentAlert: boolean;
}

interface AnimalRecommendRaw {
  speciesId: string;
  inheritPreset: string;
  override?: Partial<DefenseConfig>;
}

const FALLBACK_DANGER_PRESETS: Record<DangerLevel, DefenseConfig> = {
  [DangerLevel.LOW]: {
    ledFlash: false,
    ledColor: 'WHITE',
    ledIntensity: 0,
    speakerWarn: false,
    audioSampleId: null,
    audioIntensity: 0,
    electricFence: false,
    electricFenceDuration: 0,
    silentAlert: true
  },
  [DangerLevel.MEDIUM]: {
    ledFlash: true,
    ledColor: 'YELLOW',
    ledIntensity: 50,
    speakerWarn: true,
    audioSampleId: 'A_dog_bark',
    audioIntensity: 50,
    electricFence: false,
    electricFenceDuration: 0,
    silentAlert: false
  },
  [DangerLevel.HIGH]: {
    ledFlash: true,
    ledColor: 'RED',
    ledIntensity: 80,
    speakerWarn: true,
    audioSampleId: 'A_alarm_siren',
    audioIntensity: 80,
    electricFence: false,
    electricFenceDuration: 0,
    silentAlert: false
  },
  [DangerLevel.CRITICAL]: {
    ledFlash: true,
    ledColor: 'STROBE',
    ledIntensity: 100,
    speakerWarn: true,
    audioSampleId: 'A_gunshot',
    audioIntensity: 100,
    electricFence: true,
    electricFenceDuration: 15,
    silentAlert: false
  }
};

const FALLBACK_PRESET_SCENARIOS: Record<string, DefenseConfig> = {
  critical_danger: {
    ledFlash: true,
    ledColor: 'STROBE',
    ledIntensity: 100,
    speakerWarn: true,
    audioSampleId: 'A_gunshot',
    audioIntensity: 100,
    electricFence: true,
    electricFenceDuration: 15,
    silentAlert: false
  },
  medium_danger: {
    ledFlash: true,
    ledColor: 'YELLOW',
    ledIntensity: 50,
    speakerWarn: true,
    audioSampleId: 'A_dog_bark',
    audioIntensity: 50,
    electricFence: false,
    electricFenceDuration: 0,
    silentAlert: false
  },
  intruder: {
    ledFlash: true,
    ledColor: 'RED',
    ledIntensity: 90,
    speakerWarn: true,
    audioSampleId: 'A_alarm_siren',
    audioIntensity: 90,
    electricFence: false,
    electricFenceDuration: 0,
    silentAlert: false
  }
};

const DEFAULT_DANGER_LEVEL_MAP: Record<DangerLevel, string> = {
  [DangerLevel.LOW]: 'intruder',
  [DangerLevel.MEDIUM]: 'medium_danger',
  [DangerLevel.HIGH]: 'critical_danger',
  [DangerLevel.CRITICAL]: 'critical_danger'
};

const loadedPresetScenarios = { ...FALLBACK_PRESET_SCENARIOS };
const loadedDangerLevelMap = { ...DEFAULT_DANGER_LEVEL_MAP };
const loadedAnimalRecommends: Record<string, AnimalRecommendRaw> = {};

// 1. Đọc file danger-presets.yaml
try {
  const yamlPath = path.join(__dirname, '../../hard-config/danger-presets.yaml');
  if (fs.existsSync(yamlPath)) {
    const fileContent = fs.readFileSync(yamlPath, 'utf8');
    const doc = yaml.load(fileContent) as Record<string, unknown> | null;

    if (doc) {
      if (doc.preset_scenarios) {
        const scenarios = doc.preset_scenarios as Record<string, Partial<DefenseConfig>>;
        Object.keys(FALLBACK_PRESET_SCENARIOS).forEach((key) => {
          loadedPresetScenarios[key] = {
            ...FALLBACK_PRESET_SCENARIOS[key],
            ...(scenarios[key] || {})
          };
        });
      }

      if (doc.danger_level_to_preset_map) {
        const dMap = doc.danger_level_to_preset_map as Record<DangerLevel, string>;
        Object.keys(DEFAULT_DANGER_LEVEL_MAP).forEach((key) => {
          const dl = key as DangerLevel;
          if (dMap[dl]) {
            loadedDangerLevelMap[dl] = dMap[dl];
          }
        });
      }

      console.log('[Presets] Đã load tệp cấu hình danger-presets.yaml thành công.');
    }
  } else {
    console.warn('[Presets] Không tìm thấy file danger-presets.yaml, sử dụng cấu hình mặc định.');
  }
} catch (error) {
  console.error('[Presets] Lỗi khi load file cấu hình danger-presets.yaml:', error);
}

// 2. Đọc file animal-recommend.yaml
try {
  const recommendPath = path.join(__dirname, '../../hard-config/animal-recommend.yaml');
  if (fs.existsSync(recommendPath)) {
    const fileContent = fs.readFileSync(recommendPath, 'utf8');
    const list = yaml.load(fileContent) as AnimalRecommendRaw[] | null;

    if (Array.isArray(list)) {
      list.forEach((item) => {
        if (item && item.speciesId) {
          loadedAnimalRecommends[item.speciesId] = item;
        }
      });
      console.log('[Presets] Đã load tệp cấu hình animal-recommend.yaml thành công.');
    }
  } else {
    console.warn('[Presets] Không tìm thấy file animal-recommend.yaml, bỏ qua cấu hình đề xuất riêng.');
  }
} catch (error) {
  console.error('[Presets] Lỗi khi load file cấu hình animal-recommend.yaml:', error);
}

/**
 * Hàm tra cứu cấu hình đề xuất cho từng loài cụ thể.
 * Hỗ trợ cơ chế Kế thừa (inheritPreset) và Ghi đè (override).
 */
export function getRecommendedPresetForSpecies(speciesId: string, dangerLevel: DangerLevel): DefenseConfig {
  const recommend = loadedAnimalRecommends[speciesId];
  if (recommend) {
    // Tìm preset được kế thừa
    const inheritName = recommend.inheritPreset.startsWith('@') 
      ? recommend.inheritPreset.substring(1) 
      : recommend.inheritPreset;
    
    // Lấy cấu hình gốc của preset kế thừa
    let baseConfig = loadedPresetScenarios[inheritName];
    if (!baseConfig) {
      // Nếu preset kế thừa không tồn tại, fallback về kịch bản theo Danger Level
      const mappedPreset = loadedDangerLevelMap[dangerLevel] || DEFAULT_DANGER_LEVEL_MAP[dangerLevel];
      baseConfig = loadedPresetScenarios[mappedPreset] || FALLBACK_PRESET_SCENARIOS[mappedPreset];
    }

    // Merge ghi đè với cấu hình override
    return {
      ...baseConfig,
      ...(recommend.override || {})
    };
  }

  // Nếu không có cấu hình đề xuất riêng cho loài, lấy cấu hình theo Danger Level tương ứng
  const targetPreset = loadedDangerLevelMap[dangerLevel] || DEFAULT_DANGER_LEVEL_MAP[dangerLevel];
  const config = loadedPresetScenarios[targetPreset] || FALLBACK_PRESET_SCENARIOS[targetPreset];
  if (config) {
    return config;
  }

  // Fallback cuối cùng
  return FALLBACK_DANGER_PRESETS[dangerLevel];
}

// Xuất các biến hằng để tương thích ngược với code cũ
export const PRESET_SCENARIOS = loadedPresetScenarios;
export const DANGER_PRESETS: Record<DangerLevel, DefenseConfig> = {
  [DangerLevel.LOW]: getRecommendedPresetForSpecies('low_dummy', DangerLevel.LOW),
  [DangerLevel.MEDIUM]: getRecommendedPresetForSpecies('medium_dummy', DangerLevel.MEDIUM),
  [DangerLevel.HIGH]: getRecommendedPresetForSpecies('high_dummy', DangerLevel.HIGH),
  [DangerLevel.CRITICAL]: getRecommendedPresetForSpecies('critical_dummy', DangerLevel.CRITICAL)
};
