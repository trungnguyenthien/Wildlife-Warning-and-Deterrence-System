import { PrismaClient, Role, CameraStatus, DangerLevel, AlertType, SmsRelation } from '@prisma/client';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

async function main() {
  console.log('=== BẮT ĐẦU SEED BỘ DỮ LIỆU PHONG PHÚ CHO KIỂM THỬ PRODUCTION ===');

  // 1. Tạo tài khoản Kiểm lâm Ranger Demo
  const rUsername = 'ranger_demo';
  const rPhone = '+84900000001';
  const hashedPassword = bcrypt.hashSync('DemoPassword123!', 10);

  // Xóa tài khoản cũ nếu có để đưa ID về dạng 4 ký tự ngắn gọn
  const existingRanger = await prisma.user.findUnique({ where: { username: rUsername } });
  if (existingRanger) {
    await prisma.responseConfig.deleteMany({ where: { lastModifiedBy: existingRanger.id } });
    await prisma.smsRecipient.deleteMany({ where: { userId: existingRanger.id } });
    await prisma.alertRead.deleteMany({ where: { userId: existingRanger.id } });
    await prisma.deviceToken.deleteMany({ where: { userId: existingRanger.id } });
    await prisma.pushToken.deleteMany({ where: { userId: existingRanger.id } });
    await prisma.user.delete({ where: { id: existingRanger.id } });
  }

  const ranger = await prisma.user.create({
    data: {
      id: 'u_rg', // Gán ID ngắn gọn 4 ký tự đẹp mắt
      username: rUsername,
      passwordHash: hashedPassword,
      fullName: 'Trần Văn Kiểm Lâm',
      phoneNumber: rPhone,
      role: Role.RANGER
    }
  });
  console.log(`[User] Đã nạp tài khoản Kiểm lâm: ${ranger.username} với ID: ${ranger.id} (Mật khẩu: DemoPassword123!)`);

  // Tạo thêm số điện thoại phụ để nhận SMS (SmsRecipient)
  await prisma.smsRecipient.deleteMany({ where: { userId: ranger.id } });
  await prisma.smsRecipient.createMany({
    data: [
      {
        userId: ranger.id,
        fullName: 'Nguyễn Thị Gia Đình',
        phoneNumber: '+84900000002',
        relation: SmsRelation.family
      },
      {
        userId: ranger.id,
        fullName: 'Lê Văn Hàng Xóm',
        phoneNumber: '+84900000003',
        relation: SmsRelation.neighbor
      }
    ]
  });
  console.log(`[SMS] Đã đăng ký 2 số điện thoại phụ nhận cảnh báo khẩn cấp.`);

  // 2. Tạo danh mục Loài động vật (Species)
  const speciesList = [
    {
      id: 'voi_rung',
      displayName: 'Voi Rừng Tây Nguyên',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Voi châu Á (Elephas maximus) phân bố tại Tây Nguyên. Cực kỳ nguy hiểm khi xung đột khu dân cư.</p>',
      aggressionLevel: 90,
      recommendAction: 'Không kích động voi, tắt đèn pha, di tản khỏi khu vực hàng rào điện và gọi ngay cho trạm kiểm lâm.'
    },
    {
      id: 'ho_dong_duong',
      displayName: 'Hổ Đông Dương',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Hổ Đông Dương (Panthera tigris corbetti). Loài thú ăn thịt đầu bảng nguy cấp cực kỳ cao.</p>',
      aggressionLevel: 95,
      recommendAction: 'Đóng kín cửa chuồng trại gia súc, di tản trẻ nhỏ vào nhà kiên cố, tuyệt đối không săn đuổi.'
    },
    {
      id: 'monkey',
      displayName: 'Khỉ Vàng',
      dangerLevel: DangerLevel.LOW,
      isHuman: false,
      htmlDescription: '<p>Khỉ vàng (Macaca mulatta) thường đi theo đàn phá hoại nông sản nhẹ.</p>',
      aggressionLevel: 30,
      recommendAction: 'Sử dụng âm thanh chó sủa lớn hoặc chớp LED trắng để xua đuổi nhẹ nhàng khỏi ruộng rẫy.'
    },
    {
      id: 'nai_vang',
      displayName: 'Nai Vàng Nam Bộ',
      dangerLevel: DangerLevel.MEDIUM,
      isHuman: false,
      htmlDescription: '<p>Nai vàng (Rusa unicolor) ăn cỏ, có thể đi vào phá hoa màu vào ban đêm.</p>',
      aggressionLevel: 10,
      recommendAction: 'Kích hoạt hàng rào điện sinh học nhẹ và chớp LED vàng cảnh báo tại chỗ.'
    },
    {
      id: 'human_border_intruder',
      displayName: 'Người lạ xâm nhập biên giới',
      dangerLevel: DangerLevel.HIGH,
      isHuman: true,
      htmlDescription: '<p>Đối tượng xâm nhập vùng cấm biên giới hoặc lâm tặc phá hoại rừng.</p>',
      aggressionLevel: 50,
      recommendAction: 'Lực lượng Biên phòng và Kiểm lâm lập tức triển khai tuần tra hiện trường khẩn cấp.'
    }
  ];

  for (const s of speciesList) {
    await prisma.species.upsert({
      where: { id: s.id },
      update: s,
      create: s
    });
  }
  console.log(`[Species] Đã nạp danh mục 5 loài động vật chuẩn.`);

  // 3. Tạo 4 Trạm Camera (Camera)
  const cameras = [
    {
      id: 'camera_01',
      name: 'Trạm Bờ Sông Đăk Bla',
      latitude: 14.3496,
      longitude: 108.0062,
      address: 'Xã Đăk Rơ Wa, Thành phố Kon Tum, Kon Tum',
      status: CameraStatus.ONLINE,
      liveFeedUrl: 'https://www.w3schools.com/html/mov_bbb.mp4'
    },
    {
      id: 'camera_02',
      name: 'Cửa Rừng Quốc Gia Yok Don',
      latitude: 12.8764,
      longitude: 107.7289,
      address: 'Huyện Buôn Đôn, Đắk Lắk',
      status: CameraStatus.ONLINE,
      liveFeedUrl: 'https://www.w3schools.com/html/mov_bbb.mp4'
    },
    {
      id: 'camera_03',
      name: 'Vành Đai Biên Giới Đức Cơ',
      latitude: 13.8052,
      longitude: 107.4583,
      address: 'Cửa khẩu Quốc tế Lệ Thanh, Đức Cơ, Gia Lai',
      status: CameraStatus.ONLINE,
      liveFeedUrl: 'https://www.w3schools.com/html/mov_bbb.mp4'
    },
    {
      id: 'camera_04',
      name: 'Trạm Kiểm Soát Đèo Mang Yang',
      latitude: 13.9931,
      longitude: 108.2614,
      address: 'Quốc lộ 19, Mang Yang, Gia Lai',
      status: CameraStatus.ONLINE,
      liveFeedUrl: 'https://www.w3schools.com/html/mov_bbb.mp4'
    }
  ];

  for (const c of cameras) {
    await prisma.camera.upsert({
      where: { id: c.id },
      update: c,
      create: c
    });
  }
  console.log(`[Camera] Đã nạp 4 trạm camera giám sát biên phòng và lâm phận.`);

  // 4. Nạp sẵn cấu hình phòng vệ tùy chỉnh (ResponseConfig) cho nhiều loài
  await prisma.responseConfig.deleteMany({});
  await prisma.responseConfig.createMany({
    data: [
      {
        cameraId: 'camera_01',
        speciesId: 'voi_rung',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_gunshot',
        audioIntensity: 90,
        ledFlashRate: 'FAST',
        ledColor: 'RED',
        ledDurationSeconds: 20,
        fenceLevel: 'HIGH',
        fenceWarningLight: true,
        fenceAutoNotify: true,
        fenceAutoOffEnabled: true,
        fenceAutoOffMinutes: 2,
        speakerSampleId: 'N_voi_rung',
        silentAlert: false
      },
      {
        cameraId: 'camera_02',
        speciesId: 'ho_dong_duong',
        lastModifiedBy: ranger.id,
        audioSampleId: null,
        audioIntensity: 0,
        ledFlashRate: null,
        ledColor: null,
        ledDurationSeconds: 0,
        fenceLevel: null,
        fenceWarningLight: false,
        fenceAutoNotify: false,
        fenceAutoOffEnabled: false,
        fenceAutoOffMinutes: 0,
        speakerSampleId: null,
        silentAlert: true // Cảnh báo âm thầm đối với Hổ tại Cửa Rừng
      },
      {
        cameraId: 'camera_03',
        speciesId: 'human_border_intruder',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_alarm_siren',
        audioIntensity: 100,
        ledFlashRate: 'FAST',
        ledColor: 'STROBE',
        ledDurationSeconds: 30,
        fenceLevel: null,
        fenceWarningLight: true,
        fenceAutoNotify: true,
        fenceAutoOffEnabled: false,
        fenceAutoOffMinutes: 0,
        speakerSampleId: 'N_thu_du',
        silentAlert: false
      },
      {
        cameraId: 'camera_04',
        speciesId: 'nai_vang',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_dog_bark',
        audioIntensity: 60,
        ledFlashRate: 'SLOW',
        ledColor: 'YELLOW',
        ledDurationSeconds: 10,
        fenceLevel: 'LOW',
        fenceWarningLight: false,
        fenceAutoNotify: false,
        fenceAutoOffEnabled: true,
        fenceAutoOffMinutes: 1,
        speakerSampleId: null,
        silentAlert: false
      }
    ]
  });
  console.log(`[ResponseConfig] Đã nạp sẵn cấu hình phòng vệ tùy chỉnh (Custom Config) cho nhiều cặp Camera-Loài.`);

  // 5. Tạo lịch sử Sự kiện & Cảnh báo (Event, Detection, Alert) phong phú trong 30 ngày qua
  console.log('[Event] Bắt đầu sinh ngẫu nhiên nhật ký sự kiện 30 ngày qua...');
  await prisma.alertRead.deleteMany({});
  await prisma.alert.deleteMany({});
  await prisma.deviceLog.deleteMany({});
  await prisma.eventDetection.deleteMany({});
  await prisma.event.deleteMany({});

  const now = new Date();
  let eventCounter = 1;

  // Lặp qua 30 ngày
  for (let offsetDays = 30; offsetDays >= 0; offsetDays--) {
    const dayDate = new Date(now.getTime() - offsetDays * 24 * 60 * 60 * 1000);
    
    // Mỗi ngày sinh từ 1 đến 3 sự kiện
    const numEventsOfDay = Math.floor(Math.random() * 3) + 1;

    for (let e = 0; e < numEventsOfDay; e++) {
      // Thiết lập giờ ngẫu nhiên trong ngày
      const eventTime = new Date(dayDate);
      eventTime.setHours(Math.floor(Math.random() * 24), Math.floor(Math.random() * 60));

      const eventId = `evt_prod_seed_${eventCounter}`;
      const alertId = `alt_prod_seed_${eventCounter}`;
      
      // Chọn ngẫu nhiên camera
      const camera = cameras[Math.floor(Math.random() * cameras.length)];
      
      // Chọn ngẫu nhiên loài
      const species = speciesList[Math.floor(Math.random() * speciesList.length)];
      
      // Ảnh ngẫu nhiên từ Picsum
      const snapshotUrl = `https://picsum.photos/seed/${eventId}/800/600`;

      // Xác định loại Alert
      let alertType = AlertType.ANIMAL_RARE;
      let alertTitle = `Phát hiện ${species.displayName}`;
      if (species.isHuman) {
        alertType = AlertType.HUMAN_BORDER;
        alertTitle = `Xâm nhập: ${species.displayName}`;
      } else if (species.dangerLevel === DangerLevel.LOW) {
        alertType = AlertType.INTRUDER;
      }

      // A. Lưu Event
      await prisma.event.create({
        data: {
          id: eventId,
          cameraId: camera.id,
          detectedAt: eventTime,
          snapshotUrl
        }
      });

      // B. Lưu EventDetection
      await prisma.eventDetection.create({
        data: {
          eventId,
          speciesId: species.id,
          confidence: parseFloat((0.85 + Math.random() * 0.14).toFixed(2)),
          detectedAt: eventTime
        }
      });

      // C. Lưu Alert
      await prisma.alert.create({
        data: {
          id: alertId,
          type: alertType,
          title: alertTitle,
          dangerLevel: species.dangerLevel,
          cameraId: camera.id,
          eventId,
          createdAt: eventTime
        }
      });

      // D. Lưu DeviceLog giả lập phản ứng cho các loài nguy hiểm
      if (species.dangerLevel === DangerLevel.CRITICAL || species.dangerLevel === DangerLevel.HIGH) {
        await prisma.deviceLog.create({
          data: {
            eventId,
            deviceKey: 'speaker',
            action: 'ON',
            actionAt: eventTime
          }
        });
        if (species.id === 'voi_rung') {
          await prisma.deviceLog.create({
            data: {
              eventId,
              deviceKey: 'fence',
              action: 'ON',
              actionAt: eventTime
            }
          });
        }
      }

      eventCounter++;
    }
  }

  console.log(`[Event/Alert] Đã sinh và nạp thành công ${eventCounter - 1} sự kiện & cảnh báo.`);
  console.log('=== HOÀN TẤT SEED DỮ LIỆU THỬ NGHIỆM PHONG PHÚ TRÊN PRODUCTION ===');
}

main()
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (e) => {
    console.error('Lỗi khi seed dữ liệu:', e);
    await prisma.$disconnect();
    process.exit(1);
  });
