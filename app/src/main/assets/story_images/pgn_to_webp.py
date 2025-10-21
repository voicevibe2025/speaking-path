from PIL import Image
import os

# Folder paths (edit these as needed)
input_folder = "malin_kundang"       # folder where your PNG/JPEG files are
output_folder = "malin_kundang" # folder to save converted files

# Make sure the input folder exists
if not os.path.exists(input_folder):
    print(f"❌ Input folder '{input_folder}' not found!")
    exit()

# Create output folder if it doesn't exist
os.makedirs(output_folder, exist_ok=True)

# Track how many files converted
converted = 0

# Loop through all files
for filename in os.listdir(input_folder):
    lower = filename.lower()
    if lower.endswith((".png", ".jpg", ".jpeg")):
        input_path = os.path.join(input_folder, filename)
        output_path = os.path.join(output_folder, os.path.splitext(filename)[0] + ".webp")

        try:
            with Image.open(input_path) as img:
                img = img.convert("RGB")  # ensures compatibility
                img.save(output_path, "webp", quality=85)
                print(f"✅ Converted: {filename} → {os.path.basename(output_path)}")
                converted += 1
        except Exception as e:
            print(f"⚠️ Failed to convert {filename}: {e}")

if converted == 0:
    print("⚠️ No PNG or JPEG files found in the folder.")
else:
    print(f"\n🎉 Done! Converted {converted} file(s) to WebP.")
