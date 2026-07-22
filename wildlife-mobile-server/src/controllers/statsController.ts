import { Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthenticatedRequest } from '../middlewares/auth';

const prisma = new PrismaClient();

// GET /stats/summary - Lấy thống kê tổng hợp biểu đồ & heatmap
export async function getSummary(req: AuthenticatedRequest, res: Response) {
  const { startDate, endDate } = req.query;

  // Validation: Thiếu các trường required
  if (!startDate) {
    return res.status(400).json({ error: 'missed_start_date', message: 'Thiếu thông tin bắt buộc: startDate.' });
  }
  if (!endDate) {
    return res.status(400).json({ error: 'missed_end_date', message: 'Thiếu thông tin bắt buộc: endDate.' });
  }

  // Validation: Định dạng ngày sai ISO 8601
  const isoDateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!isoDateRegex.test(startDate as string) || !isoDateRegex.test(endDate as string)) {
    return res.status(400).json({ error: 'invalid_date_format', message: 'Định dạng ngày tháng không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.' });
  }

  const start = new Date(startDate as string);
  const end = new Date(endDate as string);

  // Validation: Lỗi logic ngày bắt đầu sau ngày kết thúc
  if (start.getTime() > end.getTime()) {
    return res.status(400).json({ error: 'invalid_date_range', message: 'Lỗi logic dữ liệu: ngày bắt đầu (startDate) không được lớn hơn ngày kết thúc (endDate).' });
  }

  try {
    // 1. Tải toàn bộ nhận dạng trong khoảng ngày
    const detections = await prisma.eventDetection.findMany({
      where: {
        detectedAt: {
          gte: start,
          lte: new Date(end.getTime() + 24 * 60 * 60 * 1000) // hết ngày kết thúc
        }
      },
      include: {
        species: true,
        event: {
          include: { camera: true }
        }
      }
    });

    // 2. Tính toán trendData (lượng phát hiện theo ngày)
    const trendMap: Record<string, number> = {};
    // Khởi tạo các ngày với giá trị 0
    const current = new Date(start);
    while (current <= end) {
      const dateStr = current.toISOString().split('T')[0];
      trendMap[dateStr] = 0;
      current.setDate(current.getDate() + 1);
    }

    detections.forEach((d) => {
      const dateStr = d.detectedAt.toISOString().split('T')[0];
      if (trendMap[dateStr] !== undefined) {
        trendMap[dateStr]++;
      }
    });

    const trendData = Object.entries(trendMap).map(([date, count]) => ({
      date,
      count
    }));

    // 3. Tính toán breakdown theo loài (speciesBreakdown)
    const speciesMap: Record<string, { displayName: string; count: number }> = {};
    detections.forEach((d) => {
      if (!speciesMap[d.speciesId]) {
        speciesMap[d.speciesId] = {
          displayName: d.species.displayName,
          count: 0
        };
      }
      speciesMap[d.speciesId].count++;
    });

    const speciesBreakdown = Object.entries(speciesMap).map(([speciesId, val]) => ({
      speciesId,
      displayName: val.displayName,
      count: val.count
    }));

    // 4. Tính toán heatmapData (tọa độ GPS + tần suất)
    const heatmapMap: Record<string, { lat: number; lng: number; intensity: number }> = {};
    detections.forEach((d) => {
      const cam = d.event.camera;
      const key = `${cam.latitude}_${cam.longitude}`;
      if (!heatmapMap[key]) {
        heatmapMap[key] = {
          lat: cam.latitude,
          lng: cam.longitude,
          intensity: 0
        };
      }
      heatmapMap[key].intensity++;
    });

    const heatmapData = Object.values(heatmapMap);

    return res.status(200).json({
      trendData,
      speciesBreakdown,
      heatmapData
    });
  } catch (error) {
    console.error('Lỗi khi tính toán thống kê tổng hợp:', error);
    return res.status(500).json({ error: 'Lỗi máy chủ nội bộ.' });
  }
}
