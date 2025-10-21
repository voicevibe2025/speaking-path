# Story Text Assets

Place one .txt file per story in this folder. File names must match the story slug used in the app.

Naming convention:
- <slug>.txt
- Encoding: UTF-8
- Plain text (no markdown required)

Current stories and slugs:
- malin_kundang.txt — Malin Kundang
- sangkuriang.txt — Sangkuriang
- timun_mas.txt — Timun Mas
- roro_jonggrang.txt — Roro Jonggrang
- legend_of_lake_toba.txt — Legend of Lake Toba

Audio files:
- Stored in Supabase public bucket: indo-folklore-audio
- File names should match slugs with .ogg extension, e.g. malin_kundang.ogg
- App builds URL as: https://<project-id>.supabase.co/storage/v1/object/public/indo-folklore-audio/<slug>.ogg

Troubleshooting:
- If a story fails to load, verify the .txt exists here and the slug matches exactly.
- If audio fails, confirm the .ogg exists in the Supabase bucket and is publicly readable.
