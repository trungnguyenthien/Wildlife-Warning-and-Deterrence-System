#!/usr/bin/env bash
# ============================================================
# start-server.sh — Khởi động wildlife-mobile-server
# ============================================================
# Dùng: ./scripts/start-server.sh [--env <file>] [--port <port>]
#   --env   : File biến môi trường (mặc định: .env.local)
#   --port  : Cổng lắng nghe       (mặc định: 3000)
# ============================================================

set -e

# ---- Mặc định -----------------------------------------------
ENV_FILE=".env.local"
PORT_OVERRIDE=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ---- Parse tham số dòng lệnh --------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env)   ENV_FILE="$2"; shift 2 ;;
    --port)  PORT_OVERRIDE="$2"; shift 2 ;;
    --help)
      grep '^#' "$0" | sed 's/^# //' | sed 's/^#//'
      exit 0
      ;;
    *)
      echo "Tham số không hợp lệ: $1  (Dùng --help để xem hướng dẫn)"
      exit 1
      ;;
  esac
done

cd "$PROJECT_DIR"

# ---- Kiểm tra file môi trường --------------------------------
if [[ ! -f "$ENV_FILE" ]]; then
  echo "⚠️  Cảnh báo: Không tìm thấy file môi trường '$ENV_FILE'."
  echo "   Hãy tạo file $ENV_FILE hoặc truyền --env <file>."
  echo "   Server vẫn sẽ khởi động với biến môi trường hệ thống hiện có."
fi

# ---- Kiểm tra node_modules -----------------------------------
if [[ ! -d "node_modules" ]]; then
  echo "📦 Chưa có node_modules. Đang cài dependencies..."
  npm install
fi

# ---- Ghi đè PORT nếu được chỉ định --------------------------
if [[ -n "$PORT_OVERRIDE" ]]; then
  export PORT="$PORT_OVERRIDE"
fi

# ---- Khởi động server ----------------------------------------
echo ""
echo "🚀 Đang khởi động wildlife-mobile-server..."
echo "   Môi trường : $ENV_FILE"
echo "   Cổng       : ${PORT:-3000}"
echo "   Thư mục    : $PROJECT_DIR"
echo ""

exec npx dotenv-cli -e "$ENV_FILE" -- tsx watch src/index.ts
