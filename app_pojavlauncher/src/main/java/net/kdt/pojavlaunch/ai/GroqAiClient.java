package net.kdt.pojavlaunch.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GroqAiClient {
    private static final String ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions";

    /*
     * Keep the model in one place so it can be changed later without
     * touching the UI or networking code.
     */
    private static final String MODEL = "openai/gpt-oss-120b";

    private GroqAiClient() {}

    public static String chat(String apiKey, String message) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(45000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + apiKey
            );
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("temperature", 0.7);
            body.put("max_tokens", 700);

            JSONArray messages = new JSONArray();

            JSONObject system = new JSONObject();
            system.put(
                    "role",
                    "system"
            );
            system.put(
                    "content",
                    "You are the AI assistant inside ABG XS Launcher. " +
                    "Be concise, helpful, natural, and honest when unsure."
            );
            messages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", message);
            messages.put(user);

            body.put("messages", messages);

            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(data);
            }

            int status = connection.getResponseCode();

            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (status < 200 || status >= 300) {
                try {
                    JSONObject error = new JSONObject(response);
                    JSONObject errorObject = error.optJSONObject("error");

                    if (errorObject != null) {
                        String messageText =
                                errorObject.optString("message", null);

                        if (messageText != null && !messageText.isEmpty()) {
                            throw new Exception(messageText);
                        }
                    }
                } catch (Exception ignored) {
                    // Fall through to the generic HTTP error below.
                }

                throw new Exception(
                        "Groq request failed (HTTP " + status + ")"
                );
            }

            JSONObject json = new JSONObject(response);
            JSONArray choices = json.optJSONArray("choices");

            if (choices == null || choices.length() == 0) {
                throw new Exception("Groq returned no response.");
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject responseMessage = choice.optJSONObject("message");

            if (responseMessage == null) {
                throw new Exception("Groq returned an invalid response.");
            }

            String content = responseMessage.optString("content", "");

            if (content.trim().isEmpty()) {
                throw new Exception("Groq returned an empty response.");
            }

            return content.trim();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}
