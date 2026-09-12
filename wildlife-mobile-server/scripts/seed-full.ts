import { PrismaClient, Role, CameraStatus, DangerLevel, SmsRelation, AlertType } from '@prisma/client';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

// Unsplash high quality sample images for species
const SPECIES_IMAGES: Record<string, string> = {
  elephant: 'https://images.unsplash.com/photo-1557050543-4d5f4e07ef46?w=800&q=80',
  tiger: 'https://images.unsplash.com/photo-1561731216-c3a4d99437d5?w=800&q=80',
  monkey: 'https://images.unsplash.com/photo-1540573133985-780688d1728d?w=800&q=80',
  human: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80',
  crocodile: 'https://images.unsplash.com/photo-1516467508483-a7212febe31a?w=800&q=80',
  giraffe: 'https://images.unsplash.com/photo-1547721064-da6cfb341d50?w=800&q=80',
  leopard: 'https://images.unsplash.com/photo-1456926631375-92c8ce872def?w=800&q=80',
  rhino: 'https://images.unsplash.com/photo-1534177616072-ef7dc120449d?w=800&q=80',
  snake: 'https://images.unsplash.com/photo-1531386151447-fd76ad50012f?w=800&q=80',
  deer: 'https://images.unsplash.com/photo-1484406566174-9da000fda645?w=800&q=80',
  fish: 'https://images.unsplash.com/photo-1522069169874-c58ec4b76be5?w=800&q=80',
  dog: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=800&q=80'
};

async function main() {
  console.log('=== BẮT ĐẦU SEED BỘ DỮ LIỆU ĐẦY ĐỦ (SEED FULL) CHO HỆ THỐNG ===');

  // ---------------------------------------------------------------------------
  // 1. Tạo tài khoản Kiểm lâm Ranger Demo & SMS Recipients
  // ---------------------------------------------------------------------------
  const rUsername = 'ranger_demo';
  const rPhone = '+84900000001';
  const hashedPassword = bcrypt.hashSync('DemoPassword123!', 10);

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
      id: 'u_rg',
      username: rUsername,
      passwordHash: hashedPassword,
      fullName: 'Trần Văn Kiểm Lâm',
      phoneNumber: rPhone,
      role: Role.RANGER
    }
  });
  console.log(`[1/6] [User] Đã nạp tài khoản Kiểm lâm: ${ranger.username} (ID: ${ranger.id})`);

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
  console.log(`[1/6] [SMS] Đã đăng ký 2 số điện thoại phụ nhận cảnh báo khẩn cấp.`);

  // ---------------------------------------------------------------------------
  // 2. Tạo danh mục Loài động vật (Species)
  // ---------------------------------------------------------------------------
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
    },
    {
      id: 'fish',
      displayName: 'Cá',
      dangerLevel: DangerLevel.LOW,
      isHuman: false,
      htmlDescription: '<p>Các loài cá di chuyển theo đàn gần khu vực lòng hồ thủy điện hoặc sông ngòi biên giới.</p>',
      aggressionLevel: 5,
      recommendAction: 'Hệ thống chỉ ghi nhận thông tin theo dõi hệ sinh thái thủy sản và không bật các chế độ xua đuổi.'
    },
    {
      id: 'dog',
      displayName: 'Chó',
      dangerLevel: DangerLevel.MEDIUM,
      isHuman: false,
      htmlDescription: '<p>Chó nhà hoặc chó săn di chuyển gần khu vực nương rẫy, nhà dân.</p>',
      aggressionLevel: 35,
      recommendAction: 'Cảnh báo nhẹ, sử dụng còi hú vừa phải hoặc hệ thống đèn chớp vàng để xua đuổi.'
    }
  ];

  const validSpeciesIds = speciesList.map(s => s.id);
  await prisma.species.deleteMany({
    where: { id: { notIn: validSpeciesIds } }
  });

  for (const s of speciesList) {
    await prisma.species.upsert({
      where: { id: s.id },
      update: s,
      create: s
    });
  }
  console.log(`[2/6] [Species] Đã nạp 12 loài động vật chuẩn.`);

  // ---------------------------------------------------------------------------
  // 3. Tạo các Trạm Camera (Camera)
  // ---------------------------------------------------------------------------
  await prisma.camera.deleteMany({});
  const cameras = [
    {
      id: 'camera_01',
      name: 'trạm 01',
      latitude: 14.3496,
      longitude: 108.0062,
      address: 'Xã Đăk Rơ Wa, Thành phố Kon Tum, Kon Tum',
      status: CameraStatus.ONLINE,
      liveFeedUrl: 'https://www.w3schools.com/html/mov_bbb.mp4'
    },
    {
      id: 'camera_02',
      name: 'trạm 02',
      latitude: 12.8764,
      longitude: 107.7289,
      address: 'Huyện Buôn Đôn, Đắk Lắk',
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
  console.log(`[3/6] [Camera] Đã nạp 2 trạm camera giám sát.`);

  // ---------------------------------------------------------------------------
  // 4. Cấu hình phòng vệ tùy chỉnh (ResponseConfig)
  // ---------------------------------------------------------------------------
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
        silentAlert: true
      },
      {
        userId: ranger.id,
        speciesId: 'human',
        lastModifiedBy: ranger.id,
        audioSampleId: 'A_explosion',
        audioIntensity: 100,
        ledFlashRate: 'FAST',
        ledColor: 'STROBE',
        ledDurationSeconds: 30,
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
        silentAlert: false
      }
    ]
  });
  console.log(`[4/6] [ResponseConfig] Đã nạp các kịch bản cấu hình phòng vệ.`);

  // ---------------------------------------------------------------------------
  // 5. Dọn dẹp sạch toàn bộ lịch sử Sự kiện & Cảnh báo cũ
  // ---------------------------------------------------------------------------
  console.log('[5/6] [Cleanup] Dọn dẹp sạch các sự kiện cũ trước khi sinh dữ liệu phong phú...');
  await prisma.alertRead.deleteMany({});
  await prisma.alert.deleteMany({});
  await prisma.deviceLog.deleteMany({});
  await prisma.eventDetection.deleteMany({});
  await prisma.snapshot.deleteMany({});
  await prisma.event.deleteMany({});

  // ---------------------------------------------------------------------------
  // 6. Sinh Dữ Liệu Giả Phong Phú (Mô phỏng 40 Sự kiện trong 30 ngày qua)
  // ---------------------------------------------------------------------------
  console.log('[6/6] [Rich Seed] Bắt đầu sinh 40 sự kiện phát hiện thú, cảnh báo & nhật ký thiết bị...');

  const now = new Date();
  
  // Danh sách kịch bản mẫu cho các sự kiện ngẫu nhiên
  const eventTemplates = [
    { speciesId: 'elephant', dangerLevel: DangerLevel.CRITICAL, alertType: AlertType.ANIMAL_RARE, title: 'Voi châu Á xuất hiện gần nương rẫy dân cư' },
    { speciesId: 'tiger', dangerLevel: DangerLevel.CRITICAL, alertType: AlertType.ANIMAL_RARE, title: 'Hổ Đông Dương di chuyển ven cửa rừng' },
    { speciesId: 'human', dangerLevel: DangerLevel.HIGH, alertType: AlertType.INTRUDER, title: 'Người lạ xâm nhập khu vực bảo tồn nghiêm ngặt' },
    { speciesId: 'leopard', dangerLevel: DangerLevel.CRITICAL, alertType: AlertType.ANIMAL_RARE, title: 'Báo hoa mai đi săn đêm gần trạm kiểm lâm' },
    { speciesId: 'monkey', dangerLevel: DangerLevel.LOW, alertType: null, title: null },
    { speciesId: 'deer', dangerLevel: DangerLevel.MEDIUM, alertType: AlertType.HIGHWAY_NEARBY, title: 'Nai vàng di chuyển gần đường giao thông' },
    { speciesId: 'crocodile', dangerLevel: DangerLevel.HIGH, alertType: AlertType.ANIMAL_RARE, title: 'Cá sấu xuất hiện ở lòng sông ven biên giới' },
    { speciesId: 'rhino', dangerLevel: DangerLevel.CRITICAL, alertType: AlertType.ANIMAL_RARE, title: 'Tê giác 1 sừng quý hiếm di chuyển qua trạm' },
    { speciesId: 'snake', dangerLevel: DangerLevel.MEDIUM, alertType: null, title: null },
    { speciesId: 'dog', dangerLevel: DangerLevel.MEDIUM, alertType: null, title: null }
  ];

  let totalEvents = 0;
  let totalAlerts = 0;
  let totalLogs = 0;
  let totalSnapshots = 0;

  // Tạo 40 sự kiện trải dài 30 ngày vừa qua
  for (let i = 40; i >= 1; i--) {
    // Thời gian lùi dần về quá khứ (khoảng cách ngẫu nhiên từ 12 giờ đến 20 giờ mỗi sự kiện)
    const hoursAgo = i * 18 - Math.floor(Math.random() * 6);
    const eventTime = new Date(now.getTime() - hoursAgo * 3600 * 1000);

    const camId = (i % 2 === 0) ? 'camera_01' : 'camera_02';
    const template = eventTemplates[i % eventTemplates.length];
    const eventId = `evt_${eventTime.toISOString().slice(0, 10).replace(/-/g, '')}_${String(i).padStart(3, '0')}`;
    const imgUrl = SPECIES_IMAGES[template.speciesId] || SPECIES_IMAGES.elephant;

    // A. Tạo Event
    await prisma.event.create({
      data: {
        id: eventId,
        cameraId: camId,
        detectedAt: eventTime,
        snapshotUrl: imgUrl
      }
    });
    totalEvents++;

    // B. Tạo EventDetection
    const confidence = parseFloat((0.80 + Math.random() * 0.18).toFixed(2)); // 0.80 - 0.98
    await prisma.eventDetection.create({
      data: {
        eventId: eventId,
        speciesId: template.speciesId,
        confidence: confidence,
        detectedAt: eventTime
      }
    });

    // C. Tạo DeviceLog (nếu thú nguy hiểm MEDIUM, HIGH, CRITICAL)
    if (template.dangerLevel !== DangerLevel.LOW) {
      const logAction = template.dangerLevel === DangerLevel.CRITICAL ? 'SIREN_ON' : 'LED_FLASH';
      await prisma.deviceLog.create({
        data: {
          eventId: eventId,
          deviceKey: template.dangerLevel === DangerLevel.CRITICAL ? 'SIREN' : 'LED',
          action: logAction,
          actionAt: eventTime,
          autoOffAt: new Date(eventTime.getTime() + 15 * 1000)
        }
      });
      totalLogs++;
    }

    // D. Tạo Alert (nếu có alertType & title)
    if (template.alertType && template.title) {
      const alertId = `alt_${eventTime.toISOString().slice(0, 10).replace(/-/g, '')}_${String(i).padStart(3, '0')}`;
      const alert = await prisma.alert.create({
        data: {
          id: alertId,
          type: template.alertType,
          title: template.title,
          dangerLevel: template.dangerLevel,
          cameraId: camId,
          eventId: eventId,
          createdAt: eventTime
        }
      });
      totalAlerts++;

      // E. Đánh dấu đã đọc cho ~60% alert cũ, chừa 40% alert mới (đặc biệt các alert gần đây) chưa đọc
      if (i > 10 && i % 3 !== 0) {
        await prisma.alertRead.create({
          data: {
            userId: ranger.id,
            alertId: alert.id,
            readAt: new Date(eventTime.getTime() + 5 * 60 * 1000)
          }
        });
      }
    }

    // F. Tạo Snapshot tải lên bởi Kiểm lâm cho 1 số event tiêu biểu
    if (i % 3 === 0) {
      await prisma.snapshot.create({
        data: {
          cameraId: camId,
          userId: ranger.id,
          url: imgUrl,
          uploadedAt: eventTime
        }
      });
      totalSnapshots++;
    }
  }

  console.log(`[Summary] Hoàn tất sinh dữ liệu:`);
  console.log(` - 🎯 Sự kiện (Event & EventDetection): ${totalEvents} bản ghi`);
  console.log(` - 🚨 Tin Cảnh báo (Alert): ${totalAlerts} bản ghi`);
  console.log(` - ⚡ Nhật ký Thiết bị (DeviceLog): ${totalLogs} bản ghi`);
  console.log(` - 📸 Ảnh Snapshot thực địa: ${totalSnapshots} bản ghi`);
  console.log('=== HOÀN TẤT SEED FULL DỮ LIỆU THÀNH CÔNG ===');
}

main()
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (e) => {
    console.error('Lỗi khi chạy seed-full:', e);
    await prisma.$disconnect();
    process.exit(1);
  });
