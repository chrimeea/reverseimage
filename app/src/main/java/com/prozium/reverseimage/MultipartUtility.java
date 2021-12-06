package com.prozium.reverseimage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cristian on 02.08.2016.
 */
public class MultipartUtility {

    final String boundary = "===" + System.currentTimeMillis() + "===";
    static final String LINE_FEED = "\r\n";
    static final int BUFFER_SIZE = 8192;
    final HttpURLConnection httpConn;
    final OutputStream outputStream;
    final PrintWriter writer;
    long upload, download;

    public MultipartUtility(final String requestURL) throws IOException {
        setUserAgent();
        final URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoOutput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
    }

    void setUserAgent() {
        String[] agents = new String[] {
                "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19",
                "Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12H321 Safari/600.1.4",
                "Mozilla/5.0 (Linux; U; Android 4.1.1; he-il; Nexus 7 Build/JRO03D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30",
                "Mozilla/5.0 (Linux; Android 4.2.2; SGH-I547 Build/JDQ39) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.96 Mobile Safari/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B440 Safari/600.1.4",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_2 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Mobile/11D257"};
        System.setProperty("http.agent", agents[new Random().nextInt(agents.length)]);
    }

    public void addFilePart(final String fieldName, final String fileName, final byte[] data) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("--" + boundary).append(LINE_FEED);
        sb.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
        sb.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
        sb.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        sb.append(LINE_FEED);
        writer.append(sb);
        writer.flush();
        outputStream.write(data);
        outputStream.flush();
        writer.append(LINE_FEED);
        writer.flush();
        upload += sb.length() + data.length + LINE_FEED.length();
    }

    public String finish(final Pattern pattern) throws IOException {
        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();
        upload += 2 * LINE_FEED.length() + boundary.length();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()), BUFFER_SIZE);
        String line;
        String response = "???";
        while ((line = reader.readLine()) != null) {
            download += line.length() + 1;
            final Matcher m = pattern.matcher(line);
            if (m.find()) {
                response = m.group(1);
                break;
            }
        }
        reader.close();
        httpConn.disconnect();
        download = BUFFER_SIZE * (download / BUFFER_SIZE + 1);
        return response;
    }
}