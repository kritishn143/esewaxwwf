package app.kyc.verification;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import java.util.UUID;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import android.content.SharedPreferences;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup start button
        MaterialButton btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, KycDashboardActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
        });

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup notifications
        findViewById(R.id.btnNotifications).setOnClickListener(v -> showNotificationsSheet());
        updateNotifBadge();

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationDashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_identity) {
                startActivity(new Intent(this, KycDashboardActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_assist) {
                startActivity(new Intent(this, KycSmartAssistActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });

        // Entrance animations
        runEntranceAnimations();
    }

    private void runEntranceAnimations() {
        View heroImage = findViewById(R.id.heroImage);
        View btnStart = findViewById(R.id.btnStart);

        // Hero image fade + slide up
        heroImage.setAlpha(0f);
        heroImage.setTranslationY(60f);
        ObjectAnimator heroFade = ObjectAnimator.ofFloat(heroImage, View.ALPHA, 0f, 1f);
        ObjectAnimator heroSlide = ObjectAnimator.ofFloat(heroImage, View.TRANSLATION_Y, 60f, 0f);
        AnimatorSet heroSet = new AnimatorSet();
        heroSet.playTogether(heroFade, heroSlide);
        heroSet.setDuration(600);
        heroSet.setInterpolator(new DecelerateInterpolator());
        heroSet.setStartDelay(100);
        heroSet.start();

        // Button scale in
        btnStart.setAlpha(0f);
        btnStart.setScaleX(0.9f);
        btnStart.setScaleY(0.9f);
        ObjectAnimator btnFade = ObjectAnimator.ofFloat(btnStart, View.ALPHA, 0f, 1f);
        ObjectAnimator btnScaleX = ObjectAnimator.ofFloat(btnStart, View.SCALE_X, 0.9f, 1f);
        ObjectAnimator btnScaleY = ObjectAnimator.ofFloat(btnStart, View.SCALE_Y, 0.9f, 1f);
        AnimatorSet btnSet = new AnimatorSet();
        btnSet.playTogether(btnFade, btnScaleX, btnScaleY);
        btnSet.setDuration(400);
        btnSet.setInterpolator(new DecelerateInterpolator());
        btnSet.setStartDelay(450);
        btnSet.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Generate pseudo-auth UID if not exists
        android.content.SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        if (!prefs.contains("DEVICE_UID")) {
            prefs.edit().putString("DEVICE_UID", UUID.randomUUID().toString()).apply();
        }
        
        fetchServerStatus();
        updateNotifBadge();
    }

    private void fetchServerStatus() {
        android.content.SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String uid = prefs.getString("DEVICE_UID", null);
        if (uid == null) return;

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Config.API_USER_STATUS + uid)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {}

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        String status = json.getString("status");
                        
                        String oldStatus = prefs.getString("KYC_STATUS", "NONE");
                        if (!status.equals(oldStatus)) {
                            prefs.edit()
                                .putString("KYC_STATUS", status)
                                .putBoolean("NOTIF_UNREAD", true)
                                .apply();
                            runOnUiThread(() -> updateNotifBadge());
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }
    private void showNotificationsSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_notifications_sheet, null);
        android.widget.LinearLayout list = view.findViewById(R.id.llNotificationsList);

        android.content.SharedPreferences sp = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String status = sp.getString("KYC_STATUS", "NONE");

        if ("APPROVED".equals(status)) {
            addNotificationItem(list, "KYC Approved", "Congratulations! Your identity has been verified.", R.drawable.ic_check_circle, "#10B981", () -> {
                startActivity(new Intent(this, KycDashboardActivity.class));
                dialog.dismiss();
            });
        } else if ("DECLINED".equals(status)) {
            addNotificationItem(list, "KYC Rejected", "Your application was declined. Click to see details.", R.drawable.ic_flash, "#EF4444", () -> {
                startActivity(new Intent(this, KycDashboardActivity.class));
                dialog.dismiss();
            });
        } else if ("PENDING".equals(status)) {
            addNotificationItem(list, "Application Sent", "Your KYC is currently under review. We'll notify you soon.", R.drawable.ic_security, "#F59E0B", null);
        }

        addNotificationItem(list, "Welcome to eVA", "Start your verification process to unlock all features.", R.drawable.ic_smart_assist, "#004AC6", () -> {
            startActivity(new Intent(this, KycSmartAssistActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();

        // Mark as read
        sp.edit().putBoolean("NOTIF_UNREAD", false).apply();
        updateNotifBadge();
    }

    private void updateNotifBadge() {
        android.content.SharedPreferences sp = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        boolean unread = sp.getBoolean("NOTIF_UNREAD", true); // Default to true for new installs
        findViewById(R.id.viewNotifBadge).setVisibility(unread ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void addNotificationItem(android.widget.LinearLayout container, String title, String msg, int iconRes, String iconColor, Runnable onClick) {
        android.view.View item = getLayoutInflater().inflate(R.layout.item_notification, null);
        ((android.widget.TextView) item.findViewById(R.id.tvNotifTitle)).setText(title);
        ((android.widget.TextView) item.findViewById(R.id.tvNotifMessage)).setText(msg);
        
        android.widget.ImageView icon = item.findViewById(R.id.ivNotifIcon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(iconColor)));
        
        if (onClick != null) {
            item.setOnClickListener(v -> onClick.run());
        }
        
        container.addView(item);
    }
}
