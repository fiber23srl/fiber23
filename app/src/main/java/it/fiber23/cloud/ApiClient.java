package it.fiber23.cloud;

import android.content.ContentResolver;
import android.net.Uri;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class ApiClient {
    static final String DEFAULT_API_URL = "https://cloud.fiber23.it/mobile_api.php";

    private final String apiUrl;
    private final String token;

    ApiClient(String apiUrl, String token) {
        this.apiUrl = apiUrl == null || apiUrl.trim().isEmpty() ? DEFAULT_API_URL : apiUrl.trim();
        this.token = token == null ? "" : token;
    }

    JSONObject login(String email, String password, String deviceName) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("email", email);
        fields.put("password", password);
        fields.put("device_name", deviceName);
        return postForm("login", fields);
    }

    JSONObject me() throws Exception {
        return get("me");
    }

    JSONObject upload(ContentResolver resolver, Uri uri, String fileName, String mimeType, boolean autoBackup) throws Exception {
        String boundary = "F23-" + UUID.randomUUID();
        HttpURLConnection connection = open("upload", "POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setDoOutput(true);

        try (OutputStream out = connection.getOutputStream()) {
            writeField(out, boundary, "auto_backup", autoBackup ? "1" : "0");
            writeFile(out, boundary, "file", fileName, mimeType, resolver.openInputStream(uri));
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        return readJson(connection);
    }

    private JSONObject get(String action) throws Exception {
        return readJson(open(action, "GET"));
    }

    private JSONObject postForm(String action, Map<String, String> fields) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
            body.append('=');
            body.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = open(action, "POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(bytes);
        }
        return readJson(connection);
    }

    private HttpURLConnection open(String action, String method) throws Exception {
        URL url = new URL(apiUrl + "?action=" + action);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(0);
        connection.setRequestProperty("Accept", "application/json");
        if (!token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        return connection;
    }

    private JSONObject readJson(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = readAll(stream);
        JSONObject json = new JSONObject(body);
        if (status >= 400 || !json.optBoolean("ok", false)) {
            throw new Exception(json.optString("error", "Errore cloud"));
        }
        return json;
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    private void writeField(OutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFile(OutputStream out, String boundary, String name, String fileName, String mimeType, InputStream input) throws Exception {
        if (input == null) throw new Exception("File non leggibile");
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + safeFileName(fileName) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + (mimeType == null ? "application/octet-stream" : mimeType) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byte[] buffer = new byte[1024 * 256];
        int read;
        try (InputStream in = input) {
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String safeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "media.bin";
        return name.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    static String humanBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(java.util.Locale.ITALY, unit == 0 ? "%.0f %s" : "%.2f %s", value, units[unit]);
    }
}
