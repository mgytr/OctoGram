# Whisper STT Integration

This document describes the OpenAI Whisper Speech-to-Text (STT) integration added to OctoGram.

## Overview

The Whisper integration allows users to transcribe voice messages using OpenAI's Whisper API. This implementation follows the same pattern as other AI providers (Gemini, ChatGPT, OpenRouter) for consistency.

## Implementation Details

### Files Added

1. **WhisperHelper.java** (`utils/ai/whisper/WhisperHelper.java`)
   - Main helper class for Whisper API integration
   - Handles audio file upload and transcription
   - Supports automatic language detection
   - Based on GeminiHelper pattern

2. **MultipartHTTPRequest.java** (`utils/network/MultipartHTTPRequest.java`)
   - Utility class for HTTP multipart/form-data requests
   - Used for uploading audio files to Whisper API
   - Handles file and form field parts

### Files Modified

1. **Enum.kt** - Added `WHISPER` to `AiProvidersDetails` enum
2. **OctoConfig.java** - Added Whisper configuration properties:
   - `aiFeaturesUseWhisperAPI` - Enable/disable Whisper
   - `aiFeaturesWhisperAPIKey` - API key storage
   - `aiFeaturesWhisperModel` - Model selection (default: "whisper-1")

3. **MainAiHelper.java** - Integrated WhisperHelper into provider selection
4. **AiProvidersConfigBottomSheet.java** - Added Whisper login/configuration UI
5. **strings_octo.xml** - Added Whisper UI strings

## How It Works

### Audio Transcription Flow

1. User receives a voice message in Telegram
2. User selects "Transcribe" option (if enabled)
3. System checks if Whisper provider is available and configured
4. Audio file is uploaded to OpenAI Whisper API
5. Transcription is returned and displayed to user

### Supported Audio Formats

- OGG (Telegram voice messages)
- MP3
- WAV
- M4A

### API Details

- **Endpoint**: `https://api.openai.com/v1/audio/transcriptions`
- **Model**: `whisper-1` (configurable)
- **Authentication**: Bearer token (OpenAI API key)
- **Request**: Multipart form-data with audio file
- **Response**: JSON with transcribed text

## Configuration

### For Users

1. Navigate to AI Features settings
2. Select "Add Provider" → "Whisper API"
3. Follow the steps to get an OpenAI API key:
   - Go to https://platform.openai.com/api-keys
   - Create a new secret key
   - Copy and paste into OctoGram
4. Enable transcription in AI Features settings

### For Developers

The Whisper provider integrates seamlessly with the existing AI features architecture:

```java
// Check if Whisper is available
if (WhisperHelper.isAvailable()) {
    // Create audio prompt
    AiPrompt prompt = new AiPrompt(
        "System prompt",
        "User text", 
        "/path/to/audio.ogg",
        "audio/ogg",
        false // loadAsImage
    );
    
    // Call Whisper API
    WhisperHelper.prompt(prompt, new MainAiHelper.OnResultState() {
        @Override
        public void onSuccess(String transcription) {
            // Handle transcription
        }
        
        @Override
        public void onFailed() {
            // Handle error
        }
    });
}
```

## Future Enhancements

### Local Whisper Model Support

The current implementation uses OpenAI's cloud-based Whisper API. A future enhancement could add support for local inference using whisper.cpp:

**Benefits of Local Model:**
- No API costs
- Offline functionality
- Privacy (no data sent to cloud)
- No rate limits

**Implementation Requirements:**
- Integrate whisper.cpp via JNI
- Add model file management (whisper-base.bin ~140MB)
- Audio format conversion to 16kHz WAV
- Additional native dependencies

**Recommended Models for Local Use:**
- `whisper-base` (~140MB) - Good balance of size and accuracy
- `whisper-small` (~460MB) - Better accuracy, larger size

## Limitations

### Current Limitations

1. **Cloud-based**: Requires internet connection and API key
2. **Cost**: OpenAI Whisper API is a paid service ($0.006 per minute)
3. **File Size**: Limited by API constraints (typically 25MB max)
4. **Language**: Auto-detection (manual language selection not yet implemented)

### Known Issues

None currently identified. The implementation follows the established patterns in the codebase.

## Testing

### Manual Testing Steps

1. Configure Whisper API key in settings
2. Receive or send a voice message
3. Long-press on voice message
4. Select "Transcribe" option
5. Verify transcription appears correctly

### Test Cases

- [ ] Voice message in English
- [ ] Voice message in other languages
- [ ] Voice message with background noise
- [ ] Invalid API key handling
- [ ] Network error handling
- [ ] Rate limit handling (429 error)

## API Key Security

⚠️ **Important**: API keys are stored locally in shared preferences. Users should:
- Never share their API keys
- Rotate keys if compromised
- Monitor API usage on OpenAI dashboard
- Set usage limits to prevent unexpected charges

## References

- [OpenAI Whisper API Documentation](https://platform.openai.com/docs/guides/speech-to-text)
- [whisper.cpp GitHub](https://github.com/ggerganov/whisper.cpp) (for future local implementation)
- [Whisper Model Card](https://github.com/openai/whisper/blob/main/model-card.md)

## Changelog

### Version 1.0 (Current)
- Initial Whisper STT integration
- OpenAI API support
- Multipart file upload utility
- UI configuration screens
- Auto-detected language support
