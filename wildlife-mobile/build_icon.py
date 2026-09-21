import os
from PIL import Image

src_path = '/Users/trungnguyen/.gemini/antigravity-ide/brain/cad5b52a-90d5-4cfb-a1b9-84ce8091f2e7/.user_uploaded/media_1789978241727.png'
res_dir = 'app/src/main/res'

img = Image.open(src_path).convert('RGBA')
width, height = img.size

# 1. Tách hình chú hươu sừng kiêu hãnh và chuyển sang màu trắng tinh trên nền trong suốt cho Launcher Foreground
white_deer = Image.new('RGBA', (width, height), (0, 0, 0, 0))
pixels = img.load()
w_pixels = white_deer.load()

for y in range(height):
    for x in range(width):
        r, g, b, a = pixels[x, y]
        luminance = (r + g + b) // 3
        # Điểm ảnh hươu màu xanh thẫm / tối
        if a > 10 and luminance < 150 and r < 130 and g < 145:
            w_pixels[x, y] = (255, 255, 255, 255)

# Resize chú hươu kiêu hãnh vào vùng an toàn 76x76 px trong khung 108x108 px
deer_fg = white_deer.resize((76, 76), Image.Resampling.LANCZOS)
fg_108 = Image.new('RGBA', (108, 108), (0, 0, 0, 0))
fg_108.paste(deer_fg, ((108 - 76) // 2, (108 - 76) // 2), deer_fg)

drawable_dir = os.path.join(res_dir, 'drawable')
os.makedirs(drawable_dir, exist_ok=True)
fg_108.save(os.path.join(drawable_dir, 'ic_launcher_fg.png'), 'PNG')

# 2. Tạo logo 512x512 trên nền xanh lá #27AE60 phục vụ giao diện ứng dụng
logo_bg = Image.new('RGBA', (512, 512), (39, 174, 96, 255))
w_deer_512 = white_deer.resize((380, 380), Image.Resampling.LANCZOS)
logo_bg.paste(w_deer_512, ((512 - 380) // 2, (512 - 380) // 2), w_deer_512)

logo_bg.save(os.path.join(drawable_dir, 'fawn_logo.png'), 'PNG')
logo_bg.save(os.path.join(drawable_dir, 'app_icon.png'), 'PNG')
logo_bg.save(os.path.join(drawable_dir, 'elephant.png'), 'PNG')

print('STANDING STAG DEER ICON BUILT SUCCESSFULLY')
