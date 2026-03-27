import os
from PIL import Image, ImageOps

def make_icon(source_path, target_path, logo_target_size=880):
    if not os.path.exists(source_path):
        print(f"Error: Source {source_path} not found.")
        return

    img = Image.open(source_path)
    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # Trim transparency
    alpha = img.getchannel('A')
    mask = alpha.point(lambda p: 255 if p > 10 else 0)
    bbox = mask.getbbox()
    if bbox:
        img = img.crop(bbox)

    # Calculate scaling
    w, h = img.size
    aspect = w / h
    if aspect > 1:
        new_w = logo_target_size
        new_h = int(logo_target_size / aspect)
    else:
        new_h = logo_target_size
        new_w = int(logo_target_size * aspect)

    img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)

    # Create 1024x1024 opaque white background
    final = Image.new("RGB", (1024, 1024), (255, 255, 255))
    
    # Precise centering
    x = (1024 - new_w) // 2
    y = (1024 - new_h) // 2
    
    # Paste logo onto white background
    final.paste(img, (x, y), img if img.mode == 'RGBA' else None)
    final.save(target_path, "PNG")
    print(f"Icon saved to {target_path} (Final: {new_w}x{new_h})")

# SITA Seal source
source_sita = "/Users/shah/AndroidStudioProjects/sita_apps/sita_card_master/composeApp/src/androidMain/res/drawable/sita_logo.png"

# Target for SITA Card Master
dest_master = "/Users/shah/AndroidStudioProjects/sita_apps/sita_card_master/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
make_icon(source_sita, dest_master, logo_target_size=880)

# Target for SITA Cardent (DeviSoft)
dest_cardent = "/Users/shah/AndroidStudioProjects/sita_apps/sita_cardent/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
make_icon(source_sita, dest_cardent, logo_target_size=880)
