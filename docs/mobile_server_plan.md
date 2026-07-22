# Kế hoạch & Kiến trúc triển khai Mobile Server (Express.js + TypeScript)

Tài liệu này phác thảo kế hoạch xây dựng, cấu trúc thư mục, kiến trúc server, cơ chế bảo mật và chiến lược kiểm thử tự động (Automation Test) cho Mobile Server của dự án Wildlife Warning and Deterrence System.

---

## 1. Công nghệ áp dụng & Môi trường triển khai

*   **Ngôn ngữ & Framework:** Node.js + Express.js viết bằng **TypeScript** toàn phần.
*   **Database ORM:** **Prisma ORM** (kết nối PostgreSQL). Prisma giúp học sinh dễ dàng tương tác với DB thông qua các hàm Javascript/TypeScript có sẵn gợi ý code (IntelliSense) mà không cần viết các câu lệnh SQL raw phức tạp.
*   **Môi trường chạy cục bộ:** `tsx` hoặc `ts-node-dev` để chạy trực tiếp file `.ts`.
*   **Môi trường triển khai:** **Vercel Serverless Functions**. Express App sẽ được đóng gói và cấu hình để chạy trực tiếp trên hạ tầng Serverless của Vercel mà không cần thuê VPS riêng.

---

## 2. Hướng dẫn thiết lập môi trường phát triển (Dev Environment Setup)

Để học sinh và AI có thể thiết lập, phát triển và chạy thử ứng dụng một cách nhanh chóng trên máy tính cá nhân, dưới đây là bảng hướng dẫn cài đặt nhanh các công cụ cần thiết cho cả **macOS Apple Silicon (M-chip)** và **Windows 11**:

| Tên công cụ | Vai trò | Mục đích sử dụng | Cách cài nhanh trên macOS (chip M) | Cách cài nhanh trên Windows 11 |
|---|---|---|---|---|
| **Node.js & npm** | Bắt buộc | Môi trường nền tảng để chạy và biên dịch mã nguồn Backend. | Mở Terminal chạy:<br>`brew install node@20` | Tải trình cài đặt `.msi` bản LTS từ [nodejs.org](https://nodejs.org) và chạy. |
| **Git** | Bắt buộc | Quản lý mã nguồn và kết nối tự động để deploy lên Vercel. | Mở Terminal chạy:<br>`git --version`<br>(Nhấp *Install* khi hộp thoại hiện ra) | Tải bản cài đặt từ [git-scm.com](https://git-scm.com) (tích hợp kèm *Git Bash*). |
| **VS Code** | Bắt buộc | Trình soạn thái viết code chính, hỗ trợ debug và tự báo lỗi cú pháp. | Tải bản cài đặt Mac (Apple Silicon) từ [code.visualstudio.com](https://code.visualstudio.com). | Tải bản cài đặt Windows từ [code.visualstudio.com](https://code.visualstudio.com) và chạy. |
| **Prisma Studio** | Bắt buộc | Giao diện Web xem dữ liệu trực quan như Excel để quản lý database. | Không cần cài đặt phần mềm ngoài.<br>Chạy lệnh ở Terminal:<br>`npx prisma studio` | Không cần cài đặt phần mềm ngoài.<br>Chạy lệnh ở Terminal/cmd:<br>`npx prisma studio` |
| **PostgreSQL** | Bắt buộc | Hệ quản trị cơ sở dữ liệu lưu trữ để phát triển và chạy test tự động. | Tải ứng dụng **Postgres App** từ [postgresapp.com](https://postgresapp.com) (hoặc chạy: `brew install postgresql`). | Tải trình cài đặt từ trang chủ [postgresql.org](https://www.postgresql.org/download/windows/) (chọn bản EDB PostgreSQL 16) và cài đặt. |

### Các tiện ích mở rộng (Extensions) khuyên dùng trong VS Code:
Để học sinh viết code dễ dàng và hạn chế tối đa lỗi cú pháp, hãy cài thêm các tiện ích mở rộng sau trực tiếp trong VS Code (mục Extensions):
*   **Prisma:** Highlight màu sắc cú pháp và tự động format file `schema.prisma`.
*   **ESLint:** Cảnh báo lỗi cú pháp TypeScript thời gian thực ngay khi gõ code.
*   **Prettier - Code Formatter:** Tự động căn lề và định dạng code đẹp mắt khi lưu file (`Ctrl + S` hoặc `Cmd + S`).
*   **Thunder Client:** Extension kiểm thử API trực tiếp siêu nhẹ (thay thế cho Postman).

---

## 3. Cấu trúc thư mục đơn giản (Dành cho học sinh)

Để giúp học sinh dễ đọc, dễ tiếp cận và hiểu luồng dữ liệu, cấu trúc code sẽ được thiết kế theo hướng phẳng, tập trung, **tránh chia quá nhỏ** file dẫn đến việc trace code khó khăn:

```text
wildlife-mobile-server/
├── prisma/
│   └── schema.prisma          # Định nghĩa cấu trúc Database (PostgreSQL)
├── src/
│   ├── config/
│   │   └── cloudinary.ts      # Cấu hình lưu trữ ảnh Cloudinary
│   ├── middlewares/
│   │   └── auth.ts            # Middleware xác thực token đơn giản
│   ├── controllers/
│   │   ├── authController.ts  # Đăng ký, đăng nhập, đăng xuất
│   │   ├── cameraController.ts# Quản lý camera, thiết bị ngoại vi, test loa
│   │   ├── eventController.ts # Nhật ký sự kiện, nhận dạng AI
│   │   ├── configController.ts# Cấu hình phòng vệ species/camera
│   │   ├── statsController.ts # Thống kê tổng hợp (summary, trend, heatmap)
│   │   └── smsController.ts   # Quản lý SĐT nhận cảnh báo SMS
│   ├── app.ts                 # Khởi tạo Express app (dành cho Vercel & local)
│   └── index.ts               # Entrypoint chạy local server (port 3000)
├── tests/                     # Thư mục chứa các tệp kiểm thử tự động
│   ├── auth.test.ts
│   ├── camera.test.ts
│   ├── event.test.ts
│   └── helper.ts              # Tạo dữ liệu giả lập cho test
├── package.json
├── tsconfig.json              # Cấu hình TypeScript compiler
└── vercel.json                # Cấu hình định tuyến Serverless cho Vercel
```

---

## 3. Kiến trúc Vercel Serverless Integration

Để chạy ứng dụng Express trên Vercel Serverless, tệp `vercel.json` ở thư mục gốc sẽ định tuyến tất cả các request đến file `src/app.ts`.

### Cấu hình `vercel.json`
```json
{
  "version": 2,
  "builds": [
    {
      "src": "src/app.ts",
      "use": "@vercel/node"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "src/app.ts"
    }
  ]
}
```

### Điểm khởi chạy Vercel/Local (`src/app.ts`)
```typescript
import express from 'express';
import cors from 'cors';
// Import các routes khác...

const app = express();

app.use(cors());
app.use(express.json());

// Đăng ký các endpoints tập trung
// app.use('/api/auth', authRouter);
// app.use('/api/cameras', cameraRouter);

// Export app để Vercel Serverless Engine import và xử lý
export default app;
```

---

## 4. Cơ chế Bảo mật cơ bản (Security ở mức cơ bản)

Do đây là dự án nghiên cứu khoa học cấp học sinh, các cơ chế bảo mật sẽ tập trung vào sự đơn giản, trực quan nhưng vẫn đảm bảo tính an toàn cơ bản:

1.  **Xác thực Token vĩnh viễn (Static Bearer Token):**
    *   Không sử dụng cơ chế Refresh Token phức tạp. Khi người dùng đăng nhập thành công, Server sinh ra một chuỗi token ngẫu nhiên lưu vào cột `token` của thiết bị đăng ký và trả về Client.
    *   Client lưu trữ token này trong bộ nhớ máy (`SharedPreferences` trên Android) và gửi kèm lên Header HTTP dưới dạng `Authorization: Bearer <token>` trong các request tiếp theo.
2.  **Chống SQL Injection:**
    *   Sử dụng Prisma ORM. Mặc định Prisma thực thi tất cả các câu lệnh truy vấn dưới dạng **Parameterized Queries** giúp ngăn chặn triệt để lỗ hổng tấn công SQL Injection mà học sinh không cần xử lý thủ công.
3.  **Quản lý biến bảo mật (Environment Variables):**
    *   Các thông tin nhạy cảm như `DATABASE_URL`, `CLOUDINARY_API_SECRET`, `SMS_API_KEY` được lưu hoàn toàn trong `.env` ở local và cấu hình trong trang quản trị Vercel Environment Variables khi deploy lên cloud.

---

## 5. Chiến lược Automation Test cho AI & Học sinh

Để hỗ trợ AI có thể tự động chạy test, phân tích log lỗi và sửa code liên tục cho đến khi đạt độ phủ 100% testcase mà không cần sự can thiệp thủ công:

*   **Thư viện Test:** Sử dụng **Jest** kết hợp với **Supertest** (để giả lập request HTTP lên Express app mà không cần chạy server thực tế).
*   **Database cho Test:** Để tránh làm bẩn database PostgreSQL phát triển chính (`wildlife_dev`), chúng ta sẽ cấu hình Prisma sử dụng database **PostgreSQL Test độc lập (`wildlife_test`)** chạy ngay trên môi trường local:
    *   Tự động chạy `npx prisma db push` trước khi chạy test để đồng bộ cấu trúc bảng và trigger lên database test.
    *   Trước và sau mỗi file test, hệ thống tự động xóa sạch dữ liệu cũ (`deleteMany`) để đảm bảo tính độc lập giữa các testcase.
    *   *Ưu điểm:* Test chạy trên PostgreSQL cục bộ thực tế có tốc độ phản hồi cực nhanh (dưới 10ms/request) và hỗ trợ hoàn hảo tất cả các trigger DB đặc thù của Postgres mà SQLite không chạy được.

### Ví dụ mã nguồn Test (`tests/auth.test.ts`)
```typescript
import request from 'supertest';
import app from '../src/app';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

beforeAll(async () => {
  // Đồng bộ schema và dọn dẹp dữ liệu cũ trước khi test
  await prisma.user.deleteMany({});
});

describe('POST /api/auth/register', () => {
  it('Đăng ký tài khoản thành công với thông tin hợp lệ', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({
        username: 'ranger_cat_tien',
        password: 'password123',
        fullName: 'Nguyễn Văn Kiểm Lâm',
        phoneNumber: '+84901234567',
        role: 'RANGER'
      });

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.username).toBe('ranger_cat_tien');
  });
});

afterAll(async () => {
  await prisma.$disconnect();
});
```

---

## 6. Cấu hình đa môi trường (Local & Production)

Để đảm bảo quy trình phát triển an toàn và dễ quản lý, hệ thống phân chia rõ ràng thành 2 môi trường hoạt động độc lập:

### 6.1. Môi trường cục bộ (Local Environment)
*   **Mục đích:** Dành cho học sinh lập trình thiết kế tính năng mới, AI tự động kiểm thử và sửa lỗi.
*   **Database:** **PostgreSQL cục bộ (Local PostgreSQL)**.
    *   *Mô tả:* Được cài đặt trực tiếp thông qua trình cài đặt **PostgreSQL Installer** chính thức của EDB (trên Windows) hoặc ứng dụng **Postgres App** (trên Mac).
    *   *Yêu cầu:* Sau khi cài đặt cơ sở dữ liệu thành công, sử dụng công cụ quản lý đi kèm (như pgAdmin) hoặc chạy các câu lệnh SQL để tạo thủ công hai database độc lập:
        1.  `wildlife_dev` (Cơ sở dữ liệu dành cho lập trình phát triển local).
        2.  `wildlife_test` (Cơ sở dữ liệu dành cho kiểm thử tự động).
    *   *Tại sao bắt buộc?* Giúp đồng bộ 100% các tính năng đặc thù (như trigger DB giới hạn 3 SĐT, các hàm thời gian) và đặc biệt là tối ưu hóa tốc độ chạy của **Automation Test** (kiểm thử tự động chạy local siêu nhanh, không phụ thuộc vào kết nối mạng internet).
*   **Cấu hình biến môi trường:** Lưu tại tệp `.env.local` ở thư mục gốc:
    ```env
    DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_dev?schema=public"
    PORT=3000
    CLOUDINARY_CLOUD_NAME="dev_cloud"
    CLOUDINARY_API_KEY="dev_api_key"
    CLOUDINARY_API_SECRET="dev_api_secret"
    SMS_API_KEY="dev_sms_key"
    ```

### 6.2. Môi trường thực tế (Production Environment)
*   **Mục đích:** Chạy thực tế phục vụ báo cáo đề tài và kết nối trực tiếp với ứng dụng Android của người dân/kiểm lâm.
*   **Database:** PostgreSQL Serverless chính thức (được cấp bởi Vercel Postgres, Neon DB hoặc Supabase).
*   **Cấu hình biến môi trường:** Được thiết lập trực tiếp trên **Vercel Dashboard** (Settings -> Environment Variables). Không đẩy tệp `.env` lên Git để bảo mật thông tin xác thực.
    *   `DATABASE_URL`: Đường dẫn kết nối đến DB PostgreSQL Production thực tế (yêu cầu SSL).
    *   Các Key Cloudinary và SMS API Production thật.

### 6.3. Môi trường kiểm thử tự động (Testing Environment)
*   **Mục đích:** Chạy tự động bộ test suite (AI loop test hoặc học sinh chạy test).
*   **Database:** Sử dụng cơ sở dữ liệu **PostgreSQL Test độc lập** nằm ngay trên máy cục bộ (ví dụ: database `wildlife_test`). Việc này giúp kiểm thử chạy cực kỳ nhanh và độc lập hoàn toàn với database phát triển local (`wildlife_dev`), tránh làm bẩn hoặc hỏng dữ liệu đang lập trình.
*   *Cấu hình:* Jest tự động nạp cấu hình từ `.env.test` khi chạy lệnh `npm run test`:
    ```env
    DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_test?schema=public"
    ```

---

## 7. Bộ Scripts tự động hóa (Automation & Build Scripts)

Trong `package.json` sẽ tích hợp sẵn các tập lệnh để học sinh và AI dễ dàng kiểm tra tính đúng đắn của mã nguồn trước khi deploy:

```json
"scripts": {
  "dev": "tsx watch src/index.ts",
  "build": "tsc",
  "lint": "eslint 'src/**/*.ts'",
  "lint:fix": "eslint 'src/**/*.ts' --fix",
  "db:push": "prisma db push",
  "db:studio": "prisma studio",
  "test": "cross-env NODE_ENV=test jest --runInBand --detectOpenHandles",
  "validate": "npm run lint && npm run build && npm run test"
}
```

*   **`npm run dev`**: Chạy Hot-Reload code khi lập trình.
*   **`npm run build`**: Biên dịch toàn bộ code TypeScript sang JavaScript (`/dist`) để xác nhận không có lỗi cú pháp tĩnh (Static Type Errors).
*   **`npm run test`**: Thực thi tự động toàn bộ các kịch bản kiểm thử tích hợp (Integration Tests) của 37 API.
*   **`npm run validate`**: Lệnh gộp kiểm tra toàn diện (Lint -> Build -> Test). Trước khi deploy lên Vercel, bắt buộc lệnh này phải trả về kết quả thành công. AI có thể sử dụng lệnh này để tự đánh giá mức độ hoàn thiện sau mỗi lần fix bug.

---

## 8. Hướng dẫn AI Agent phát triển và kiểm thử tự động (AI Agent Integration Guidelines)

Để các AI Agent (như Cursor, GitHub Copilot, hoặc Gemini Coding Assistant) có thể tự động đọc tài liệu nghiệp vụ bên ngoài thư mục source code, tiến hành sinh mã nguồn (code generation), tự viết và chạy test để fix lỗi lặp (self-healing), chúng ta sẽ thiết lập một tệp chỉ thị AI chuyên biệt.

### 8.1. Vị trí tệp chỉ thị AI
Tạo một tệp tin tên là `AI_INSTRUCTIONS.md` (hoặc cấu hình vào `.cursorrules`) đặt trực tiếp tại **thư mục gốc của source code backend** (`wildlife-mobile-server/AI_INSTRUCTIONS.md`).

### 8.2. Nội dung tệp chỉ thị mẫu (`AI_INSTRUCTIONS.md`)

```markdown
# Chỉ thị dành cho AI Coding Assistant (System Instructions)

Bạn là một trợ lý lập trình AI được giao nhiệm vụ phát triển và kiểm thử tự động cho Mobile Server của dự án này. Hãy đọc kỹ các yêu cầu và quy trình dưới đây trước khi thực hiện bất kỳ hành động nào.

## 1. Yêu cầu đọc tài liệu nghiệp vụ bên ngoài
Trước khi viết mã nguồn hoặc viết testcase, bạn BẮT BUỘC phải đọc toàn bộ nội dung của các tài liệu đặc tả nằm tại thư mục tài liệu chung của dự án (nằm ở thư mục cha `../docs/` tương đối):
*   **Đặc tả API di động:** [03-mobile_api.md](../docs/03-mobile_api.md) (Chứa danh sách 37 API và cấu hình Request/Response mẫu).
*   **Sơ đồ tương tác Sequence:** [04-sequence-diagram.md](../docs/04-sequence-diagram.md) (Chứa luồng tương tác nghiệp vụ chi tiết của 18 Actions).
*   **Thiết kế Database:** [05-database.md](../docs/05-database.md) (Chứa sơ đồ ERD và cấu trúc chi tiết 11 bảng dữ liệu).
*   **Kế hoạch Kiến trúc:** [mobile_server_plan.md](../docs/mobile_server_plan.md) (Chứa kiến trúc tổng quan, thiết lập môi trường và cấu trúc thư mục).

## 2. Quy định cấu trúc viết code (TypeScript)
*   **Không chia nhỏ code:** Tuân thủ cấu trúc thư mục phẳng tại `src/controllers/` và `src/routes/`. Cấm tạo các thư mục con sâu hơn làm phức tạp luồng đọc của học sinh.
*   **Database Schema:** Mọi thay đổi về cấu trúc bảng phải được cập nhật trước vào `prisma/schema.prisma` dựa theo Đặc tả Database và chạy lệnh `npx prisma db push`.
*   **Kiểm tra cú pháp tĩnh:** Sau mỗi lần sinh code, bạn phải chạy lệnh `npm run build` để kiểm tra lỗi cú pháp TypeScript, không được để code lỗi biên dịch.

## 3. Quy trình tự động chạy test và sửa lỗi (Self-Healing Loop)
Khi phát triển một tính năng hoặc API endpoint, bạn phải tuân thủ quy trình vòng lặp tự động sau:
1.  **Viết Testcase:** Tạo tệp test tương ứng trong thư mục `tests/` sử dụng Jest + Supertest giả lập các case thành công và thất bại của API theo đúng đặc tả tài liệu 03.
2.  **Chạy Test:** Thực thi lệnh `npm run test` (chạy trên database PostgreSQL cục bộ độc lập `wildlife_test`).
3.  **Tự sửa lỗi (Fix Loop):**
    *   Nếu có testcase thất bại (`FAIL`), bạn phải tự đọc log lỗi từ terminal đầu ra.
    *   Xác định nguyên nhân (sai cấu trúc response, thiếu validate đầu vào, sai logic DB, v.v.).
    *   Tiến hành sửa mã nguồn trực tiếp tại các file controller liên quan.
    *   Lặp lại việc chạy test (`npm run test`) cho đến khi toàn bộ test suite báo màu xanh (`PASS 100%`).
4.  **Xác thực tổng thể:** Chạy lệnh `npm run validate` để đảm bảo code sạch lỗi cú pháp, compile thành công và pass toàn bộ test trước khi báo cáo hoàn thành cho người dùng.
```

