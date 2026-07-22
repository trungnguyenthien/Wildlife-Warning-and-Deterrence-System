#!/bin/bash

# Script setup môi trường dev tự động trên Windows 11 (Chạy qua Git Bash)
# Tự động điều hướng đến thư mục chứa script
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd "$SCRIPT_DIR" || exit 1

echo "========================================="
echo "BẮT ĐẦU THIẾT LẬP MÔI TRƯỜNG DEV CHO WINDOWS 11"
echo "========================================="

# 1. Cài đặt Node.js LTS (v20) qua winget
if ! command -v node &> /dev/null; then
    echo "[1/5] Đang cài đặt Node.js v20..."
    cmd.exe /c "winget install OpenJS.NodeJS.LTS --silent --accept-source-agreements --accept-package-agreements"
else
    echo "[1/5] Node.js đã được cài đặt sẵn: $(node -v)"
fi

# 2. Cài đặt Git qua winget
if ! command -v git &> /dev/null; then
    echo "[2/5] Đang cài đặt Git..."
    cmd.exe /c "winget install Git.Git --silent --accept-source-agreements --accept-package-agreements"
else
    echo "[2/5] Git đã được cài đặt sẵn: $(git --version)"
fi

# 3. Cài đặt VS Code qua winget
if ! command -v code &> /dev/null; then
    echo "[3/5] Đang cài đặt Visual Studio Code..."
    cmd.exe /c "winget install Microsoft.VisualStudioCode --silent --accept-source-agreements --accept-package-agreements"
else
    echo "[3/5] VS Code đã được cài đặt sẵn."
fi

# 4. Cài đặt và Khởi tạo PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "[4/5] Đang cài đặt PostgreSQL Server..."
    cmd.exe /c "winget install PostgreSQL.PostgreSQL --silent --accept-source-agreements --accept-package-agreements"
    echo "Lưu ý: EDB PostgreSQL Installer đang cài đặt ngầm. Khi hoàn thành, vui lòng làm theo hướng dẫn trên màn hình Windows để cài đặt mật khẩu mặc định là 'password' và cổng mặc định 5432."
else
    echo "[4/5] PostgreSQL đã được cài đặt sẵn."
fi

# Tự động nạp thư mục cài đặt PostgreSQL vào PATH tạm thời để chạy lệnh psql
PG_BIN=""
for v in 16 15 14; do
    if [ -d "/c/Program Files/PostgreSQL/$v/bin" ]; then
        PG_BIN="/c/Program Files/PostgreSQL/$v/bin"
        break
    fi
done

if [ -n "$PG_BIN" ]; then
    export PATH="$PG_BIN:$PATH"
fi

echo "Đang khởi động dịch vụ PostgreSQL..."
cmd.exe /c "net start postgresql-x64-16" &>/dev/null || \
cmd.exe /c "net start postgresql-x64-15" &>/dev/null || \
cmd.exe /c "net start postgresql-x64-14" &>/dev/null || true

echo "Đang chờ dịch vụ PostgreSQL sẵn sàng..."
if command -v pg_isready &> /dev/null; then
    until pg_isready -h localhost -p 5432 &>/dev/null; do
        sleep 1
    done
else
    sleep 5
fi
echo "PostgreSQL đã sẵn sàng."

# Khởi tạo database nếu chưa tồn tại
echo "Khởi tạo cơ sở dữ liệu..."
PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_dev'" | grep -q 1 &>/dev/null || \
PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "CREATE DATABASE wildlife_dev;" &>/dev/null || true

PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_test'" | grep -q 1 &>/dev/null || \
PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "CREATE DATABASE wildlife_test;" &>/dev/null || true

# 5. Cấu hình Dự án & Cài đặt Node Modules
echo "[5/5] Đang cài đặt các thư viện Node.js của dự án..."
# Gọi npm hoặc npm.cmd tùy thuộc vào PATH
if command -v npm &> /dev/null; then
    npm install
elif command -v npm.cmd &> /dev/null; then
    npm.cmd install
else
    echo "Lỗi: Không tìm thấy npm để cài node modules. Hãy khởi động lại terminal và chạy lại script."
fi

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

# Đẩy cấu trúc bảng (schema) lên local db
echo "Khởi tạo cấu trúc bảng (schema) trên database local..."
if command -v npx &> /dev/null; then
    npx dotenv-cli -e .env.local -- npx prisma db push
elif command -v npx.cmd &> /dev/null; then
    npx.cmd dotenv-cli -e .env.local -- npx.cmd prisma db push
fi

# Sinh static Prisma client
if command -v npx &> /dev/null; then
    npx prisma generate
elif command -v npx.cmd &> /dev/null; then
    npx.cmd prisma generate
fi

# 6. Cài đặt các VS Code Extensions khuyên dùng
echo "-----------------------------------------"
echo "CÀI ĐẶT CÁC TIỆN ÍCH VS CODE BỔ TRỢ..."
install_extension() {
    if command -v code &> /dev/null; then
        code --install-extension "$1"
    elif command -v code.cmd &> /dev/null; then
        code.cmd --install-extension "$1"
    fi
}

install_extension "prisma.prisma"
install_extension "dbaeumer.vscode-eslint"
install_extension "esbenp.prettier-vscode"
install_extension "rangav.vscode-thunder-client"
echo "Đã gửi lệnh cài các VS Code Extensions."

echo "========================================="
echo "THIẾT LẬP HOÀN TẤT!"
echo "Vui lòng khởi động lại Git Bash để cập nhật biến môi trường PATH."
echo "Cách chạy dev server: npm run dev"
echo "========================================="
