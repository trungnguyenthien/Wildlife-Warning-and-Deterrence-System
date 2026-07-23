#!/bin/bash

# Tìm danh sách các thiết bị ảo AVD
echo "Danh sách Emulator (AVD) khả dụng:"
AVDS=$(emulator -list-avds)

if [ -z "$AVDS" ]; then
  echo "LỖI: Không tìm thấy thiết bị ảo (AVD) nào. Vui lòng tạo AVD trong Android Studio trước."
  exit 1
fi

# Lấy thiết bị ảo đầu tiên tìm được
FIRST_AVD=$(echo "$AVDS" | head -n 1)
echo "Đang khởi chạy Emulator: $FIRST_AVD..."

# Khởi động emulator trong nền
emulator -avd "$FIRST_AVD" -dns-server 8.8.8.8 &

# Đợi Emulator boot hoàn tất
echo "Đang đợi thiết bị ảo khởi động hoàn tất..."
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ] ; do
  echo -n "."
  sleep 2
done

echo ""
echo "Thiết bị ảo đã sẵn sàng!"
