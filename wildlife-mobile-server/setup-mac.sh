#!/bin/bash

# Script setup môi trường dev tự động trên macOS (M-chip/Intel)
# Tự động điều hướng đến thư mục chứa script
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd "$SCRIPT_DIR" || exit 1

echo "========================================="
echo "BẮT ĐẦU THIẾT LẬP MÔI TRƯỜNG DEV CHO MAC"
echo "========================================="

# 1. Kiểm tra & Cài đặt Homebrew (Trình quản lý gói trên macOS)
if ! command -v brew &> /dev/null; then
    echo "[1/6] Không tìm thấy Homebrew. Đang tiến hành cài đặt Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    
    # Nạp Homebrew vào PATH tạm thời để sử dụng ngay
    if [[ $(uname -m) == "arm64" ]]; then
        eval "$(/opt/homebrew/bin/brew shellenv)"
    else
        eval "$(/usr/local/bin/brew shellenv)"
    fi
else
    echo "[1/6] Homebrew đã được cài đặt sẵn."
fi

# 2. Cài đặt Node.js LTS (v20)
if ! command -v node &> /dev/null; then
    echo "[2/6] Đang cài đặt Node.js v20..."
    brew install node@20
    brew link --overwrite node@20
else
    echo "[2/6] Node.js đã được cài đặt sẵn: $(node -v)"
fi

# 3. Cài đặt Git
if ! command -v git &> /dev/null; then
    echo "[3/6] Đang cài đặt Git..."
    brew install git
else
    echo "[3/6] Git đã được cài đặt sẵn: $(git --version)"
fi

# 4. Cài đặt VS Code
if ! command -v code &> /dev/null; then
    echo "[4/6] Đang cài đặt Visual Studio Code..."
    brew install --cask visual-studio-code
else
    echo "[4/6] VS Code đã được cài đặt sẵn."
fi

# 5. Cài đặt và Khởi tạo PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "[5/6] Đang cài đặt PostgreSQL Server..."
    brew install postgresql@16
    echo "Khởi động dịch vụ PostgreSQL..."
    brew services start postgresql@16
else
    echo "[5/6] PostgreSQL đã được cài đặt sẵn."
    echo "Khởi động dịch vụ PostgreSQL..."
    brew services start postgresql@16 || brew services start postgresql
fi

echo "Đang chờ dịch vụ PostgreSQL sẵn sàng..."
until pg_isready -h localhost -p 5432 &>/dev/null; do
    sleep 1
done
echo "PostgreSQL đã sẵn sàng."

CURRENT_USER=$(whoami)

echo "Đảm bảo role postgres với mật khẩu là 'password' tồn tại..."
psql -h localhost -U "$CURRENT_USER" -d postgres -c "DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'postgres') THEN
    CREATE ROLE postgres WITH SUPERUSER LOGIN PASSWORD 'password';
  ELSE
    ALTER ROLE postgres WITH PASSWORD 'password';
  END IF;
END \$\$;" &>/dev/null || true

# Tạo database nếu chưa tồn tại
if ! psql -h localhost -U "$CURRENT_USER" -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_dev'" | grep -q 1 &>/dev/null; then
    echo "Tạo cơ sở dữ liệu wildlife_dev..."
    psql -h localhost -U "$CURRENT_USER" -d postgres -c "CREATE DATABASE wildlife_dev OWNER postgres;"
fi

if ! psql -h localhost -U "$CURRENT_USER" -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_test'" | grep -q 1 &>/dev/null; then
    echo "Tạo cơ sở dữ liệu wildlife_test..."
    psql -h localhost -U "$CURRENT_USER" -d postgres -c "CREATE DATABASE wildlife_test OWNER postgres;"
fi

# 6. Cấu hình Dự án & Cài đặt Node Modules
echo "[6/6] Đang cài đặt các thư viện Node.js của dự án..."
npm install

# Tạo tệp .env.local và .env.test mẫu nếu chưa tồn tại
if [ ! -f .env.local ]; then
    echo "Tạo file .env.local mẫu..."
    echo 'DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_dev?schema=public"' > .env.local
    echo 'PORT=3000' >> .env.local
    echo 'CLOUDINARY_CLOUD_NAME="dev_cloud"' >> .env.local
    echo 'CLOUDINARY_API_KEY="dev_api_key"' >> .env.local
    echo 'CLOUDINARY_API_SECRET="dev_api_secret"' >> .env.local
    echo 'SMS_API_KEY="dev_sms_key"' >> .env.local
fi

if [ ! -f .env.test ]; then
    echo "Tạo file .env.test mẫu..."
    echo 'DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_test?schema=public"' > .env.test
    echo 'PORT=3000' >> .env.test
    echo 'CLOUDINARY_CLOUD_NAME="test_cloud"' >> .env.test
    echo 'CLOUDINARY_API_KEY="test_api_key"' >> .env.test
    echo 'CLOUDINARY_API_SECRET="test_api_secret"' >> .env.test
    echo 'SMS_API_KEY="test_sms_key"' >> .env.test
fi

# Đẩy schema database lên
echo "Khởi tạo cấu trúc bảng (schema) trên database local..."
npx dotenv-cli -e .env.local -- npx prisma db push

# Sinh static Prisma client
npx prisma generate

# 7. Cài đặt các VS Code Extensions khuyên dùng
echo "-----------------------------------------"
echo "CÀI ĐẶT CÁC TIỆN ÍCH VS CODE BỔ TRỢ..."
if command -v code &> /dev/null; then
    code --install-extension prisma.prisma
    code --install-extension dbaeumer.vscode-eslint
    code --install-extension esbenp.prettier-vscode
    code --install-extension rangav.vscode-thunder-client
    echo "Đã cài xong các VS Code Extensions."
else
    echo "Lưu ý: Không thể cài tự động extension do lệnh 'code' chưa được nạp vào PATH."
    echo "Vui lòng mở VS Code, nhấn Cmd+Shift+P và chọn 'Shell Command: Install \'code\' command in PATH' rồi chạy lại script."
fi

echo "========================================="
echo "THIẾT LẬP HOÀN TẤT!"
echo "Cách chạy dev server: npm run dev"
echo "========================================="
