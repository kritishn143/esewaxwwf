package app.kyc.verification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

public class PersonalInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_personal_info);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root_layout_info), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        findViewById(R.id.btnBackInfo).setOnClickListener(v -> finish());
        
        TextInputEditText etFullName = findViewById(R.id.etFullName);
        TextInputEditText etDob = findViewById(R.id.etDob);
        TextInputEditText etIdNumber = findViewById(R.id.etNin);
        
        SharedPreferences prefs = getSharedPreferences("KYC_DATA", MODE_PRIVATE);
        if (etFullName != null) etFullName.setText(prefs.getString("FULL_NAME", ""));
        if (etDob != null) etDob.setText(prefs.getString("DOB", ""));
        if (etIdNumber != null) etIdNumber.setText(prefs.getString("NIN", ""));

        MaterialButton btnContinue = findViewById(R.id.btnSubmitInfo);
        
        // Real-time validation for ID Number (Challenge Objective)
        if (etIdNumber != null) {
            etIdNumber.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    if (s.length() > 0 && s.length() < 12) {
                        etIdNumber.setError("ID should be 12 digits");
                    } else if (s.length() > 12) {
                        etIdNumber.setError("Too long");
                    } else {
                        etIdNumber.setError(null);
                    }
                }
            });
        }

        btnContinue.setOnClickListener(v -> {
            String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String id = etIdNumber.getText() != null ? etIdNumber.getText().toString().trim() : "";
            
            if (name.isEmpty()) {
                etFullName.setError("Name is required");
                return;
            }
            if (id.length() != 12) {
                etIdNumber.setError("Valid 12-digit ID required");
                return;
            }

            prefs.edit()
                .putString("FULL_NAME", name)
                .putString("DOB", etDob.getText() != null ? etDob.getText().toString() : "")
                .putString("NIN", id)
                .apply();
            
            finish(); // Go back to Review screen
        });
    }
}
