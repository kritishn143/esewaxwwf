package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_profile), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationDashboard);
        bottomNav.setSelectedItemId(R.id.nav_profile);
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
                Intent intent = new Intent(this, KycDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_assist) {
                Intent intent = new Intent(this, KycSmartAssistActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        
        // Load local data first for immediate display
        String fullName = prefs.getString("FULL_NAME", "Guest User");
        String address = prefs.getString("ADDRESS", "Not provided");
        
        ((TextView) findViewById(R.id.tvProfileName)).setText(fullName);
        ((TextView) findViewById(R.id.tvProfileAddress)).setText(address);
        
        // Load local selfie image
        File selfieFile = new File(getExternalFilesDir(null), "selfie.jpg");
        if (selfieFile.exists()) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(selfieFile.getAbsolutePath());
            ((ShapeableImageView) findViewById(R.id.ivProfileSelfie)).setImageBitmap(bitmap);
        }

        // Fetch verification status from server
        String uid = prefs.getString("DEVICE_UID", null);
        if (uid != null) {
            fetchServerStatus(uid);
        }
    }

    private void fetchServerStatus(String uid) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Config.API_USER_STATUS + uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Ignore network errors, keep local data
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body);
                        String status = json.getString("status");
                        
                        // If admin edited data, we could sync it here
                        String serverName = json.optString("fullName", "");
                        String serverAddress = json.optString("address", "");

                        runOnUiThread(() -> {
                            if (!serverName.isEmpty()) {
                                ((TextView) findViewById(R.id.tvProfileName)).setText(serverName);
                            }
                            if (!serverAddress.isEmpty()) {
                                ((TextView) findViewById(R.id.tvProfileAddress)).setText(serverAddress);
                            }

                            TextView tvStatus = findViewById(R.id.tvProfileStatus);
                            ImageView ivVerified = findViewById(R.id.ivVerifiedBadge);

                             if ("APPROVED".equals(status)) {
                                 tvStatus.setText("Verified User");
                                 tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));
                                 ivVerified.setImageResource(R.drawable.ic_verified_green);
                                 ivVerified.setImageTintList(null);
                                 ivVerified.setVisibility(View.VISIBLE);
                             } else if ("DECLINED".equals(status)) {
                                tvStatus.setText("Verification Failed");
                                tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                                ivVerified.setVisibility(View.GONE);
                            } else if ("PENDING".equals(status)) {
                                tvStatus.setText("Verification Pending");
                                tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"));
                                ivVerified.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
