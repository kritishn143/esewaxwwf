package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class ReviewSubmitActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_review_submit);

        // root_layout_review exists in activity_review_submit.xml
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_review), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // btnBackReview exists in the review layout toolbar
        findViewById(R.id.btnBackReview).setOnClickListener(v -> finish());

        // Populate from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        String fullName = prefs.getString("FULL_NAME", "—");
        String dob = prefs.getString("DOB", "—");
        String nin = prefs.getString("NIN", "—");
        String nationality = prefs.getString("NATIONALITY", "Nepal");
        String address = prefs.getString("ADDRESS", "—");

        setTextSafe(R.id.tvReviewName, fullName);
        setTextSafe(R.id.tvReviewDob, dob);
        setTextSafe(R.id.tvReviewNin, nin);
        setTextSafe(R.id.tvReviewNationality, nationality);
        setTextSafe(R.id.tvReviewAddress, address);

        // Show error section if critical fields are missing
        boolean hasIssues = fullName.equals("—") || nin.equals("—") || dob.equals("—");
        findViewById(R.id.errorSection).setVisibility(
                hasIssues ? android.view.View.VISIBLE : android.view.View.GONE);

        // Fix Issues → open PersonalInfoActivity
        findViewById(R.id.btnFixIssues)
                .setOnClickListener(v -> startActivity(new Intent(this, PersonalInfoActivity.class)));

        // Submit → go to success screen
        MaterialButton btnSubmit = findViewById(R.id.btnSubmitReview);
        btnSubmit.setOnClickListener(v -> submitToBackend(prefs, btnSubmit));

        // Retake → clear all state, restart capture
        MaterialButton btnRetake = findViewById(R.id.btnRetakeReview);
        btnRetake.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            // Also delete captured image files
            deleteCapture("id_card_front.jpg");
            deleteCapture("id_card_back.jpg");
            deleteCapture("selfie.jpg");
            Intent intent = new Intent(this, DocumentCaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setTextSafe(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null)
            tv.setText(text);
    }

    private void submitToBackend(SharedPreferences prefs, MaterialButton btnSubmit) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        String fullName = prefs.getString("FULL_NAME", "");
        String dob = prefs.getString("DOB", "");
        String nin = prefs.getString("NIN", "");
        String sex = prefs.getString("SEX", "");
        String nationality = prefs.getString("NATIONALITY", "");
        String address = prefs.getString("ADDRESS", "");

        String uid = prefs.getString("DEVICE_UID", "anonymous");

        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("uid", uid)
                .addFormDataPart("fullName", fullName)
                .addFormDataPart("dob", dob)
                .addFormDataPart("nin", nin)
                .addFormDataPart("sex", sex)
                .addFormDataPart("nationality", nationality)
                .addFormDataPart("address", address);

        java.io.File frontFile = new java.io.File(getExternalFilesDir(null), "id_card_front.jpg");
        if (frontFile.exists())
            builder.addFormDataPart("frontImage", "front.jpg",
                    RequestBody.create(MediaType.parse("image/jpeg"), frontFile));

        java.io.File backFile = new java.io.File(getExternalFilesDir(null), "id_card_back.jpg");
        if (backFile.exists())
            builder.addFormDataPart("backImage", "back.jpg",
                    RequestBody.create(MediaType.parse("image/jpeg"), backFile));

        java.io.File selfieFile = new java.io.File(getExternalFilesDir(null), "selfie.jpg");
        if (selfieFile.exists())
            builder.addFormDataPart("selfieImage", "selfie.jpg",
                    RequestBody.create(MediaType.parse("image/jpeg"), selfieFile));

        // Use the actual network IP so this works on physical devices and emulators
        Request request = new Request.Builder()
                .url(Config.API_KYC)
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Verification");
                    Toast.makeText(ReviewSubmitActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                    e.printStackTrace();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            org.json.JSONObject jsonObject = new org.json.JSONObject(responseBody);
                            if (jsonObject.has("id")) {
                                prefs.edit().putString("KYC_SERVER_ID", jsonObject.getString("id")).apply();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        prefs.edit().remove("FRONT_CAPTURED").remove("REJECTED_FIELDS").apply();
                        Intent intent = new Intent(ReviewSubmitActivity.this, SubmissionSuccessActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Verification");
                        Toast.makeText(ReviewSubmitActivity.this, "Server Error: " + response.code(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
        });
    }

    private void deleteCapture(String name) {
        java.io.File f = new java.io.File(getExternalFilesDir(null), name);
        if (f.exists())
            f.delete();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load Images
        loadImageIntoView("id_card_front.jpg", R.id.ivReviewFront);
        loadImageIntoView("id_card_back.jpg", R.id.ivReviewBack);
        loadImageIntoView("selfie.jpg", R.id.ivReviewSelfie);

        // Setup Edit Button
        findViewById(R.id.btnEditData).setOnClickListener(v -> showEditDialog());
    }

    private void loadImageIntoView(String fileName, int imageViewId) {
        java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);
        if (file.exists()) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
            android.widget.ImageView imageView = findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private void showEditDialog() {
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText inputName = new android.widget.EditText(this);
        inputName.setHint("Full Name");
        inputName.setText(prefs.getString("FULL_NAME", ""));
        layout.addView(inputName);

        final android.widget.EditText inputDob = new android.widget.EditText(this);
        inputDob.setHint("Date of Birth (YYYY-MM-DD)");
        inputDob.setText(prefs.getString("DOB", ""));
        layout.addView(inputDob);

        final android.widget.EditText inputNin = new android.widget.EditText(this);
        inputNin.setHint("ID Number");
        inputNin.setText(prefs.getString("NIN", ""));
        layout.addView(inputNin);

        final android.widget.EditText inputAddress = new android.widget.EditText(this);
        inputAddress.setHint("Residential Address");
        inputAddress.setText(prefs.getString("ADDRESS", ""));
        layout.addView(inputAddress);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Edit Personal Info")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    prefs.edit()
                            .putString("FULL_NAME", inputName.getText().toString().trim())
                            .putString("DOB", inputDob.getText().toString().trim())
                            .putString("NIN", inputNin.getText().toString().trim())
                            .putString("ADDRESS", inputAddress.getText().toString().trim())
                            .apply();

                    // Refresh UI
                    setTextSafe(R.id.tvReviewName, inputName.getText().toString().trim());
                    setTextSafe(R.id.tvReviewDob, inputDob.getText().toString().trim());
                    setTextSafe(R.id.tvReviewNin, inputNin.getText().toString().trim());
                    setTextSafe(R.id.tvReviewAddress, inputAddress.getText().toString().trim());

                    Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
