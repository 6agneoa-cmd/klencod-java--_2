package com.genwo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "ضع_مفتاحك_هنا";
    private static final String MODEL = "gemini-2.5-flash";

    private LinearLayout chat;
    private ScrollView scroll;
    private EditText input;
    private ProgressBar typing;
    private final List<JSONObject> history = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int BG = Color.parseColor("#0B1220");
    private static final int PANEL = Color.parseColor("#111C33");
    private static final int CYAN = Color.parseColor("#00E5FF");
    private static final int USER_BUBBLE = Color.parseColor("#1565C0");
    private static final int BOT_BUBBLE = Color.parseColor("#1E2A3D");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));
        header.setBackgroundColor(PANEL);

        TextView title = new TextView(this);
        title.setText("🤖 جينو | الذكاء الاصطناعي");
        title.setTextColor(CYAN);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);

        typing = new ProgressBar(this);
        typing.setVisibility(android.view.View.GONE);

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        header.addView(title, titleLp);
        header.addView(typing);
        root.addView(header);

        scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        chat.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroll.addView(chat);
        root.addView(scroll);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(dp(10), dp(10), dp(10), dp(10));
        inputRow.setBackgroundColor(PANEL);

        input = new EditText(this);
        input.setHint("اكتب رسالتك لجينو...");
        input.setHintTextColor(Color.parseColor("#8A97A8"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(15);
        input.setMaxLines(4);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#1A2438"));
        inputBg.setCornerRadius(dp(24));
        input.setBackground(inputBg);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        inputRow.addView(input, inputLp);

        TextView sendBtn = new TextView(this);
        sendBtn.setText("➤");
        sendBtn.setTextSize(24);
        sendBtn.setTextColor(CYAN);
        sendBtn.setGravity(Gravity.CENTER);
        GradientDrawable sendBg = new GradientDrawable();
        sendBg.setColor(Color.parseColor("#1A2438"));
        sendBg.setCornerRadius(dp(26));
        sendBtn.setBackground(sendBg);
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        sendLp.setMargins(dp(10), 0, 0, 0);
        sendBtn.setLayoutParams(sendLp);
        sendBtn.setOnClickListener(v -> send());
        inputRow.addView(sendBtn);

        root.addView(inputRow);
        setContentView(root);

        addBubble("أهلاً! أنا جينو 🤖\nاسألني أي شيء: برمجة، شرح، أفكار، ترجمة، حل مسائل...", false);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        if (API_KEY.startsWith("ضع")) {
            toast("⚠️ ضع مفتاح API في الكود أولاً");
            return;
        }
        input.setText("");
        addBubble(text, true);
        typing.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> askGemini(text)).start();
    }

    private void askGemini(String userText) {
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(buildPayload(userText).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            final String reply;
            if (code >= 200 && code < 300) {
                JSONObject root = new JSONObject(sb.toString());
                reply = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                reply = "⚠️ خطأ من الخادم (" + code + "):\n" + sb;
            }

            handler.post(() -> {
                typing.setVisibility(android.view.View.GONE);
                addBubble(reply, false);
                if (!reply.startsWith("⚠️")) saveHistory(userText, reply);
            });
        } catch (Exception e) {
            handler.post(() -> {
                typing.setVisibility(android.view.View.GONE);
                addBubble("⚠️ خطأ في الاتصال: " + e.getMessage(), false);
            });
        }
    }

    private String buildPayload(String userText) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("system_instruction", new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject()
                        .put("text", "أنت مساعد ذكي عربي اسمك جينو. أجب بالعربية بوضوح ودقة، وكن عملياً ومباشراً."))));

        JSONArray contents = new JSONArray();
        int from = Math.max(0, history.size() - 20);
        for (int i = from; i < history.size(); i++) contents.put(history.get(i));
        contents.put(new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", userText))));
        payload.put("contents", contents);
        return payload.toString();
    }

    private void saveHistory(String userText, String reply) {
        try {
            JSONObject u = new JSONObject();
            u.put("role", "user");
            u.put("parts", new JSONArray().put(new JSONObject().put("text", userText)));
            history.add(u);
            JSONObject m = new JSONObject();
            m.put("role", "model");
            m.put("parts", new JSONArray().put(new JSONObject().put("text", reply)));
            history.add(m);
        } catch (Exception ignored) {}
    }

    private void addBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(dp(18), dp(14), dp(18), dp(14));

        GradientDrawable bubble = new GradientDrawable();
        bubble.setColor(isUser ? USER_BUBBLE : BOT_BUBBLE);
        bubble.setCornerRadius(dp(18));
        tv.setBackground(bubble);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(6), dp(10), dp(6));
        lp.gravity = isUser ? Gravity.END : Gravity.START;
        tv.setLayoutParams(lp);

        chat.addView(tv);
        scroll.post(() -> scroll.fullScroll(android.view.View.FOCUS_DOWN));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
