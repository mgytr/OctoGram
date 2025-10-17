/*
 * This is the source code of OctoGram for Android
 * It is licensed under GNU GPL v2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023-2025.
 */

package it.octogram.android.utils.ai.whisper;

import android.content.Context;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import it.octogram.android.OctoConfig;
import it.octogram.android.utils.OctoLogging;
import it.octogram.android.utils.ai.AiPrompt;
import it.octogram.android.utils.ai.MainAiHelper;

/**
 * Helper class for local Whisper Speech-to-Text using TensorFlow Lite/LiteRT.
 * <p>
 * This implementation uses on-device inference with Whisper base model (~140MB)
 * for offline, privacy-focused transcription without API costs.
 * <p>
 * Supported models:
 * - whisper-base: ~140MB, good accuracy
 * - whisper-small: ~460MB, better accuracy (future)
 */
public class WhisperLocalHelper {
    private static final String TAG = "WhisperLocalHelper";
    private static final String MODEL_FILENAME = "whisper_base.tflite";
    private static final int SAMPLE_RATE = 16000;
    private static final int N_SAMPLES = 30 * SAMPLE_RATE; // 30 seconds
    
    private static WhisperModel whisperModel = null;
    
    /**
     * Transcribes audio using local Whisper model
     */
    public static void prompt(AiPrompt aiPrompt, MainAiHelper.OnResultState callback) {
        if (!isAvailable()) {
            callback.onFailed();
            return;
        }

        // Whisper only works with audio files
        if (aiPrompt.getFilePath().isEmpty()) {
            callback.onFailed();
            return;
        }

        new Thread(() -> {
            try {
                // Initialize model if needed
                if (whisperModel == null) {
                    synchronized (WhisperLocalHelper.class) {
                        if (whisperModel == null) {
                            String modelPath = getModelPath();
                            if (modelPath == null || !new File(modelPath).exists()) {
                                OctoLogging.e(TAG, "Whisper model not found. Please download it first.");
                                callback.onFailed();
                                return;
                            }
                            whisperModel = new WhisperModel(modelPath);
                        }
                    }
                }

                File audioFile = new File(aiPrompt.getFilePath());
                if (!audioFile.exists()) {
                    OctoLogging.e(TAG, "Audio file does not exist: " + aiPrompt.getFilePath());
                    callback.onFailed();
                    return;
                }

                // Process audio file
                float[] audioData = preprocessAudio(audioFile);
                
                // Run inference
                String transcription = whisperModel.transcribe(audioData);
                
                if (transcription == null || transcription.trim().isEmpty()) {
                    callback.onEmptyResponse();
                } else {
                    callback.onSuccess(transcription.trim());
                }

            } catch (Exception e) {
                OctoLogging.e(TAG, "Error during Whisper transcription: " + e.getMessage(), e);
                callback.onFailed();
            }
        }).start();
    }

    /**
     * Preprocesses audio file to format required by Whisper model
     * Converts to 16kHz mono float array
     */
    private static float[] preprocessAudio(File audioFile) throws IOException {
        // Note: This is a simplified version. In production, you'd use
        // Android AudioRecord or MediaCodec to properly decode and resample
        
        // For now, assume the audio is already in the right format
        // or use a library like FFmpeg for Android
        
        // Read raw audio data (placeholder - needs proper audio decoding)
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] audioBytes = new byte[(int) audioFile.length()];
            int readBytes = fis.read(audioBytes);
            
            // Convert bytes to float array (simplified)
            float[] audioFloat = new float[Math.min(audioBytes.length / 2, N_SAMPLES)];
            ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
            
            for (int i = 0; i < audioFloat.length && byteBuffer.remaining() >= 2; i++) {
                short sample = byteBuffer.getShort();
                audioFloat[i] = sample / 32768.0f; // Normalize to [-1, 1]
            }
            
            // Pad or truncate to N_SAMPLES
            if (audioFloat.length < N_SAMPLES) {
                float[] padded = new float[N_SAMPLES];
                System.arraycopy(audioFloat, 0, padded, 0, audioFloat.length);
                return padded;
            }
            
            return audioFloat;
        }
    }

    private static String getModelPath() {
        String savedPath = OctoConfig.INSTANCE.aiFeaturesWhisperModelPath.getValue();
        if (savedPath != null && !savedPath.isEmpty()) {
            return savedPath;
        }
        
        // Default path in app's files directory
        Context context = ApplicationLoader.applicationContext;
        File modelsDir = new File(context.getFilesDir(), "whisper_models");
        File modelFile = new File(modelsDir, MODEL_FILENAME);
        
        if (modelFile.exists()) {
            OctoConfig.INSTANCE.aiFeaturesWhisperModelPath.updateValue(modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        }
        
        return null;
    }

    public static boolean isAvailable() {
        if (!OctoConfig.INSTANCE.aiFeaturesUseLocalWhisper.getValue()) {
            return false;
        }
        
        // Check if model is downloaded
        return OctoConfig.INSTANCE.aiFeaturesWhisperModelDownloaded.getValue() 
            && getModelPath() != null;
    }
    
    /**
     * Downloads the Whisper model
     * Note: This is a placeholder - actual implementation would download from a CDN
     */
    public static void downloadModel(ModelDownloadCallback callback) {
        new Thread(() -> {
            try {
                // TODO: Implement actual model download
                // For now, this is a placeholder
                // In production, download from: https://huggingface.co/openai/whisper-base/resolve/main/whisper_base.tflite
                
                Context context = ApplicationLoader.applicationContext;
                File modelsDir = new File(context.getFilesDir(), "whisper_models");
                modelsDir.mkdirs();
                
                File modelFile = new File(modelsDir, MODEL_FILENAME);
                
                // Simulate download progress
                callback.onProgress(0);
                
                // TODO: Actually download the file
                // For now, mark as failed since we don't have the file
                OctoLogging.e(TAG, "Model download not yet implemented. Please manually place whisper_base.tflite in " + modelsDir.getAbsolutePath());
                callback.onError("Model download not yet implemented");
                
            } catch (Exception e) {
                OctoLogging.e(TAG, "Error downloading model: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    public interface ModelDownloadCallback {
        void onProgress(int progress);
        void onComplete(String modelPath);
        void onError(String error);
    }
    
    /**
     * Wrapper for TensorFlow Lite Whisper model
     * Note: This is a simplified placeholder. Real implementation would use TFLite Interpreter
     */
    private static class WhisperModel {
        // TODO: Implement actual TFLite model loading and inference
        // This requires:
        // 1. TFLite Interpreter initialization
        // 2. Input tensor preparation (mel spectrogram)
        // 3. Running inference
        // 4. Decoding output tokens to text
        
        private final String modelPath;
        
        public WhisperModel(String modelPath) {
            this.modelPath = modelPath;
            OctoLogging.d(TAG, "Initialized Whisper model from: " + modelPath);
            // TODO: Load TFLite model using org.tensorflow.lite.Interpreter
        }
        
        public String transcribe(float[] audioData) {
            // Placeholder implementation
            // Real implementation would:
            // 1. Convert audio to mel spectrogram
            // 2. Run TFLite inference
            // 3. Decode tokens to text
            
            OctoLogging.w(TAG, "Using placeholder transcription - TFLite implementation pending");
            return "Transcription pending - TFLite model integration needed";
        }
    }
}
