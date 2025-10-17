/*
 * This is the source code of OctoGram for Android
 * It is licensed under GNU GPL v2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023-2025.
 */

package it.octogram.android.utils.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import it.octogram.android.utils.OctoLogging;

/**
 * Provides functionality for making HTTP multipart/form-data requests.
 * Used primarily for file uploads with OpenAI's Whisper API.
 */
public class MultipartHTTPRequest {
    private static final String TAG = "MultipartHTTPRequest";
    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "----OctoGramBoundary" + System.currentTimeMillis();

    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> formFields = new HashMap<>();
    private final Map<String, FileData> fileParts = new HashMap<>();

    private static class FileData {
        String fileName;
        String mimeType;
        byte[] data;

        FileData(String fileName, String mimeType, byte[] data) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.data = data;
        }
    }

    public MultipartHTTPRequest(String url) {
        this.url = url;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addFormField(String key, String value) {
        formFields.put(key, value);
    }

    public void addFilePart(String fieldName, String fileName, String mimeType, byte[] data) {
        fileParts.put(fieldName, new FileData(fileName, mimeType, data));
    }

    public String execute() throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(30000); // 30 seconds for file upload
            connection.setReadTimeout(30000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Set headers
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            // Build multipart request body
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                // Add form fields
                for (Map.Entry<String, String> entry : formFields.entrySet()) {
                    outputStream.writeBytes("--" + BOUNDARY + LINE_FEED);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINE_FEED);
                    outputStream.writeBytes(LINE_FEED);
                    outputStream.writeBytes(entry.getValue() + LINE_FEED);
                }

                // Add file parts
                for (Map.Entry<String, FileData> entry : fileParts.entrySet()) {
                    FileData fileData = entry.getValue();
                    outputStream.writeBytes("--" + BOUNDARY + LINE_FEED);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + 
                            "\"; filename=\"" + fileData.fileName + "\"" + LINE_FEED);
                    outputStream.writeBytes("Content-Type: " + fileData.mimeType + LINE_FEED);
                    outputStream.writeBytes(LINE_FEED);
                    outputStream.write(fileData.data);
                    outputStream.writeBytes(LINE_FEED);
                }

                // End boundary
                outputStream.writeBytes("--" + BOUNDARY + "--" + LINE_FEED);
                outputStream.flush();
            }

            // Read response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 429) {
                throw new StandardHTTPRequest.Http429Exception();
            }

            InputStream stream;
            if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }

            if (stream == null) {
                OctoLogging.w(TAG, "Response stream is null (Code: " + responseCode + ")");
                throw new IOException("Response stream is null");
            }

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();

            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                OctoLogging.e(TAG, "HTTP error " + responseCode + ": " + response);
                throw new IOException("HTTP error " + responseCode + ": " + response);
            }

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
