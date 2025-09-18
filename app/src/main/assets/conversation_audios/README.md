Conversation Lesson local audio support

Place your per-topic conversation audios under this folder using the following structure:

- conversation_audios/
  - <topic-slug>/
    - turn_1.wav
    - turn_2.wav
    - turn_3.wav
    - ...

Where <topic-slug> is derived from the topic title by:
- lowercasing the title
- replacing any sequence of non-alphanumeric characters with a single underscore
- trimming leading/trailing underscores

Examples:
- "Daily Activities" -> daily_activities
- "At the Airport" -> at_the_airport
- "Food & Drinks" -> food_drinks

In code, ConversationLesson will try to play the asset at:

  assets/conversation_audios/<topic-slug>/turn_<N>.wav

for each turn index N (1-based). If the asset is missing or fails to play, the app will automatically fall back to server TTS for that turn.

Alternative (res/raw) fallback:
- You may also optionally place per-turn audio in res/raw using one of these naming patterns:
  - conv_<topic-slug>_turn_<N>
  - <topic-slug>_turn_<N>
  - turn_<N>

Note: res/raw does not allow subfolders and requires file names without dashes/spaces and with a-z0-9_ only. Also, resources in res/raw cannot use the .wav extension in the resource ID; the extension is part of the file name but you reference it via R.raw.<name> (without extension). The code already searches these names automatically if asset lookup fails.

Supported formats:
- WAV is supported (current asset naming assumes .wav). If you prefer MP3, rename your files accordingly and adjust code if you change extensions globally.

How to import your existing files:
- Copy your folders from your local directory (e.g., C:\Users\user\gemini-tts\conversation_audios) into:

  jetpack/app/src/main/assets/conversation_audios/

so the structure becomes, for example:

  jetpack/app/src/main/assets/conversation_audios/daily_activities/turn_1.wav
  jetpack/app/src/main/assets/conversation_audios/daily_activities/turn_2.wav
  ...

No Gradle changes are required; the assets/ directory is bundled automatically.
