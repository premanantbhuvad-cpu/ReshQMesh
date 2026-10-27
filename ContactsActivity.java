package com.example.reshqmesh;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ContactsActivity extends AppCompatActivity {

    private EditText etContactName;
    private EditText etContactPhone;
    private Button btnAddContact;
    private LinearLayout contactContainer;

    private LocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        etContactName = findViewById(R.id.etContactName);
        etContactPhone = findViewById(R.id.etContactPhone);
        btnAddContact = findViewById(R.id.btnAddContact);
        contactContainer = findViewById(R.id.contactContainer);

        localStorage = new LocalStorage(this);

        loadContacts();

        btnAddContact.setOnClickListener(v -> {

            String name = etContactName.getText().toString().trim();
            String phone = etContactPhone.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {

                Toast.makeText(
                        ContactsActivity.this,
                        "Please enter name and phone number",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            saveNewContact(name, phone);

            etContactName.setText("");
            etContactPhone.setText("");

            Toast.makeText(
                    ContactsActivity.this,
                    "Contact added successfully",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void saveNewContact(String name, String phone) {

        String existingContacts = localStorage.getContacts();

        String newContact = name + " | " + phone;

        if (existingContacts.isEmpty()) {

            existingContacts = newContact;

        } else {

            existingContacts =
                    existingContacts + "\n" + newContact;
        }

        localStorage.saveContacts(existingContacts);

        addContactToScreen(name, phone);
    }

    private void loadContacts() {

        String savedContacts = localStorage.getContacts();

        if (savedContacts.isEmpty()) {
            return;
        }

        String[] contacts = savedContacts.split("\n");

        for (String contact : contacts) {

            String[] details = contact.split(" \\| ");

            if (details.length == 2) {

                addContactToScreen(
                        details[0],
                        details[1]
                );
            }
        }
    }

    private void addContactToScreen(
            String name,
            String phone
    ) {

        TextView contactView = new TextView(this);

        contactView.setText(
                name + "\n" + phone
        );

        contactView.setTextSize(16);
        contactView.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);

        contactView.setLayoutParams(params);

        contactContainer.addView(contactView);
    }
}