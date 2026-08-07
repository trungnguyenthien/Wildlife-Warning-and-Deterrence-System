## "Em code phần nào trong dự án này?"

Tự tin trả lời "Tui không code" nhưng phải giải thích mình đang áp dụng theo xu hướng Spec Driven Development (SDD).
Giải thích SDD là gì, vai trò con người trong SDD là gì?

### Cấu trúc Spec của dự án

Tham khảo "docs/README.md"

## Cách em thiết kế giao diện (UI) cho Mobile App?

Dựa vào tài liệu "docs/02-dac-ta-man-hinh-android-app.md" chúng ta mô tả chi tiết cấu trúc giao diện, flow di chuyển giữa các màn hình.

Sử dụng công cụ "https://stitch.withgoogle.com/" của Google để đọc các đặc tả này và sinh ra giao diện. Đương nhiên sẽ có 1 số điểm nếu không map với đặc tả thì mình yêu cầu AI fix lại.

## Cách em yêu cầu AI sinh code cho từng màn hình và từng chức năng?

Em sẽ yêu cầu AI lên "plan" trước, em sẽ review plan và duyệt plan trước khi AI sinh code.

### Sample plan: wildlife-mobile/screen-plans/register-screen-plan.md

### Hướng dẫn AI viết plan: (mục 1a, 1b) wildlife-mobile/AI_INSTRUCTIONS.md

## Làm sao để em biết AI sinh code đúng như đặc tả?

Trong plan AI sử dụng trước khi viết code, dựa vào các đặc tả của tài liệu 01, 02, em có yêu cầu AI liệt kê các case mà AI có thể sinh code để test tự động (automation test) và các case con người phải test thủ công. Em sẽ review kỹ kế hoạch test và bổ sung các case cần thiết trước khi cho AI triển khai.

## Trong hệ thống có các thành phần nào?

Đọc tài liệu 04, phần "Sơ đồ Kiến trúc Tương tác Tổng quan"

Chia làm 2 phần chính:

- Phần do nhóm phát triển: Camera (Rasperry Pi), Ai Server (nhận diện ảnh), Mobile Server (API và Admin App), Mobile App.

- Phần các dịch vụ Cloud ngoài: Ably (real-time data streaming), Cloudinary (Lưu trữ ảnh), FCM (Gửi thông báo Push).

## Khi giám khảo hỏi về luồng xử lý, ví dụ "Khi camera phát hiện động vật, hệ thống sẽ xử lý như thế nào?"

Đọc tài liệu 04, sẽ chuyên về các dạng câu hỏi này. Em cần đọc sơ để biết trong tài liệu 04 đang đặc tả các luồng được xử lý như thế nào.
