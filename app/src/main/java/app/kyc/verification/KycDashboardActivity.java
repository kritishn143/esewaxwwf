package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class KycDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_kyc_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_dashboard), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        findViewById(R.id.btnBackDashboard).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        MaterialButton btnStart = findViewById(R.id.btnStartDocumentUpload);
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, DocumentCaptureActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Notifications
        findViewById(R.id.btnNotificationsDashboard).setOnClickListener(v -> showNotificationsSheet());
        updateNotifBadge();

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationDashboard);
        bottomNav.setSelectedItemId(R.id.nav_identity);
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
            return true;
        });

        findViewById(R.id.tvWhatIsThis).setOnClickListener(v -> {
            Intent intent = new Intent(this, KycSmartAssistActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchServerStatus();
        updateNotifBadge();
    }

    private void fetchServerStatus() {
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String uid = prefs.getString("DEVICE_UID", null);

        if (uid == null) {
            updateProgressLocal();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Config.API_USER_STATUS + uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> updateProgressLocal());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        String status = json.getString("status");
                        String feedback = json.optString("feedback", "");
                        org.json.JSONArray rejectedArray = json.optJSONArray("rejectedFields");
                        java.util.List<String> rejectedList = new java.util.ArrayList<>();
                        if (rejectedArray != null) {
                            for (int i = 0; i < rejectedArray.length(); i++) {
                                rejectedList.add(rejectedArray.getString(i));
                            }
                        }

                        String oldStatus = prefs.getString("KYC_STATUS", "NONE");
                        if (!status.equals(oldStatus)) {
                            prefs.edit().putBoolean("NOTIF_UNREAD", true).apply();
                        }
                        
                        runOnUiThread(() -> {
                            updateNotifBadge();
                            updateUiForStatus(status, feedback, prefs, rejectedList);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> updateProgressLocal());
                    }
                } else {
                    runOnUiThread(() -> updateProgressLocal());
                }
            }
        });
    }

    private void updateUiForStatus(String status, String feedback, SharedPreferences prefs,
            java.util.List<String> rejectedFields) {
        TextView tvHeader = findViewById(R.id.tvMainHeader);
        TextView tvSubheader = findViewById(R.id.tvMainSubheader);
        TextView tvStepLabel = findViewById(R.id.tvStepLabel);
        ProgressBar progressBar = findViewById(R.id.kycProgressBar);
        MaterialButton btnStart = findViewById(R.id.btnStartDocumentUpload);

        // Ensure all cards are visible for status display
        findViewById(R.id.cardDocumentUpload).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.cardSelfieUpload).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.cardReviewSubmit).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.ivHeaderVerifiedBadge).setVisibility(android.view.View.GONE);

        prefs.edit().putString("KYC_STATUS", status).apply();

        if ("APPROVED".equals(status)) {
            tvHeader.setText("Verified Profile");
            tvSubheader.setText("Your identity has been fully verified.");
            tvStepLabel.setText("Verified");
            progressBar.setProgress(4);
            ImageView ivBadge = findViewById(R.id.ivHeaderVerifiedBadge);
            ivBadge.setImageResource(R.drawable.ic_verified_green);
            ivBadge.setImageTintList(null);
            ivBadge.setVisibility(android.view.View.VISIBLE);

            btnStart.setText("KYC Approved");
            btnStart.setEnabled(false);
            btnStart.setBackgroundColor(android.graphics.Color.parseColor("#10B981"));
            btnStart.setIconResource(R.drawable.ic_verified_green);
            btnStart.setIconTint(null);

            // Mark all items as verified with specific labels
            updateCardStatusUI(findViewById(R.id.cardDocumentUpload), "FRONT & BACK VERIFIED", "#10B981");
            updateCardStatusUI(findViewById(R.id.cardSelfieUpload), "SELFIE VERIFIED", "#10B981");
            updateCardStatusUI(findViewById(R.id.cardReviewSubmit), "DETAILS VERIFIED", "#10B981");
            
            // Explicitly set titles for clarity
            ((TextView) findViewById(R.id.tvStep3Title)).setText("Personal Information");

        } else if ("DECLINED".equals(status)) {
            tvStepLabel.setText("Declined");
            progressBar.setProgress(2);
            ((TextView) findViewById(R.id.tvStep3Title)).setText("Review & Submit");

            // Build a descriptive reason from rejected fields
            StringBuilder reasonBuilder = new StringBuilder("Issue: ");
            if (rejectedFields.contains("frontImage")) reasonBuilder.append("ID Front, ");
            if (rejectedFields.contains("backImage")) reasonBuilder.append("ID Back, ");
            if (rejectedFields.contains("selfieImage")) reasonBuilder.append("Selfie, ");
            if (rejectedFields.contains("details")) reasonBuilder.append("Personal Info, ");
            String detailedReason = reasonBuilder.toString().replaceAll(", $", "");

            String subinnerText = detailedReason;
            if (!feedback.isEmpty()) {
                subinnerText += "\nAdmin Feedback: " + feedback;
            }
            tvSubheader.setText(subinnerText);
            tvSubheader.setTextColor(android.graphics.Color.RED);

            // Store rejected fields
            java.util.HashSet<String> rejectedSet = new java.util.HashSet<>(rejectedFields);
            prefs.edit().putStringSet("REJECTED_FIELDS", rejectedSet).apply();

            btnStart.setText("Fix Rejected Items");
            btnStart.setEnabled(true);
            btnStart.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"));
            btnStart.setIconResource(R.drawable.ic_flash);
            findViewById(R.id.tvWhatIsThis).setVisibility(android.view.View.VISIBLE);

            // Highlight exactly what was rejected
            if (rejectedFields.contains("frontImage")) {
                updateCardStatusUI(findViewById(R.id.cardIdFront), "REJECTED", "#EF4444");
                prefs.edit().putBoolean("FRONT_CAPTURED", false).apply();
            }
            if (rejectedFields.contains("backImage")) {
                updateCardStatusUI(findViewById(R.id.cardIdBack), "REJECTED", "#EF4444");
                prefs.edit().putString("ADDRESS", "").apply();
            }
            if (rejectedFields.contains("selfieImage")) {
                updateCardStatusUI(findViewById(R.id.cardSelfieUpload), "REJECTED", "#EF4444");
            }
            if (rejectedFields.contains("details")) {
                prefs.edit().putString("FULL_NAME", "").putString("NIN", "").apply();
            }

            btnStart.setOnClickListener(v -> {
                if (rejectedFields.contains("frontImage") || rejectedFields.contains("backImage")) {
                    startActivity(new Intent(this, DocumentCaptureActivity.class));
                } else if (rejectedFields.contains("selfieImage")) {
                    startActivity(new Intent(this, SelfieVerificationActivity.class));
                } else {
                    startActivity(new Intent(this, ReviewSubmitActivity.class));
                }
            });
        } else if ("PENDING".equals(status)) {
            tvHeader.setText("Under Review");
            tvSubheader.setText("Your documents are currently being manually reviewed by an admin.");
            tvStepLabel.setText("Pending");
            progressBar.setProgress(4);

            btnStart.setText("Application Pending...");
            btnStart.setEnabled(false);
            btnStart.setBackgroundColor(android.graphics.Color.parseColor("#F59E0B"));
            btnStart.setIconResource(R.drawable.ic_security);

            // Mark all items as under review with specific labels
            updateCardStatusUI(findViewById(R.id.cardDocumentUpload), "DOCUMENTS IN REVIEW", "#F59E0B");
            updateCardStatusUI(findViewById(R.id.cardSelfieUpload), "SELFIE IN REVIEW", "#F59E0B");
            updateCardStatusUI(findViewById(R.id.cardReviewSubmit), "DETAILS IN REVIEW", "#F59E0B");
            
            ((TextView) findViewById(R.id.tvStep3Title)).setText("Personal Information");
            findViewById(R.id.tvWhatIsThis).setVisibility(android.view.View.VISIBLE);
        }

        // Always show the documents if they exist
        updateProgressLocal();
        
        // Show personal details in Step 3 if submitted
        if (!"NONE".equals(status)) {
            findViewById(R.id.llStep3Details).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.tvStep3Sub).setVisibility(android.view.View.GONE);
            
            ((TextView) findViewById(R.id.tvStep3Name)).setText("Name: " + prefs.getString("FULL_NAME", "—"));
            ((TextView) findViewById(R.id.tvStep3Id)).setText("NIN: " + prefs.getString("NIN", "—"));
        }
    }

    private void updateProgressLocal() {
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        boolean frontCaptured = prefs.getBoolean("FRONT_CAPTURED", false);
        // back is done if address was extracted
        boolean backCaptured = frontCaptured && !prefs.getString("ADDRESS", "").isEmpty();
        java.io.File selfieFile = new java.io.File(getExternalFilesDir(null), "selfie.jpg");
        boolean selfieCaptured = backCaptured && selfieFile.exists();

        int step;
        String stepLabel;
        String buttonText;

        if (selfieCaptured) {
            step = 4;
            stepLabel = "Step 4 of 4";
            buttonText = "Review & Submit";
        } else if (backCaptured) {
            step = 3;
            stepLabel = "Step 3 of 4";
            buttonText = "Continue to Selfie";
        } else if (frontCaptured) {
            step = 2;
            stepLabel = "Step 2 of 4";
            buttonText = "Continue (Back Side)";
        } else {
            step = 1;
            stepLabel = "Step 1 of 4";
            buttonText = "Start ID Capture";
        }

        TextView tvStepLabel = findViewById(R.id.tvStepLabel);
        ProgressBar progressBar = findViewById(R.id.kycProgressBar);
        String currentStatusForLabel = prefs.getString("KYC_STATUS", "NONE");

        if ("NONE".equals(currentStatusForLabel)) {
            if (tvStepLabel != null)
                tvStepLabel.setText(stepLabel);
            if (progressBar != null)
                progressBar.setProgress(step);
        }

        MaterialButton btnStart = findViewById(R.id.btnStartDocumentUpload);
        String currentStatus = prefs.getString("KYC_STATUS", "NONE");
        if (btnStart != null && "NONE".equals(currentStatus))
            btnStart.setText(buttonText);

        // Update Card Visual States
        updateCardState(findViewById(R.id.cardDocumentUpload), findViewById(R.id.tvStatusStep1), step >= 1, step > 1);
        updateCardState(findViewById(R.id.cardSelfieUpload), findViewById(R.id.tvStatusStep2), step >= 3, step > 3);
        updateCardState(findViewById(R.id.cardReviewSubmit), findViewById(R.id.tvStatusStep3), step >= 4, step > 4);

        // Load Selfie thumbnail if exists
        if (selfieFile.exists()) {
            ImageView ivSelfie = findViewById(R.id.ivSelfiePreview);
            if (ivSelfie != null) {
                ivSelfie.setVisibility(android.view.View.VISIBLE);
                findViewById(R.id.tvStep2Number).setVisibility(android.view.View.GONE);
                ivSelfie.setImageBitmap(android.graphics.BitmapFactory.decodeFile(selfieFile.getAbsolutePath()));
                ivSelfie.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }

        // Highlight Front/Back sub-cards
        com.google.android.material.card.MaterialCardView cardFront = findViewById(R.id.cardIdFront);
        com.google.android.material.card.MaterialCardView cardBack = findViewById(R.id.cardIdBack);

        if (frontCaptured) {
            cardFront.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981")));
            cardFront.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F0FDF4")));

            // Show thumbnail
            File frontFile = new File(getExternalFilesDir(null), "id_card_front.jpg");
            if (frontFile.exists()) {
                ImageView ivFront = findViewById(R.id.ivIdFront);
                ivFront.setImageBitmap(android.graphics.BitmapFactory.decodeFile(frontFile.getAbsolutePath()));
                ivFront.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivFront.setImageTintList(null);
                ivFront.setImageTintMode(null);
            }
        } else {
            cardFront.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5E7EB")));
            cardFront.setBackgroundTintList(null);
            ((ImageView) findViewById(R.id.ivIdFront)).setImageResource(R.drawable.ic_id_card);
            ((ImageView) findViewById(R.id.ivIdFront)).setImageTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9CA3AF")));
        }

        if (backCaptured) {
            cardBack.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981")));
            cardBack.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F0FDF4")));

            // Show thumbnail
            File backFile = new File(getExternalFilesDir(null), "id_card_back.jpg");
            if (backFile.exists()) {
                ImageView ivBack = findViewById(R.id.ivIdBack);
                ivBack.setImageBitmap(android.graphics.BitmapFactory.decodeFile(backFile.getAbsolutePath()));
                ivBack.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivBack.setImageTintList(null);
                ivBack.setImageTintMode(null);
            }
        } else {
            cardBack.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5E7EB")));
            cardBack.setBackgroundTintList(null);
            ((ImageView) findViewById(R.id.ivIdBack)).setImageResource(R.drawable.ic_badge);
            ((ImageView) findViewById(R.id.ivIdBack)).setImageTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9CA3AF")));
        }

        // Reset visibility just in case
        findViewById(R.id.cardSelfieUpload).setVisibility(View.VISIBLE);
        findViewById(R.id.cardReviewSubmit).setVisibility(View.VISIBLE);
        findViewById(R.id.ivHeaderVerifiedBadge).setVisibility(View.GONE);

        // Show selfie thumbnail if exists
        if (selfieFile != null && selfieFile.exists()) {
            ImageView ivSelfie = findViewById(R.id.ivSelfiePreview);
            ivSelfie.setVisibility(View.VISIBLE);
            ivSelfie.setImageBitmap(android.graphics.BitmapFactory.decodeFile(selfieFile.getAbsolutePath()));
            findViewById(R.id.tvStep2Number).setVisibility(View.GONE);
        } else {
            findViewById(R.id.ivSelfiePreview).setVisibility(View.GONE);
            findViewById(R.id.tvStep2Number).setVisibility(View.VISIBLE);
        }

        // Route button to correct next screen ONLY if not in a special status
        if ("NONE".equals(currentStatus)) {
            btnStart.setOnClickListener(v -> {
                Intent intent;
                if (selfieCaptured) {
                    intent = new Intent(this, ReviewSubmitActivity.class);
                } else {
                    intent = new Intent(this, DocumentCaptureActivity.class);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    private void updateCardState(View card, TextView statusLabel, boolean isActive, boolean isCompleted) {
        if (card == null || statusLabel == null)
            return;

        if (isCompleted) {
            card.setAlpha(1.0f);
            statusLabel.setText("COMPLETED");
            statusLabel.setTextColor(android.graphics.Color.parseColor("#10B981")); // Green
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(0);
                card.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F0FDF4")));
            }
        } else if (isActive) {
            card.setAlpha(1.0f);
            statusLabel.setText("IN PROGRESS");
            statusLabel.setTextColor(android.graphics.Color.parseColor("#3B82F6")); // Blue
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(4);
                card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            }
        } else {
            card.setAlpha(0.6f);
            statusLabel.setText("PENDING");
            statusLabel.setTextColor(android.graphics.Color.parseColor("#9CA3AF")); // Gray
            if (card instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(0);
                card.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F9FAFB")));
            }
        }
    }

    private void updateCardStatusUI(View card, String statusText, String colorHex) {
        if (card == null) return;
        int color = android.graphics.Color.parseColor(colorHex);
        
        if (card instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView mCard = (com.google.android.material.card.MaterialCardView) card;
            mCard.setStrokeColor(color);
            mCard.setStrokeWidth(4);
            mCard.setAlpha(1.0f);

            int id = card.getId();
            TextView tvStatus = null;
            if (id == R.id.cardDocumentUpload || id == R.id.cardIdFront || id == R.id.cardIdBack) {
                tvStatus = findViewById(R.id.tvStatusStep1);
            } else if (id == R.id.cardSelfieUpload) {
                tvStatus = findViewById(R.id.tvStatusStep2);
            } else if (id == R.id.cardReviewSubmit) {
                tvStatus = findViewById(R.id.tvStatusStep3);
            }

            if (tvStatus != null) {
                tvStatus.setText(statusText);
                tvStatus.setTextColor(color);
            }
        }
    }
    private void showNotificationsSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_notifications_sheet, null);
        android.widget.LinearLayout list = view.findViewById(R.id.llNotificationsList);

        SharedPreferences sp = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String status = sp.getString("KYC_STATUS", "NONE");

        if ("APPROVED".equals(status)) {
            addNotificationItem(list, "KYC Approved", "Congratulations! Your identity has been verified.", R.drawable.ic_check_circle, "#10B981", () -> {
                dialog.dismiss();
                // Stay on dashboard but maybe refresh
            });
        } else if ("DECLINED".equals(status)) {
            addNotificationItem(list, "KYC Rejected", "Your application was declined. Click to see details.", R.drawable.ic_flash, "#EF4444", () -> {
                dialog.dismiss();
            });
        } else if ("PENDING".equals(status)) {
            addNotificationItem(list, "Application Sent", "Your KYC is currently under review. We'll notify you soon.", R.drawable.ic_security, "#F59E0B", null);
        }

        addNotificationItem(list, "Welcome to eVA", "Start your verification process to unlock all features.", R.drawable.ic_smart_assist, "#004AC6", () -> {
            startActivity(new Intent(this, KycSmartAssistActivity.class));
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();

        // Mark as read
        sp.edit().putBoolean("NOTIF_UNREAD", false).apply();
        updateNotifBadge();
    }

    private void updateNotifBadge() {
        SharedPreferences sp = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        boolean unread = sp.getBoolean("NOTIF_UNREAD", true);
        findViewById(R.id.viewNotifBadgeDashboard).setVisibility(unread ? android.view.View.VISIBLE : android.view.View.GONE);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
