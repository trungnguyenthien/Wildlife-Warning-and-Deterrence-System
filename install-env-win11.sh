#!/bin/bash

# Script setup môi trường dev tự động trên Windows 11 (Chạy qua Git Bash)
# Bao gồm: Node/Git/VSCode/PostgreSQL (project web) + Android dev environment
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd "$SCRIPT_DIR" || exit 1

set -o pipefail
LOG_FILE="$SCRIPT_DIR/setup-dev-env.log"
: > "$LOG_FILE"
exec > >(tee -a "$LOG_FILE") 2>&1

trap 'echo "[$(date "+%H:%M:%S")] [ERROR] Lỗi tại dòng $LINENO. Xem chi tiết trong $LOG_FILE"' ERR

SCRIPT_START_TS=$(date +%s)
TOTAL_STEPS=13
STEP_COUNTER=0

# ---- Helpers: logging chi tiết ----
_ts() { date "+%H:%M:%S"; }

log_info()  { echo "[$(_ts)] [INFO]  $1"; }
log_warn()  { echo "[$(_ts)] [WARN]  $1"; }
log_error() { echo "[$(_ts)] [ERROR] $1"; }
log_done()  { echo "[$(_ts)] [DONE]  $1"; }
log_skip()  { echo "[$(_ts)] [SKIP]  $1"; }

step_start() {
    STEP_COUNTER=$((STEP_COUNTER + 1))
    STEP_START_TS=$(date +%s)
    echo ""
    echo "[$(_ts)] [STEP $STEP_COUNTER/$TOTAL_STEPS] ▶ $1"
}

step_done() {
    local elapsed=$(( $(date +%s) - STEP_START_TS ))
    echo "[$(_ts)] [STEP $STEP_COUNTER/$TOTAL_STEPS] ✔ $1 (${elapsed}s)"
}

progress_bar() {
    # progress_bar <current> <total>
    local current=$1 total=$2 width=30
    local filled=$(( current * width / total ))
    local empty=$(( width - filled ))
    printf "["
    printf "%0.s#" $(seq 1 $filled) 2>/dev/null
    printf "%0.s-" $(seq 1 $empty) 2>/dev/null
    printf "] %d/%d\n" "$current" "$total"
}

echo "========================================="
echo "BẮT ĐẦU THIẾT LẬP MÔI TRƯỜNG DEV CHO WINDOWS 11"
echo "Thời gian bắt đầu: $(date "+%Y-%m-%d %H:%M:%S")"
echo "Log chi tiết: $LOG_FILE"
echo "========================================="

# =========================================
# PHẦN A: PROJECT WEB (Node.js / Git / VS Code / PostgreSQL)
# =========================================

# A1. Node.js
step_start "Cài đặt Node.js v20"
if ! command -v node &> /dev/null; then
    log_info "Chưa tìm thấy Node.js, đang cài qua winget..."
    cmd.exe /c "winget install OpenJS.NodeJS.LTS --silent --accept-source-agreements --accept-package-agreements"
    log_info "winget install Node.js hoàn tất (exit code $?)"
else
    log_skip "Node.js đã có sẵn: $(node -v)"
fi
step_done "Node.js"

# A2. Git
step_start "Cài đặt Git"
if ! command -v git &> /dev/null; then
    log_info "Chưa tìm thấy Git, đang cài qua winget..."
    cmd.exe /c "winget install Git.Git --silent --accept-source-agreements --accept-package-agreements"
    log_info "winget install Git hoàn tất (exit code $?)"
else
    log_skip "Git đã có sẵn: $(git --version)"
fi
step_done "Git"

# A3. VS Code
step_start "Cài đặt Visual Studio Code"
if ! command -v code &> /dev/null; then
    log_info "Chưa tìm thấy VS Code, đang cài qua winget..."
    cmd.exe /c "winget install Microsoft.VisualStudioCode --silent --accept-source-agreements --accept-package-agreements"
    log_info "winget install VS Code hoàn tất (exit code $?)"
else
    log_skip "VS Code đã có sẵn."
fi
step_done "VS Code"

# A4. PostgreSQL
step_start "Cài đặt & khởi động PostgreSQL"
log_skip "Tạm thời bỏ qua cài đặt PostgreSQL theo yêu cầu của Bệ Hạ."

PG_BIN=""
for v in 16 15 14; do
    if [ -d "/c/Program Files/PostgreSQL/$v/bin" ]; then
        PG_BIN="/c/Program Files/PostgreSQL/$v/bin"
        log_info "Tìm thấy PostgreSQL $v tại $PG_BIN"
        break
    fi
done
[ -n "$PG_BIN" ] && export PATH="$PG_BIN:$PATH"

if command -v psql &> /dev/null; then
    log_info "Đang khởi động dịch vụ PostgreSQL..."
    cmd.exe /c "net start postgresql-x64-16" &>/dev/null || \
    cmd.exe /c "net start postgresql-x64-15" &>/dev/null || \
    cmd.exe /c "net start postgresql-x64-14" &>/dev/null || log_warn "Không khởi động được service qua 'net start' (có thể đã chạy sẵn hoặc thiếu quyền admin)."

    log_info "Đang chờ dịch vụ PostgreSQL sẵn sàng..."
    if command -v pg_isready &> /dev/null; then
        RETRY=0
        until pg_isready -h localhost -p 5432 &>/dev/null; do
            RETRY=$((RETRY + 1))
            log_info "  ...chờ PostgreSQL (lần $RETRY)"
            sleep 1
            if [ "$RETRY" -ge 30 ]; then
                log_warn "PostgreSQL chưa sẵn sàng sau 30s, tiếp tục nhưng bước tạo DB có thể lỗi."
                break
            fi
        done
    else
        sleep 5
    fi
    log_info "Khởi tạo database (wildlife_dev, wildlife_test) nếu chưa tồn tại..."
    PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_dev'" | grep -q 1 &>/dev/null || \
    PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "CREATE DATABASE wildlife_dev;" &>/dev/null && log_info "Đã tạo database wildlife_dev." || log_skip "wildlife_dev đã tồn tại."

    PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'wildlife_test'" | grep -q 1 &>/dev/null || \
    PGPASSWORD=password psql -h localhost -U postgres -d postgres -c "CREATE DATABASE wildlife_test;" &>/dev/null && log_info "Đã tạo database wildlife_test." || log_skip "wildlife_test đã tồn tại."
else
    log_skip "Chưa cài đặt psql. Bỏ qua khởi động dịch vụ và tạo database."
fi
step_done "PostgreSQL"

# A5. Node modules + env files + prisma
step_start "Cài node_modules, tạo file .env, khởi tạo Prisma"
cd "$SCRIPT_DIR/wildlife-mobile-server" || exit 1
log_info "Đang chạy npm install (có thể mất vài phút)..."
if command -v npm &> /dev/null; then
    npm install
elif command -v npm.cmd &> /dev/null; then
    npm.cmd install
else
    log_error "Không tìm thấy npm. Hãy khởi động lại terminal rồi chạy lại script."
fi
log_info "npm install xong."

if [ ! -f .env.local ]; then
    log_info "Tạo file .env.local mẫu..."
    {
        echo 'DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_dev?schema=public"'
        echo 'PORT=3000'
        echo 'CLOUDINARY_CLOUD_NAME="dev_cloud"'
        echo 'CLOUDINARY_API_KEY="dev_api_key"'
        echo 'CLOUDINARY_API_SECRET="dev_api_secret"'
        echo 'SMS_API_KEY="dev_sms_key"'
    } > .env.local
else
    log_skip ".env.local đã tồn tại."
fi

if [ ! -f .env.test ]; then
    log_info "Tạo file .env.test mẫu..."
    {
        echo 'DATABASE_URL="postgresql://postgres:password@localhost:5432/wildlife_test?schema=public"'
        echo 'PORT=3000'
        echo 'CLOUDINARY_CLOUD_NAME="test_cloud"'
        echo 'CLOUDINARY_API_KEY="test_api_key"'
        echo 'CLOUDINARY_API_SECRET="test_api_secret"'
        echo 'SMS_API_KEY="test_sms_key"'
    } > .env.test
else
    log_skip ".env.test đã tồn tại."
fi

log_info "Đẩy schema Prisma lên database local (db push)..."
if command -v psql &> /dev/null; then
    if command -v npx &> /dev/null; then
        npx dotenv-cli -e .env.local -- npx prisma db push
    elif command -v npx.cmd &> /dev/null; then
        npx.cmd dotenv-cli -e .env.local -- npx.cmd prisma db push
    fi
else
    log_warn "Không tìm thấy psql. Bỏ qua bước đẩy schema Prisma lên database local (db push)."
fi

log_info "Sinh Prisma client (generate)..."
if command -v npx &> /dev/null; then
    npx prisma generate
elif command -v npx.cmd &> /dev/null; then
    npx.cmd prisma generate
fi
step_done "Node modules / env / Prisma"
cd "$SCRIPT_DIR" || exit 1

# A6. VS Code extensions
step_start "Cài VS Code extensions"
install_extension() {
    log_info "  Cài extension: $1"
    if command -v code &> /dev/null; then
        code --install-extension "$1"
    elif command -v code.cmd &> /dev/null; then
        code.cmd --install-extension "$1"
    fi
}
EXT_LIST=("prisma.prisma" "dbaeumer.vscode-eslint" "esbenp.prettier-vscode" "rangav.vscode-thunder-client")
i=0
for ext in "${EXT_LIST[@]}"; do
    i=$((i + 1))
    install_extension "$ext"
    progress_bar "$i" "${#EXT_LIST[@]}"
done
step_done "VS Code extensions"

# =========================================
# PHẦN B: MÔI TRƯỜNG ANDROID DEV
# =========================================

echo ""
echo "========================================="
echo "THIẾT LẬP MÔI TRƯỜNG ANDROID DEV"
echo "========================================="

to_win_path() {
    cygpath -w "$1" 2>/dev/null || echo "$1" | sed 's#^/c/#C:\\#; s#/#\\#g'
}

setx_if_changed() {
    local name="$1" value="$2" current
    current=$(cmd.exe /c "echo %$name%" 2>/dev/null | tr -d '\r')
    if [ "$current" != "$value" ]; then
        log_info "Set biến môi trường $name = $value"
        cmd.exe /c "setx $name \"$value\"" >/dev/null
    else
        log_skip "$name đã đúng giá trị, bỏ qua."
    fi
}

# B1. JDK 17
step_start "Cài đặt JDK 17 (Microsoft OpenJDK)"
PORTABLE_JDK="/c/Users/NGOC LAN/.openjdk/jdk-17.0.19+10"
if [ -d "$PORTABLE_JDK" ]; then
    export JAVA_HOME="$PORTABLE_JDK"
    export PATH="$PORTABLE_JDK/bin:$PATH"
    log_info "Tìm thấy JDK 17 di động tại $PORTABLE_JDK"
fi

if ! command -v java &> /dev/null; then
    log_warn "Chưa tìm thấy Java trong PATH."
else
    log_skip "Java đã có sẵn: $(java -version 2>&1 | head -n 1)"
fi

JAVA_HOME_PATH=""
if [ -d "$PORTABLE_JDK" ]; then
    JAVA_HOME_PATH="$PORTABLE_JDK"
else
    JAVA_HOME_PATH=$(find "/c/Program Files/Microsoft" -maxdepth 1 -iname "jdk-17*" 2>/dev/null | head -n 1)
fi

if [ -n "$JAVA_HOME_PATH" ]; then
    JAVA_HOME_WIN=$(to_win_path "$JAVA_HOME_PATH")
    setx_if_changed "JAVA_HOME" "$JAVA_HOME_WIN"
else
    log_warn "Không tìm thấy thư mục JDK 17. Hãy tự cài đặt Microsoft OpenJDK 17 ngoài Windows."
fi
step_done "JDK 17"

# B2. Android Studio
step_start "Cài đặt Android Studio"
# Tự động nhận diện thư mục SDK tùy chỉnh của Bệ Hạ
if [ -d "$LOCALAPPDATA/Google/AndroidSDK" ]; then
    ANDROID_SDK_DIR="$LOCALAPPDATA/Google/AndroidSDK"
elif [ -d "$HOME/AppData/Local/Google/AndroidSDK" ]; then
    ANDROID_SDK_DIR="$HOME/AppData/Local/Google/AndroidSDK"
else
    ANDROID_SDK_DIR="$LOCALAPPDATA/Android/Sdk"
    [ -z "$LOCALAPPDATA" ] && ANDROID_SDK_DIR="$HOME/AppData/Local/Android/Sdk"
fi

if ! command -v studio64.exe &> /dev/null && [ ! -d "/c/Program Files/Android/Android Studio" ]; then
    log_skip "Tạm thời bỏ qua tự động cài Android Studio theo yêu cầu của Bệ Hạ."
else
    log_skip "Android Studio đã có sẵn."
fi
step_done "Android Studio"

# B3. Kiểm tra SDK / cmdline-tools
step_start "Kiểm tra Android SDK & sdkmanager"
SDKMANAGER=""
if [ -f "$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager.bat" ]; then
    SDKMANAGER="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager.bat"
    log_info "Tìm thấy sdkmanager tại: $SDKMANAGER"
else
    log_warn "Chưa tìm thấy sdkmanager. SDK chỉ được tải khi mở Android Studio lần đầu."
    echo ""
    echo "-----------------------------------------"
    echo "⚠️  CẦN THAO TÁC THỦ CÔNG MỘT LẦN:"
    echo "1. Mở Android Studio"
    echo "2. Hoàn tất Setup Wizard (chọn Standard) để nó tự tải SDK"
    echo "3. Chạy lại script này để tiếp tục cài SDK packages + tạo AVD"
    echo "-----------------------------------------"
fi
step_done "Kiểm tra SDK"

if [ -n "$SDKMANAGER" ]; then
    # B4. Env vars
    step_start "Set biến môi trường ANDROID_HOME / PATH"
    ANDROID_SDK_DIR_WIN=$(to_win_path "$ANDROID_SDK_DIR")
    setx_if_changed "ANDROID_HOME" "$ANDROID_SDK_DIR_WIN"
    setx_if_changed "ANDROID_SDK_ROOT" "$ANDROID_SDK_DIR_WIN"

    CURRENT_PATH=$(cmd.exe /c "echo %PATH%" 2>/dev/null | tr -d '\r')
    ADDITIONS=""
    for sub in "platform-tools" "cmdline-tools\\latest\\bin" "emulator"; do
        p="$ANDROID_SDK_DIR_WIN\\$sub"
        if [[ "$CURRENT_PATH" != *"$p"* ]]; then
            ADDITIONS="$ADDITIONS;$p"
            log_info "Sẽ thêm vào PATH: $p"
        else
            log_skip "$p đã có trong PATH."
        fi
    done
    if [ -n "$ADDITIONS" ]; then
        NEW_PATH="${CURRENT_PATH}${ADDITIONS}"
        if [ ${#NEW_PATH} -lt 1024 ]; then
            cmd.exe /c "setx PATH \"$NEW_PATH\"" >/dev/null
            log_info "Đã cập nhật PATH."
        else
            log_warn "PATH hiện tại quá dài (${#NEW_PATH} ký tự), hãy tự thêm thủ công: $ANDROID_SDK_DIR_WIN\\platform-tools, \\cmdline-tools\\latest\\bin, \\emulator"
        fi
    fi
    export PATH="$ANDROID_SDK_DIR/platform-tools:$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$ANDROID_SDK_DIR/emulator:$PATH"
    step_done "Biến môi trường Android"

    # B5. Licenses
    step_start "Chấp nhận SDK licenses"
    yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || log_warn "Một số license có thể chưa được accept, kiểm tra lại nếu build lỗi."
    step_done "SDK licenses"

    # B6. SDK packages
    step_start "Cài SDK packages (platform-tools, build-tools, platform, emulator, system-image)"
    PACKAGES=("platform-tools" "build-tools;34.0.0" "platforms;android-34" "emulator" "system-images;android-34;google_apis;x86_64")
    log_info "Danh sách package: ${PACKAGES[*]}"
    "$SDKMANAGER" "${PACKAGES[@]}" | while IFS= read -r line; do
        echo "[$(_ts)] [sdkmanager] $line"
    done
    step_done "SDK packages"

    # B7. AVD
    step_start "Tạo AVD mẫu (Pixel_6_API_34)"
    AVD_NAME="Pixel_6_API_34"
    AVDMANAGER="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/avdmanager.bat"
    if [ -f "$AVDMANAGER" ]; then
        if ! "$AVDMANAGER" list avd | grep -q "$AVD_NAME"; then
            log_info "Đang tạo AVD $AVD_NAME..."
            echo "no" | "$AVDMANAGER" create avd -n "$AVD_NAME" -k "system-images;android-34;google_apis;x86_64" -d "pixel_6"
            log_info "Tạo AVD hoàn tất."
        else
            log_skip "AVD $AVD_NAME đã tồn tại."
        fi
    else
        log_warn "Không tìm thấy avdmanager, bỏ qua bước tạo AVD."
    fi
    step_done "AVD"

    # Verify
    log_info "Kiểm tra cài đặt Android:"
    "$ANDROID_SDK_DIR/platform-tools/adb.exe" version 2>/dev/null || log_warn "adb: chưa sẵn sàng (thử mở terminal mới)"
    "$ANDROID_SDK_DIR/emulator/emulator.exe" -list-avds 2>/dev/null || log_warn "emulator: chưa sẵn sàng (thử mở terminal mới)"
fi

TOTAL_ELAPSED=$(( $(date +%s) - SCRIPT_START_TS ))
echo ""
echo "========================================="
echo "THIẾT LẬP HOÀN TẤT! (tổng thời gian: ${TOTAL_ELAPSED}s)"
echo "Đã thực hiện $STEP_COUNTER/$TOTAL_STEPS bước."
echo "Vui lòng khởi động lại Git Bash / terminal để cập nhật các biến môi trường (PATH, JAVA_HOME, ANDROID_HOME)."
echo "Chạy dev server (web): npm run dev"
echo "Chạy emulator (android): emulator -avd Pixel_6_API_34"
echo "Log chi tiết đầy đủ tại: $LOG_FILE"
echo "========================================="