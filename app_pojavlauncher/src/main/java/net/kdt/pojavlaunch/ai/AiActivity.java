package net.kdt.pojavlaunch.ai;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private LinearLayout root;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText input;
    private ImageButton sendButton;
    private ProgressBar typingSpinner;
    private TextView typingText;

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String key = null;

        try {
            key = GroqKeyStore.load(this);
        } catch (Exception ignored) {
        }

        if (key == null || key.trim().isEmpty()) {
            showKeyScreen();
        } else {
            showChatScreen();
        }
    }

    private GradientDrawable background(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView label(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(10));

        root.setBackgroundColor(Color.rgb(10, 11, 18));

        setContentView(root);
    }

    private void showKeyScreen() {
        setupRoot();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(22));
        card.setBackground(background(Color.rgb(24, 25, 36), 24));

        TextView icon = label("✦", 42, Color.rgb(150, 120, 255));
        icon.setGravity(Gravity.CENTER);

        TextView title = label("ABG XS AI", 27, Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView description = label(
                "استخدم مفتاح Groq الخاص بك للاتصال المباشر بالذكاء الاصطناعي.",
                14,
                Color.rgb(185, 187, 202)
        );
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(8), 0, dp(18));

        EditText keyInput = new EditText(this);
        keyInput.setHint("مفتاح الذكاء الاصطناعي");
        keyInput.setHintTextColor(Color.rgb(125, 127, 143));
        keyInput.setTextColor(Color.WHITE);
        keyInput.setSingleLine(true);
        keyInput.setPadding(dp(16), 0, dp(16), 0);
        keyInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        keyInput.setBackground(
                background(Color.rgb(34, 35, 49), 16)
        );

        TextView save = label("تشغيل الذكاء الاصطناعي", 15, Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setPadding(0, dp(14), 0, dp(14));
        save.setBackground(
                background(Color.rgb(112, 82, 220), 16)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        cardParams.gravity = Gravity.CENTER_VERTICAL;

        root.addView(card, cardParams);

        card.addView(icon);
        card.addView(title);
        card.addView(description);
        card.addView(
                keyInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                );
        saveParams.topMargin = dp(14);

        card.addView(save, saveParams);

        save.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();

            if (key.isEmpty()) {
                keyInput.setError("أدخل مفتاح Groq");
                return;
            }

            save.setEnabled(false);
            save.setAlpha(0.6f);

            try {
                GroqKeyStore.save(this, key);
                showChatScreen();
            } catch (Exception e) {
                save.setEnabled(true);
                save.setAlpha(1f);

                Toast.makeText(
                        this,
                        "تعذر حفظ المفتاح",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void showChatScreen() {
        setupRoot();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(6), dp(5), dp(4), dp(12));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = label(
                "ABG XS AI",
                22,
                Color.WHITE
        );

        TextView subtitle = label(
                "Groq • اتصال مباشر",
                12,
                Color.rgb(145, 148, 165)
        );

        titleBox.addView(title);
        titleBox.addView(subtitle);

        ImageButton clearButton = iconButton("⌫");
        ImageButton settingsButton = iconButton("⚙");

        header.addView(
                titleBox,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        header.addView(clearButton);
        header.addView(settingsButton);

        root.addView(header);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, dp(8));

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(0, dp(4), 0, dp(10));

        scrollView.addView(chatContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        addAiMessage(
                "أهلاً 👋\nأنا ABG XS AI. اكتب أي حاجة وخلينا نبدأ."
        );

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(2), dp(8), dp(2), dp(18));

        LinearLayout inputBox = new LinearLayout(this);
        inputBox.setGravity(Gravity.CENTER_VERTICAL);
        inputBox.setPadding(dp(14), 0, dp(6), 0);
        inputBox.setBackground(
                background(Color.rgb(25, 26, 38), 22)
        );

        input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setHintTextColor(Color.rgb(115, 118, 135));
        input.setTextColor(Color.WHITE);
        input.setTextSize(15);
        input.setMaxLines(4);
        input.setSingleLine(false);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        input.setBackgroundColor(Color.TRANSPARENT);

        sendButton = iconButton("➤");

        inputBox.addView(
                input,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        inputBox.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)
                )
        );

        composer.addView(
                inputBox,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(composer);

        createTypingIndicator();

        settingsButton.setOnClickListener(v -> showKeyScreen());

        clearButton.setOnClickListener(v -> {
            chatContainer.removeAllViews();
            addAiMessage("المحادثة اتمسحت ✨");
        });

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private ImageButton iconButton(String icon) {
        ImageButton button = new ImageButton(this);

        button.setBackground(
                background(Color.rgb(45, 43, 65), 18)
        );
        button.setColorFilter(Color.WHITE);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));

        android.graphics.Bitmap bitmap =
                android.graphics.Bitmap.createBitmap(
                        dp(44),
                        dp(44),
                        android.graphics.Bitmap.Config.ARGB_8888
                );

        android.graphics.Canvas canvas =
                new android.graphics.Canvas(bitmap);

        android.graphics.Paint paint =
                new android.graphics.Paint(
                        android.graphics.Paint.ANTI_ALIAS_FLAG
                );

        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(19));
        paint.setTextAlign(
                android.graphics.Paint.Align.CENTER
        );

        android.graphics.Paint.FontMetrics metrics =
                paint.getFontMetrics();

        float baseline =
                dp(22) -
                (metrics.ascent + metrics.descent) / 2f;

        canvas.drawText(
                icon,
                dp(22),
                baseline,
                paint
        );

        button.setImageBitmap(bitmap);
        button.setContentDescription(icon);

        button.setOnTouchListener((v, event) -> {
            if (event.getAction() ==
                    android.view.MotionEvent.ACTION_DOWN) {

                v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(70)
                        .start();

            } else if (
                    event.getAction() ==
                            android.view.MotionEvent.ACTION_UP ||
                    event.getAction() ==
                            android.view.MotionEvent.ACTION_CANCEL
            ) {

                v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
            }

            return false;
        });

        return button;
    }

    private void createTypingIndicator() {
        typingText = label(
                "ABG XS AI يفكر...",
                12,
                Color.rgb(150, 153, 172)
        );

        typingSpinner = new ProgressBar(this);
        typingSpinner.setVisibility(View.GONE);

        root.addView(typingSpinner);

        typingText.setVisibility(View.GONE);
        root.addView(typingText);
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        input.setText("");

        addUserMessage(message);

        if (LauncherAiActions.tryHandle(this, message)) {
            return;
        }

        sendButton.setEnabled(false);
        sendButton.setAlpha(0.5f);

        typingSpinner.setVisibility(View.VISIBLE);
        typingText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            String result;

            try {
                String key = GroqKeyStore.load(this);

                if (key == null || key.trim().isEmpty()) {
                    throw new Exception(
                            "مفتاح الذكاء الاصطناعي غير موجود."
                    );
                }

                result = GroqAiClient.chat(key, message);
            } catch (Exception e) {
                result = "حصل خطأ:\n" +
                        (e.getMessage() == null
                                ? "تعذر الاتصال بـ Groq."
                                : e.getMessage());
            }

            String finalResult = result;

            runOnUiThread(() -> {
                typingSpinner.setVisibility(View.GONE);
                typingText.setVisibility(View.GONE);

                addAiMessage(finalResult);

                sendButton.setEnabled(true);
                sendButton.setAlpha(1f);
            });
        });
    }

    private void addUserMessage(String message) {
        addBubble(
                "أنت",
                message,
                true
        );
    }

    private void addAiMessage(String message) {
        addBubble(
                "ABG XS AI",
                message,
                false
        );
    }

    private void addBubble(
            String sender,
            String message,
            boolean user
    ) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        TextView senderView = label(
                sender,
                11,
                Color.rgb(145, 148, 166)
        );

        TextView messageView = label(
                message,
                15,
                Color.WHITE
        );

        messageView.setPadding(
                dp(14),
                dp(11),
                dp(14),
                dp(11)
        );

        messageView.setBackground(
                background(
                        user
                                ? Color.rgb(83, 66, 151)
                                : Color.rgb(28, 29, 42),
                        18
                )
        );

        wrapper.addView(senderView);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        messageParams.topMargin = dp(4);
        messageParams.gravity =
                user ? Gravity.END : Gravity.START;

        wrapper.addView(messageView, messageParams);

        LinearLayout.LayoutParams wrapperParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        wrapperParams.topMargin = dp(8);

        chatContainer.addView(wrapper, wrapperParams);

        messageView.setAlpha(0f);
        messageView.setTranslationY(dp(8));

        messageView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollView == null) {
            return;
        }

        scrollView.postDelayed(
                () -> scrollView.fullScroll(View.FOCUS_DOWN),
                80
        );
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
