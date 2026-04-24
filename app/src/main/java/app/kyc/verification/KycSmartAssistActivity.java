package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class KycSmartAssistActivity extends AppCompatActivity {

    private java.util.List<ChatMessage> chatList = new java.util.ArrayList<>();
    private ChatAdapter adapter;
    private androidx.recyclerview.widget.RecyclerView rvChat;
    private android.widget.EditText etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_kyc_smart_assist);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_assist), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // Set light status bar
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FAF8FF"));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        findViewById(R.id.toolbarAssist).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Setup Chat
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etChatMessage);
        adapter = new ChatAdapter(chatList);
        rvChat.setAdapter(adapter);

        // Initial Greeting with Status Awareness
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String status = prefs.getString("KYC_STATUS", "NONE");
        String greeting = "Hi! I'm your KYC SmartAssist. How can I help you today?";

        if ("PENDING".equals(status)) {
            greeting = "Hi! I've checked your status: Your application is currently UNDER REVIEW. We are verifying your documents manually. This usually takes 5-10 minutes. Anything else I can help with?";
        } else if ("APPROVED".equals(status)) {
            greeting = "Great news! Your account is FULLY VERIFIED. You now have access to all premium features. How can I assist you further?";
        } else if ("DECLINED".equals(status)) {
            greeting = "I see your application was DECLINED. Don't worry, you can fix this! Please check the red highlights on your dashboard for the exact reasons. Ready to fix it?";
        }

        addBotMessage(greeting);

        // Setup Buttons
        findViewById(R.id.btnSendChat).setOnClickListener(v -> sendMessage());

        findViewById(R.id.chipWhyRejected).setOnClickListener(v -> {
            addUserMessage("Why is my KYC rejected?");
            addBotMessage(
                    "Common reasons for rejection include blurry photos, mismatched names, or expired documents. Please check the items highlighted in red on your dashboard.");
        });

        findViewById(R.id.chipHowLong).setOnClickListener(v -> {
            addUserMessage("How long does it take?");
            addBotMessage(
                    "Standard verification takes about 5-10 minutes, but it can take up to 24 hours during peak times.");
        });

        findViewById(R.id.btnToolbarGuide).setOnClickListener(v -> showKycGuide());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationDashboard);
        bottomNav.setSelectedItemId(R.id.nav_assist);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_identity) {
                startActivity(new Intent(this, KycDashboardActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return true;
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (!msg.isEmpty()) {
            addUserMessage(msg);
            etMessage.setText("");

            // Mock AI delay
            new android.os.Handler().postDelayed(() -> {
                addBotMessage("I've received your query about \"" + msg + "\". One of our agents will get back to you");
            }, 1000);
        }
    }

    private void addUserMessage(String text) {
        chatList.add(new ChatMessage(text, false));
        adapter.notifyItemInserted(chatList.size() - 1);
        rvChat.scrollToPosition(chatList.size() - 1);
    }

    private void addBotMessage(String text) {
        chatList.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(chatList.size() - 1);
        rvChat.scrollToPosition(chatList.size() - 1);
    }

    private void showKycGuide() {
        String guide = "1. Lighting: Ensure you are in a well-lit room. Avoid glare on ID cards.\n\n" +
                "2. Framing: Keep the ID card within the frame. Don't cut off corners.\n\n" +
                "3. Stability: Hold your phone steady to prevent blur.\n\n" +
                "4. Details: Double-check that your Name and Date of Birth match your ID exactly.\n\n" +
                "5. Selfie: Remove glasses or hats for the selfie photo.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("KYC Approval Guide")
                .setMessage(guide)
                .setPositiveButton("Got it", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
