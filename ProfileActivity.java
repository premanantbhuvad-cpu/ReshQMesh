package com.example.reshqmesh;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etUserId;
    private EditText etPhone;
    private Spinner spinnerUserType;
    private Button btnSaveProfile;

    private LocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.etName);
        etUserId = findViewById(R.id.etUserId);
        etPhone = findViewById(R.id.etPhone);
        spinnerUserType = findViewById(R.id.spinnerUserType);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        localStorage = new LocalStorage(this);

        String[] userTypes = {
                "Victim",
                "Rescue Team"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                userTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerUserType.setAdapter(adapter);

        loadProfile();

        btnSaveProfile.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String userId = etUserId.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            String userType =
                    spinnerUserType.getSelectedItem().toString();

            if (name.isEmpty() || userId.isEmpty() || phone.isEmpty()) {

                Toast.makeText(
                        ProfileActivity.this,
                        R.string.profile_fill_all_fields,
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                localStorage.saveProfile(
                        name,
                        userId,
                        phone,
                        userType
                );

                Toast.makeText(
                        ProfileActivity.this,
                        R.string.profile_saved_successfully,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadProfile() {

        String name = localStorage.getName();
        String userId = localStorage.getUserId();
        String phone = localStorage.getPhone();
        String userType = localStorage.getUserType();

        etName.setText(name);
        etUserId.setText(userId);
        etPhone.setText(phone);

        if (userType.equals("Rescue Team")) {
            spinnerUserType.setSelection(1);
        } else {
            spinnerUserType.setSelection(0);
        }
    }
}