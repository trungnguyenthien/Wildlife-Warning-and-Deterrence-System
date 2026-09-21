import os
from PIL import Image

src_path = '/Users/trungnguyen/.gemini/antigravity-ide/brain/cad5b52a-90d5-4cfb-a1b9-84ce8091f2e7/.user_uploaded/media_1789962023976.jpg'
res_dir = 'app/src/main/res'

img = Image.open(src_path).convert('RGBA')
datas = img.getdata()

# Extract white deer head, making green background transparent
newData = []
for item in datas:
    r, g, b, a = item
    # Green background check
    if g > 100 and g > r + 20 and g > b + 20:
        newData.append((255, 255, 255, 0))
    else:
        newData.append((255, 255, 255, 255))

img.putdata(newData)

# Generate foreground launcher icon (deer head centered in 108x108 safe zone)
deer = img.resize((68, 68), Image.Resampling.LANCZOS)
fg_108 = Image.new('RGBA', (108, 108), (0, 0, 0, 0))
fg_108.paste(deer, ((108 - 68) // 2, (108 - 68) // 2), deer)

# Save to drawable/ic_launcher_fg.png
drawable_dir = os.path.join(res_dir, 'drawable')
os.makedirs(drawable_dir, exist_ok=True)
fg_108.save(os.path.join(drawable_dir, 'ic_launcher_fg.png'), 'PNG')

# Remove old default robot ic_launcher_foreground.xml if present
xml_fg = os.path.join(drawable_dir, 'ic_launcher_foreground.xml')
if os.path.exists(xml_fg):
    os.remove(xml_fg)

# Also update fawn_logo.png, app_icon.png, elephant.png
img_original = Image.open(src_path)
img_original.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(drawable_dir, 'fawn_logo.png'), 'PNG')
img_original.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(drawable_dir, 'app_icon.png'), 'PNG')
img_original.resize((512, 512), Image.Resampling.LANCZOS).save(os.path.join(drawable_dir, 'elephant.png'), 'PNG')

print('SUCCESS UPDATED DEER ICON FG')
