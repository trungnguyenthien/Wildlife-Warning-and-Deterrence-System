import { v2 as cloudinary } from 'cloudinary';
import dotenv from 'dotenv';

dotenv.config({ path: '.env.local' });
dotenv.config();

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

export { cloudinary };

/**
 * Upload ảnh lên Cloudinary
 * @param filePath Đường dẫn file cục bộ hoặc link ảnh
 * @param folder Thư mục chứa ảnh trên Cloudinary
 */
export async function uploadImage(filePath: string, folder = 'manual_snapshots'): Promise<string> {
  const result = await cloudinary.uploader.upload(filePath, {
    folder,
    resource_type: 'image',
  });
  return result.secure_url;
}
