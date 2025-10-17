/*
 * This is the source code of OctoGram for Android
 * It is licensed under GNU GPL v2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023-2025.
 */

package it.octogram.android.utils.ai.whisper;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import it.octogram.android.OctoConfig;
import it.octogram.android.utils.OctoLogging;
import it.octogram.android.utils.ai.AiPrompt;
import it.octogram.android.utils.ai.MainAiHelper;
import it.octogram.android.utils.network.MultipartHTTPRequest;
import it.octogram.android.utils.network.StandardHTTPRequest;

public class WhisperHelper {
    private static final String TAG = "WhisperHelper";
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

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
                File audioFile = new File(aiPrompt.getFilePath());
                if (!audioFile.exists()) {
                    OctoLogging.e(TAG, "Audio file does not exist: " + aiPrompt.getFilePath());
                    callback.onFailed();
                    return;
                }

                // Read audio file
                byte[] audioData;
                try (FileInputStream fis = new FileInputStream(audioFile)) {
                    audioData = new byte[(int) audioFile.length()];
                    int readBytes = fis.read(audioData);
                    if (readBytes != audioData.length) {
                        callback.onFailed();
                        return;
                    }
                }

                // Create multipart request
                MultipartHTTPRequest request = new MultipartHTTPRequest(WHISPER_API_URL);
                request.addHeader("Authorization", "Bearer " + getApiKey());
                
                // Determine file extension and mime type
                String fileName = audioFile.getName();
                String mimeType = aiPrompt.getMimeType();
                if (mimeType == null || mimeType.isEmpty()) {
                    // Fallback to determining from file extension
                    if (fileName.endsWith(".ogg")) {
                        mimeType = "audio/ogg";
                    } else if (fileName.endsWith(".mp3")) {
                        mimeType = "audio/mpeg";
                    } else if (fileName.endsWith(".wav")) {
                        mimeType = "audio/wav";
                    } else if (fileName.endsWith(".m4a")) {
                        mimeType = "audio/mp4";
                    } else {
                        mimeType = "audio/ogg"; // Default for Telegram voice messages
                    }
                }

                request.addFilePart("file", fileName, mimeType, audioData);
                request.addFormField("model", OctoConfig.INSTANCE.aiFeaturesWhisperModel.getValue());
                
                // Add optional parameters if provided in prompt
                if (!aiPrompt.getPrompt().isEmpty()) {
                    request.addFormField("prompt", aiPrompt.getPrompt());
                }

                String responseBody = request.execute();
                
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.has("text")) {
                    String transcription = jsonResponse.getString("text");
                    if (transcription == null || transcription.trim().isEmpty()) {
                        callback.onEmptyResponse();
                    } else {
                        callback.onSuccess(transcription.trim());
                    }
                } else {
                    OctoLogging.e(TAG, "No text field in Whisper API response: " + responseBody);
                    callback.onFailed();
                }

            } catch (StandardHTTPRequest.Http429Exception e) {
                OctoLogging.e(TAG, "Whisper API rate limit: " + e.getMessage(), e);
                callback.onTooManyRequests();
            } catch (IOException e) {
                OctoLogging.e(TAG, "IO error during Whisper request: " + e.getMessage(), e);
                callback.onFailed();
            } catch (Exception e) {
                OctoLogging.e(TAG, "Error during Whisper transcription: " + e.getMessage(), e);
                callback.onFailed();
            }
        }).start();
    }

    private static String getApiKey() {
        return OctoConfig.INSTANCE.aiFeaturesWhisperAPIKey.getValue().replaceAll(" ", "").trim();
    }

    public static boolean isAvailable() {
        return OctoConfig.INSTANCE.aiFeaturesUseWhisperAPI.getValue() && !getApiKey().isEmpty();
    }
}
