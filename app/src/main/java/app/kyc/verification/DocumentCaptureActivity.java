package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DocumentCaptureActivity extends AppCompatActivity {

    private static final String PREFS = "KYC_DATA";
    private static final String KEY_FRONT_CAPTURED = "FRONT_CAPTURED";

    private boolean isFrontCaptured = false;
    private ImageView ivDocumentPreview;
    private TextView tvInstructionTitle;
    private TextView tvInstructionDesc;
    private TextView tvStepIndicator;
    private MaterialButton btnUpload;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_document_capture);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_capture), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        ivDocumentPreview = findViewById(R.id.ivDocumentPreview);
        tvInstructionTitle = findViewById(R.id.tvInstructionTitle);
        tvInstructionDesc = findViewById(R.id.tvInstructionDesc);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        btnUpload = findViewById(R.id.btnUpload);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        isFrontCaptured = prefs.getBoolean(KEY_FRONT_CAPTURED, false);
        updateStepUI();

        findViewById(R.id.btnBackCapture).setOnClickListener(v -> finish());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivDocumentPreview.setImageURI(uri);
                        ivDocumentPreview.setImageTintList(null);
                        ivDocumentPreview.setPadding(0, 0, 0, 0);
                        processImage(uri);
                    }
                });

        btnUpload.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
    }

    private void updateStepUI() {
        if (isFrontCaptured) {
            if (tvStepIndicator != null)
                tvStepIndicator.setText("Step 2 of 4");
            tvInstructionTitle.setText("Upload Back of ID");
            tvInstructionDesc.setText("Please select an image of the back of your document");
            btnUpload.setText("Select Back Image");
            ivDocumentPreview.setImageResource(R.drawable.ic_id_card); // Use card icon for back
        } else {
            if (tvStepIndicator != null)
                tvStepIndicator.setText("Step 1 of 4");
            tvInstructionTitle.setText("Upload Front of ID");
            tvInstructionDesc.setText("Please select an image of the front of your document");
            btnUpload.setText("Select Front Image");
            ivDocumentPreview.setImageResource(R.drawable.nid_front); // Use specific front icon
        }
    }

    private void processImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition
                    .getClient(new DevanagariTextRecognizerOptions.Builder().build());

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        if (!isFrontCaptured) {
                            if (isFrontDataValid(visionText)) {
                                extractDataFromFront(visionText);
                                saveImageToInternalStorage(uri, "id_card_front.jpg");

                                isFrontCaptured = true;
                                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                        .putBoolean(KEY_FRONT_CAPTURED, true).apply();
                                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                                java.util.Set<String> rej = sp.getStringSet("REJECTED_FIELDS",
                                        new java.util.HashSet<>());
                                boolean backDone = !sp.getString("ADDRESS", "").isEmpty();
                                if (backDone && !rej.contains("backImage")) {
                                    boolean selfieOk = new File(getExternalFilesDir(null), "selfie.jpg").exists()
                                            && !rej.contains("selfieImage");
                                    startActivity(selfieOk ? new Intent(this, ReviewSubmitActivity.class)
                                            : new Intent(this, SelfieVerificationActivity.class));
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                    finish();
                                } else {
                                    updateStepUI();
                                }
                                Toast.makeText(this, "Front processed! Now upload the back.", Toast.LENGTH_LONG)
                                        .show();
                                // ✓
                            } else {
                                Toast.makeText(this, "Could not detect valid Front NID data. Please try another image.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (isBackDataValid(visionText)) {
                                extractDataFromBack(visionText);
                                saveImageToInternalStorage(uri, "id_card_back.jpg");

                                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                        .remove(KEY_FRONT_CAPTURED).apply();
                                Toast.makeText(this, "Back processed!", Toast.LENGTH_SHORT).show();

                                // Smart Navigation: Skip Selfie if it's already done and NOT rejected
                                Intent intent;
                                SharedPreferences sharedPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                                java.util.Set<String> rejected = sharedPrefs.getStringSet("REJECTED_FIELDS",
                                        new java.util.HashSet<>());
                                boolean selfieExists = new File(getExternalFilesDir(null), "selfie.jpg").exists();

                                if (selfieExists && !rejected.contains("selfieImage")) {
                                    intent = new Intent(this, ReviewSubmitActivity.class);
                                } else {
                                    intent = new Intent(this, SelfieVerificationActivity.class);
                                }

                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            } else {
                                Toast.makeText(this, "Could not detect valid Back ID data. Please try another image.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToInternalStorage(Uri uri, String fileName) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = new File(getExternalFilesDir(null), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Validation ──────────────────────────────────────────────────────────
    private boolean isFrontDataValid(Text visionText) {
        String text = visionText.getText();
        boolean hasNin = java.util.regex.Pattern.compile("\\d{3}-\\d{3}-\\d{4}").matcher(text).find();
        boolean hasDate = java.util.regex.Pattern.compile("\\d{4}[-/]\\d{2}[-/]\\d{2}").matcher(text).find();
        boolean hasName = java.util.regex.Pattern.compile("[A-Z][a-z]+\\s+[A-Z][a-z]+").matcher(text).find();
        return hasNin && hasDate && hasName;
    }

    private boolean isBackDataValid(Text visionText) {
        String text = visionText.getText();
        // Check for common address keywords or the word "address"
        return java.util.regex.Pattern.compile("(?i)Municipality|Metropolitan|Rural|Ward|District|Address|Permanent")
                .matcher(text).find();
    }

    // ── Extraction ──────────────────────────────────────────────────────────
    private void extractDataFromFront(Text visionText) {
        KycDocumentParser.KycData data = KycDocumentParser.parseFront(visionText);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("FULL_NAME", data.fullName != null ? data.fullName : "")
                .putString("DOB", data.dob != null ? data.dob : "")
                .putString("NIN", data.nin != null ? data.nin : "")
                .putString("SEX", data.sex != null ? data.sex : "")
                .putString("NATIONALITY", data.nationality != null ? data.nationality : "")
                .apply();
    }

    private void extractDataFromBack(Text visionText) {
        KycDocumentParser.KycData data = KycDocumentParser.parseBack(visionText);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("ADDRESS", data.address != null ? data.address : "").apply();
    }
}
