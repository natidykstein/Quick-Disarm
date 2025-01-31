package com.quick.disarm.register;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.quick.disarm.R;

public class RegisterActivity extends AppCompatActivity {
    public static final String EXTRA_CAR_BLUETOOTH = "com.quick.disarm.extra.CAR_BLUETOOTH_MAC";

    private static final int SMS_PERMISSION_CODE = 2000;

    private EditText editTextLicensePlate;
    private EditText editTextPhoneNumber;
    private EditText editTextVerificationCode;
    private TextView textViewSummary;
    private View page1;
    private View page2;
    private View page3;
    private String licensePlate;
    private String phoneNumber;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupPage1();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private void initViews() {
        page1 = findViewById(R.id.page1);
        page2 = findViewById(R.id.page2);
        page3 = findViewById(R.id.page3);
        editTextLicensePlate = findViewById(R.id.editTextLicensePlate);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        textViewSummary = findViewById(R.id.textViewSummary);
    }

    private void setupPage1() {
        Button buttonNext1 = findViewById(R.id.buttonNext1);
        buttonNext1.setOnClickListener(view -> {
            licensePlate = editTextLicensePlate.getText().toString().trim();
            phoneNumber = editTextPhoneNumber.getText().toString().trim();
            if (validatePage1()) {
                sendSmsVerificationCode(phoneNumber);
                showPage2();
            }
        });
    }

    private boolean validatePage1() {
        if (TextUtils.isEmpty(licensePlate)) {
            editTextLicensePlate.setError("License plate is required");
            return false;
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhoneNumber.setError("Phone number is required");
            return false;
        }
        return true;
    }

    private void sendSmsVerificationCode(String phoneNumber) {
        // Simulate sending SMS verification code
        Log.d("MainActivity", "Sending SMS verification code to " + phoneNumber);
    }

    private void showPage2() {
        page1.setVisibility(View.GONE);
        page2.setVisibility(View.VISIBLE);
        setupPage2();
    }

    private void setupPage2() {
        Button buttonNext2 = findViewById(R.id.buttonNext2);
        buttonNext2.setOnClickListener(view -> {
            verificationCode = editTextVerificationCode.getText().toString().trim();
            if (validatePage2()) {
                showPage3();
            }
        });
    }

    private boolean validatePage2() {
        if (TextUtils.isEmpty(verificationCode)) {
            editTextVerificationCode.setError("Verification code is required");
            return false;
        }
        return true;
    }

    private void showPage3() {
        page2.setVisibility(View.GONE);
        page3.setVisibility(View.VISIBLE);
        textViewSummary.setText("License Plate: " + licensePlate + "\nPhone Number: " + phoneNumber + "\nVerification Code: " + verificationCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsReceiver);
    }

    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String messageBody = smsMessage.getMessageBody();
                        // Extract verification code from the SMS message
                        // Assuming the verification code is the message body for simplicity
                        editTextVerificationCode.setText(messageBody);
                    }
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
