#!/bin/bash

# Biên dịch và cài đặt ứng dụng
echo "Đang biên dịch và cài đặt ứng dụng lên thiết bị ảo..."
./gradlew installDebug

if [ $? -ne 0 ]; then
  echo "LỖI: Biên dịch Gradle thất bại."
  exit 1
fi

# Chạy ứng dụng thông qua adb
echo "Đang mở ứng dụng..."
adb shell am start -n com.wildlife.deterrence/.MainActivity

if [ $? -ne 0 ]; then
  echo "LỖI: Không thể khởi chạy ứng dụng. Đảm bảo thiết bị đã kết nối adb."
  exit 1
fi

echo "Ứng dụng đã khởi chạy thành công! Đang lọc xem logcat của app..."
adb logcat | grep com.wildlife.deterrence
