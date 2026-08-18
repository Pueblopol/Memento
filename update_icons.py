from PIL import Image, ImageDraw
import os

source_path = '/home/pol/Scaricati/Newlogo.jpeg'
base_res_dir = '/home/pol/Progetti/Memento/app/src/main/res'

resolutions = {
    'mdpi': (48, 108),
    'hdpi': (72, 162),
    'xhdpi': (96, 216),
    'xxhdpi': (144, 324),
    'xxxhdpi': (192, 432)
}

def make_round(img):
    # create a circular mask
    mask = Image.new('L', img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, img.size[0], img.size[1]), fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result

try:
    with Image.open(source_path) as img:
        img = img.convert('RGBA')
        
        # force square by cropping the center
        width, height = img.size
        size = min(width, height)
        left = (width - size) / 2
        top = (height - size) / 2
        right = (width + size) / 2
        bottom = (height + size) / 2
        img = img.crop((left, top, right, bottom))
        
        for density, (legacy_size, adaptive_size) in resolutions.items():
            out_dir = os.path.join(base_res_dir, f'mipmap-{density}')
            os.makedirs(out_dir, exist_ok=True)
            
            # legacy icon
            legacy_img = img.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
            legacy_img.save(os.path.join(out_dir, 'ic_launcher.webp'), 'WEBP')
            
            # legacy round
            round_img = make_round(legacy_img)
            round_img.save(os.path.join(out_dir, 'ic_launcher_round.webp'), 'WEBP')
            
            # adaptive foreground
            adaptive_img = img.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS)
            adaptive_img.save(os.path.join(out_dir, 'ic_launcher_foreground.webp'), 'WEBP')

    print("Icons successfully updated!")
except Exception as e:
    print(f"Error: {e}")

