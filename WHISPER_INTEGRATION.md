# Local Whisper STT Integration

This document describes the local Whisper Speech-to-Text (STT) integration added to OctoGram using TensorFlow Lite/LiteRT.

## Overview

The Whisper integration allows users to transcribe voice messages using a local, on-device Whisper model. This implementation uses TensorFlow Lite (now called LiteRT - AI Edge Lite Runtime) for offline transcription without API costs or internet requirements after initial model download.

## Implementation Details

### Files Added

1. **WhisperLocalHelper.java** (`utils/ai/whisper/WhisperLocalHelper.java`)
   - Main helper class for local Whisper inference
   - Uses TensorFlow Lite/LiteRT for on-device processing
   - Handles audio preprocessing and model management
   - Based on GeminiHelper pattern for consistency

### Files Modified

1. **Enum.kt** - Added `WHISPER` to `AiProvidersDetails` enum (local, no API key required)
2. **OctoConfig.java** - Added Whisper configuration properties:
   - `aiFeaturesUseLocalWhisper` - Enable/disable toggle
   - `aiFeaturesWhisperModelPath` - Path to downloaded model
   - `aiFeaturesWhisperModelDownloaded` - Model download status

3. **MainAiHelper.java** - Integrated WhisperLocalHelper into provider selection
4. **AiProvidersConfigBottomSheet.java** - Added Whisper configuration UI
5. **strings_octo.xml** - Added Whisper UI strings
6. **build.gradle** - Added TensorFlow Lite dependencies
7. **libs.versions.toml** - Added TFLite version configuration

## How It Works

### Audio Transcription Flow

1. User receives a voice message in Telegram
2. User selects "Transcribe" option (if enabled and model downloaded)
3. Audio file is preprocessed (converted to 16kHz mono)
4. Local TFLite model performs inference on-device
5. Transcription is returned and displayed to user

### Supported Audio Formats

- OGG (Telegram voice messages)
- MP3
- WAV
- M4A

### Model Details

- **Model**: Whisper Base (~140MB)
- **Framework**: TensorFlow Lite 2.14.0 / LiteRT
- **Processing**: 100% on-device, no internet required after download
- **Privacy**: Audio never leaves the device

## Configuration

### For Users

1. Navigate to AI Features settings
2. Select "Add Provider" → "Local Whisper"
3. Download the Whisper base model (~140MB one-time download)
4. Enable transcription in AI Features settings
5. Transcribe voice messages offline!

### Model Download

The first time Whisper is enabled, the app will:
- Download the Whisper base model from CDN (~140MB)
- Store it in app's private storage
- Verify the model integrity
- Enable transcription functionality

**Note**: Model download requires ~140MB of storage and initial internet connection.

## Architecture

The implementation follows the existing AI provider pattern but with local inference:

```
Voice Message → WhisperLocalHelper → TFLite Model → Transcription
     ↓                                      ↓
Audio Preprocessing                  On-Device Inference
(16kHz mono conversion)            (No internet required)
```

## Advantages Over Cloud API

### Benefits of Local Model

✅ **No API Costs** - Completely free after model download  
✅ **Privacy** - Audio never leaves device  
✅ **Offline** - Works without internet connection  
✅ **No Rate Limits** - Transcribe unlimited messages  
✅ **Fast** - No network latency  

### Trade-offs

- **Storage**: Requires ~140MB for model file
- **Initial Setup**: One-time model download required
- **Device Requirements**: Needs reasonable CPU/GPU

## Technical Implementation

### TensorFlow Lite Integration

Dependencies added:
```gradle
implementation 'org.tensorflow:tensorflow-lite:2.14.0'
implementation 'org.tensorflow:tensorflow-lite-support:2.14.0'
```

Usage:
```java
// Initialize model
WhisperModel model = new WhisperModel(modelPath);

// Transcribe audio
float[] audioData = preprocessAudio(audioFile);
String transcription = model.transcribe(audioData);
```

### Audio Preprocessing

The audio preprocessing pipeline:
1. Read audio file (OGG/MP3/WAV/M4A)
2. Decode to PCM
3. Resample to 16kHz
4. Convert to mono
5. Normalize to float32 [-1, 1]
6. Pad/truncate to 30 seconds

### Model Format

- **Input**: Float32 audio array (16kHz, mono, 30s max)
- **Output**: Text transcription string
- **Format**: TensorFlow Lite (.tflite)

## Current Status

### Implemented ✅
- [x] TensorFlow Lite dependency integration
- [x] WhisperLocalHelper framework
- [x] Provider configuration
- [x] UI integration
- [x] Model management structure

### TODO (Requires Completion)
- [ ] Complete TFLite model loading/inference
- [ ] Proper audio preprocessing (requires FFmpeg or AudioRecord)
- [ ] Model download implementation
- [ ] Mel spectrogram computation
- [ ] Token decoding logic

## Future Enhancements

### Model Options
- **whisper-tiny** (~40MB) - Fastest, lower accuracy
- **whisper-small** (~460MB) - Better accuracy
- **whisper-medium** (~1.5GB) - High accuracy

### Optimizations
- GPU acceleration using TFLite GPU delegate
- NNAPI acceleration for supported devices
- Model quantization for smaller size
- Streaming inference for long audio

## Testing

### Manual Testing Steps

1. Enable Local Whisper in settings
2. Download model (first time only)
3. Receive or send a voice message
4. Long-press on voice message
5. Select "Transcribe" option
6. Verify transcription appears correctly
7. Test offline (airplane mode)

### Test Cases

- [ ] Voice message in English
- [ ] Voice message in other languages
- [ ] Voice message with background noise
- [ ] Offline transcription
- [ ] Model download process
- [ ] Storage management

## Model Download

The Whisper base TFLite model can be obtained from:
- Hugging Face: https://huggingface.co/openai/whisper-base
- Convert using: `optimum-cli export tflite --model openai/whisper-base whisper_base.tflite`

Or use pre-converted models from community sources.

## References

- [TensorFlow Lite Documentation](https://www.tensorflow.org/lite)
- [LiteRT (AI Edge)](https://ai.google.dev/edge/litert)
- [Whisper Model](https://github.com/openai/whisper)
- [TFLite Model Conversion](https://www.tensorflow.org/lite/models/convert)

## Changelog

### Version 1.0 (Current)
- Initial local Whisper STT integration
- TensorFlow Lite/LiteRT support (replacing cloud API)
- On-device inference framework
- Model management structure
- Privacy-focused, offline transcription
