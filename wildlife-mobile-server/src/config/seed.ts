import { PrismaClient, Role, CameraStatus, DangerLevel, AlertType, SmsRelation } from '@prisma/client';
import bcrypt from 'bcrypt';

export async function runSeed(prisma: PrismaClient) {
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
      id: 'elephant',
      displayName: 'Voi',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Voi châu Á (Elephas maximus) thường di chuyển gần các vùng canh tác hoa màu của dân cư Tây Nguyên.</p>',
      aggressionLevel: 90,
      recommendAction: 'Tránh kích động voi, tắt các nguồn ánh sáng mạnh trực diện, thông báo khẩn cấp cho ban chỉ huy lâm phận.'
    },
    {
      id: 'tiger',
      displayName: 'Hổ',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Hổ Đông Dương (Panthera tigris corbetti) săn mồi nguy hiểm.</p>',
      aggressionLevel: 95,
      recommendAction: 'Đóng cửa chuồng trại, đưa trẻ nhỏ vào vùng an toàn, không săn bắt hổ trái phép.'
    },
    {
      id: 'monkey',
      displayName: 'Khỉ',
      dangerLevel: DangerLevel.LOW,
      isHuman: false,
      htmlDescription: '<p>Khỉ vàng thường đi theo đàn lớn tàn phá cây trái ruộng rẫy.</p>',
      aggressionLevel: 30,
      recommendAction: 'Sử dụng âm thanh chó sủa lớn hoặc còi hú nhẹ kèm LED sáng trắng để xua đuổi khỉ.'
    },
    {
      id: 'human',
      displayName: 'Người',
      dangerLevel: DangerLevel.HIGH,
      isHuman: true,
      htmlDescription: '<p>Người lạ xâm nhập khu vực bảo tồn nghiêm ngặt hoặc biên giới.</p>',
      aggressionLevel: 50,
      recommendAction: 'Lực lượng biên phòng phối hợp kiểm lâm lập tức tổ chức tuần tra xác minh thực địa.'
    },
    {
      id: 'crocodile',
      displayName: 'Cá sấu',
      dangerLevel: DangerLevel.HIGH,
      isHuman: false,
      htmlDescription: '<p>Cá sấu thường xuất hiện ở sông ngòi ven rừng phòng hộ biên giới.</p>',
      aggressionLevel: 80,
      recommendAction: 'Không di chuyển đến gần mép nước, sử dụng thiết bị báo động tại chỗ cảnh báo người dân.'
    },
    {
      id: 'giraffe',
      displayName: 'Hươu cao cổ',
      dangerLevel: DangerLevel.LOW,
      isHuman: false,
      htmlDescription: '<p>Hươu cao cổ ăn lá trên cao, hoàn toàn hiền lành và thân thiện.</p>',
      aggressionLevel: 10,
      recommendAction: 'Hệ thống chỉ cảnh báo nhẹ hoặc ghi nhận thông tin và bỏ qua chế độ xua đuổi mạnh.'
    },
    {
      id: 'leopard',
      displayName: 'Báo',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Báo hoa mai hoặc báo gấm ăn thịt. Tốc độ di chuyển và săn mồi nhanh.</p>',
      aggressionLevel: 85,
      recommendAction: 'Kích hoạt ngay còi hú xua đuổi và cảnh báo người dân xung quanh khu vực.'
    },
    {
      id: 'rhino',
      displayName: 'Tê giác',
      dangerLevel: DangerLevel.CRITICAL,
      isHuman: false,
      htmlDescription: '<p>Tê giác một sừng cực kỳ quý hiếm, có tập tính húc phá khi hoảng sợ.</p>',
      aggressionLevel: 75,
      recommendAction: 'Hạn chế tiếng ồn lớn, kích hoạt LED STROBE giữ khoảng cách an toàn cho tê giác.'
    },
    {
      id: 'snake',
      displayName: 'Rắn',
      dangerLevel: DangerLevel.MEDIUM,
      isHuman: false,
      htmlDescription: '<p>Các loại rắn độc xuất hiện gần bờ cỏ hoặc khu dân cư ven rừng.</p>',
      aggressionLevel: 40,
      recommendAction: 'Quan sát kỹ lối đi, tránh cỏ rậm, sử dụng thiết bị rung giật nhẹ để xua đuổi bò sát.'
    },
    {
      id: 'deer',
      displayName: 'Nai',
      dangerLevel: DangerLevel.MEDIUM,
      isHuman: false,
      htmlDescription: '<p>Nai vàng hoặc nai rừng tìm kiếm thức ăn vào ban đêm.</p>',
      aggressionLevel: 15,
      recommendAction: 'Sử dụng đèn LED chớp vàng và còi chó sủa cường độ vừa phải để điều hướng nai ra khỏi rẫy.'
    }
  ];

  for (const s of speciesList) {
    await prisma.species.upsert({
      where: { id: s.id },
      update: s,
      create: s
    });
  }
  console.log(`[Species] Đã nạp danh mục 10 loài động vật chuẩn.`);

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
        userId: ranger.id,
        speciesId: 'elephant',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_gunshot',
        audioIntensity: 90,
        ledFlashRate: 'FAST',
        ledColor: 'RED',
        ledDurationSeconds: 20,
        speakerSampleId: 'tiger',
        silentAlert: false
      },
      {
        userId: ranger.id,
        speciesId: 'tiger',
        lastModifiedBy: ranger.id,
        audioSampleId: null,
        audioIntensity: 0,
        ledFlashRate: null,
        ledColor: null,
        ledDurationSeconds: 0,
        speakerSampleId: null,
        silentAlert: true // Cảnh báo âm thầm đối với Hổ tại Cửa Rừng
      },
      {
        userId: ranger.id,
        speciesId: 'human',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_alarm_siren',
        audioIntensity: 100,
        ledFlashRate: 'FAST',
        ledColor: 'STROBE',
        ledDurationSeconds: 30,
        speakerSampleId: 'monkey',
        silentAlert: false
      },
      {
        userId: ranger.id,
        speciesId: 'deer',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_dog_bark',
        audioIntensity: 60,
        ledFlashRate: 'SLOW',
        ledColor: 'YELLOW',
        ledDurationSeconds: 10,
        speakerSampleId: null,
        silentAlert: false
      }
    ]
  });
  console.log(`[ResponseConfig] Đã nạp sẵn cấu hình phòng vệ tùy chỉnh (Custom Config) cho Ranger.`);

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
      let alertType = AlertType.ANIMAL_RARE as AlertType;
      let alertTitle = `Phát hiện ${species.displayName}`;
      if (species.isHuman) {
        alertType = AlertType.HUMAN_BORDER as AlertType;
        alertTitle = `Xâm nhập: ${species.displayName}`;
      } else if (species.dangerLevel === DangerLevel.LOW) {
        alertType = AlertType.INTRUDER as AlertType;
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
        if (species.id === 'elephant') {
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
