# Hướng dẫn Tích hợp và Sử dụng Dịch vụ Upload Ảnh lên Cloudinary (Node.js)

Tài liệu này hướng dẫn cách xây dựng dịch vụ tải hình ảnh (upload image service) sử dụng Cloudinary SDK trên nền tảng Node.js phục vụ cho việc lưu trữ ảnh snapshot sự kiện cảnh báo động vật hoang dã.

---

## 1. Tổng quan các tham số xác thực (Credentials)

Để kết nối tới Cloudinary, bạn cần 3 biến môi trường cấu hình xác thực dưới đây. Các giá trị này được lấy từ trang quản trị của Cloudinary sau khi đăng ký tài khoản.

| Biến môi trường | Ý nghĩa | Cách lấy trên Dashboard Cloudinary |
|---|---|---|
| `CLOUDINARY_CLOUD_NAME` | Tên định danh vùng chứa dữ liệu (cloud name) của bạn trên Cloudinary. | Hiển thị ở phần **Account Details** dưới dạng: `Cloud name: xxxxx` |
| `CLOUDINARY_API_KEY` | Khóa công khai (Public Key) dùng để định danh ứng dụng khi gửi yêu cầu gọi API. | Hiển thị ở phần **Account Details** dưới dạng: `API Key: 123456789012345` |
| `CLOUDINARY_API_SECRET` | Khóa bí mật (Private Key) dùng để ký và xác thực yêu cầu. **Tuyệt đối bảo mật ở phía Server.** | Bị ẩn mặc định, nhấn biểu tượng "Reveal" (con mắt) để copy giá trị. |

> [!CAUTION]
> Tuyệt đối không nhúng `CLOUDINARY_API_SECRET` vào mã nguồn của Frontend (Mobile App / React Web) vì người dùng có thể trích xuất ra và chiếm đoạt toàn bộ quyền quản trị không gian lưu trữ của bạn.

---

## 2. Cài đặt và Cấu hình dự án

### Cài đặt thư viện
Chạy lệnh cài đặt SDK chính thức của Cloudinary và thư viện đọc biến môi trường `.env`:
```bash
npm install cloudinary dotenv
```

### Cấu hình biến môi trường
Tạo tệp `.env` tại thư mục gốc của Backend Server (đảm bảo tệp này đã được thêm vào `.gitignore` để tránh bị đẩy lên GitHub công khai):
```env
CLOUDINARY_CLOUD_NAME=your_cloud_name_here
CLOUDINARY_API_KEY=your_api_key_here
CLOUDINARY_API_SECRET=your_api_secret_here
```

---

## 3. Mã nguồn Dịch vụ lưu trữ (`uploadCloudinary.js`)

Viết module xử lý upload ảnh lên Cloudinary sử dụng v2 SDK:

```javascript
require('dotenv').config();
const cloudinary = require('cloudinary').v2;

// Cấu hình Cloudinary từ biến môi trường
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

/**
 * Upload ảnh lên Cloudinary
 * @param {string} filePath - Đường dẫn file ảnh cục bộ (local path), URL ảnh từ xa, hoặc chuỗi Base64
 * @param {object} options - Tùy chọn mở rộng (folder, public_id, ...)
 * @returns {Promise<object>} Đối tượng thông tin ảnh sau khi upload thành công (bao gồm secure_url)
 */
async function uploadImage(filePath, options = {}) {
  try {
    const result = await cloudinary.uploader.upload(filePath, {
      folder: options.folder || 'wildlife_detections',
      public_id: options.public_id, // Tùy chọn định danh file, để trống hệ thống tự sinh
      resource_type: 'image',
      ...options,
    });

    return {
      url: result.secure_url,
      publicId: result.public_id,
      width: result.width,
      height: result.height,
      format: result.format,
    };
  } catch (error) {
    console.error('Upload Cloudinary thất bại:', error.message);
    throw error;
  }
}

module.exports = { uploadImage, cloudinary };
```

---

## 4. Cách thức tích hợp

### Cách dùng cơ bản (File cục bộ / Test Script)
```javascript
const { uploadImage } = require('./uploadCloudinary');

(async () => {
  try {
    // Tải ảnh từ thư mục cục bộ lên thư mục 'events_test'
    const result = await uploadImage('./snapshots/test-image.jpg', { folder: 'events_test' });
    console.log('Upload thành công! URL ảnh:', result.url);
  } catch (error) {
    console.error('Lỗi thực thi:', error);
  }
})();
```

### Tích hợp với Web Server Express (Sử dụng Multer nhận ảnh tải lên từ client)
Dưới đây là API Endpoint mẫu nhận ảnh snapshot gửi lên từ thiết bị hoặc app di động và chuyển tiếp lưu trữ lên Cloudinary:

```javascript
const express = require('express');
const multer = require('multer');
const fs = require('fs');
const { uploadImage } = require('./uploadCloudinary');

const app = express();
const upload = multer({ dest: 'tmp/' }); // Lưu trữ tạm thời file ảnh ở local trước khi tải lên Cloudinary

app.post('/api/upload', upload.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Không tìm thấy file ảnh tải lên.' });
    }

    // Tải tệp tạm lên Cloudinary
    const result = await uploadImage(req.file.path, { folder: 'event_snapshots' });

    // Dọn dẹp tệp lưu tạm trên ổ đĩa local sau khi hoàn thành
    fs.unlink(req.file.path, (err) => {
      if (err) console.error('Lỗi khi xóa tệp tạm:', err);
    });

    // Trả kết quả chứa đường dẫn secure URL về client
    return res.status(201).json(result);
  } catch (error) {
    // Đảm bảo dọn dẹp file tạm nếu xảy ra lỗi trong quá trình upload
    if (req.file && fs.existsSync(req.file.path)) {
      fs.unlinkSync(req.file.path);
    }
    return res.status(500).json({ error: error.message });
  }
});
```

---

## 5. Một số lưu ý kỹ thuật quan trọng

1.  **Tính đa năng của input path:** Hàm `cloudinary.uploader.upload()` có thể tự động nhận diện tham số đầu vào `filePath` là đường dẫn local, link URL internet của ảnh hoặc chuỗi dữ liệu nhị phân Base64 mà không cần viết các hàm phân tách logic.
2.  **Dọn dẹp bộ nhớ đệm (Temporary Files):** Khi sử dụng `multer` hoặc các middleware ghi file tạm ra ổ đĩa local của web server, hãy luôn thực thi xóa file tạm (`fs.unlink`) ở cả block chạy thành công và catch lỗi để tránh hiện tượng rò rỉ dung lượng ổ đĩa (Disk space leakage).
3.  **Cấu hình tối giản bằng CLOUDINARY_URL:** Ngoài cách truyền 3 tham số cấu hình riêng lẻ, Cloudinary SDK hỗ trợ tự động nhận diện biến môi trường duy nhất là `CLOUDINARY_URL` có định dạng:
    ```
    CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME
    ```
    Nếu biến này đã được cài đặt trong môi trường chạy, SDK sẽ tự liên kết mà không cần gọi `cloudinary.config()`.
