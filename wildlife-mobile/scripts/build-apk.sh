#!/bin/bash

# Xác định thư mục chứa script này để chuyển về thư mục gốc của project android (wildlife-mobile)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$( dirname "$SCRIPT_DIR" )"

cd "$PROJECT_DIR" || exit 1

echo "=========================================================="
echo "BẮT ĐẦU BIÊN DỊCH VÀ XUẤT FILE APK CÀI ĐẶT..."
echo "Thư mục dự án: $PROJECT_DIR"
echo "=========================================================="

# Chạy lệnh clean và assembleDebug để build ra file APK debug
./gradlew clean assembleDebug

if [ $? -ne 0 ]; then
  echo "LỖI: Quá trình biên dịch Gradle thất bại."
  exit 1
fi

APK_SRC_PATH="app/build/outputs/apk/debug/app-debug.apk"
APK_DEST_PATH="wildlife-app-debug.apk"

if [ -f "$APK_SRC_PATH" ]; then
  # Sao chép file APK ra thư mục gốc wildlife-mobile
  cp "$APK_SRC_PATH" "$APK_DEST_PATH"
  echo "=========================================================="
  echo "BIÊN DỊCH THÀNH CÔNG!"
  echo "File APK cài đặt đã được đặt tại:"
  echo " -> $PROJECT_DIR/$APK_DEST_PATH"
  echo "=========================================================="
else
  echo "LỖI: Không tìm thấy file APK tại đường dẫn $APK_SRC_PATH sau khi build."
  exit 1
fi
