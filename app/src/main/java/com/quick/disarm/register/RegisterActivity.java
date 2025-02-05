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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.VolleyError;
import com.quick.disarm.Car;
import com.quick.disarm.QuickDisarmApplication;
import com.quick.disarm.R;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;
import com.quick.disarm.infra.network.volley.AppResponse;
import com.quick.disarm.infra.network.volley.IturanServerAPI;
import com.quick.disarm.infra.network.volley.VolleyResponseListener;
import com.quick.disarm.model.ActivationAnswer;
import com.quick.disarm.model.ModelUtils;
import com.quick.disarm.model.SerializationAnswer;
import com.quick.disarm.utils.PreferenceCache;

public class RegisterActivity extends AppCompatActivity {
    public static final String EXTRA_CAR_BLUETOOTH_NAME = "com.quick.disarm.extra.CAR_BLUETOOTH_NAME";
    public static final String EXTRA_CAR_BLUETOOTH_MAC = "com.quick.disarm.extra.CAR_BLUETOOTH_MAC";

    private static final int SMS_PERMISSION_CODE = 2000;

    private EditText editTextLicensePlate;
    private EditText editTextPhoneNumber;
    private EditText editTextIturanCode;
    private ProgressBar progressBar;

    private EditText editTextVerificationCode;
    private TextView textViewSummary;
    private View page1;
    private View page2;
    private View page3;
    private String licensePlate;
    private String phoneNumber;
    private String ituranCode;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        page1 = findViewById(R.id.page1);
        page2 = findViewById(R.id.page2);
        page3 = findViewById(R.id.page3);
        editTextLicensePlate = findViewById(R.id.editTextLicensePlate);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextIturanCode = findViewById(R.id.editTextIturanCode);
        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        textViewSummary = findViewById(R.id.textViewSummary);
        progressBar = findViewById(R.id.progressBarRegister);

        setupPage1();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private void setupPage1() {
        final Button buttonNext1 = findViewById(R.id.buttonNext1);
        buttonNext1.setOnClickListener(view -> {
            licensePlate = editTextLicensePlate.getText().toString().trim();
            phoneNumber = editTextPhoneNumber.getText().toString().trim();
            ituranCode = editTextIturanCode.getText().toString().trim();
            if (validatePage1()) {
                progressBar.setVisibility(View.VISIBLE);
                sendSmsVerificationCode();
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
        if (TextUtils.isEmpty(ituranCode)) {
            editTextIturanCode.setError("Ituran code is required");
            return false;
        }
        return true;
    }

    private void sendSmsVerificationCode() {
        Log.d("RegisterActivity", "Sending SMS verification code to " + phoneNumber + "...");

        final VolleyResponseListener<AppResponse<ActivationAnswer>> activationResponseListener = new VolleyResponseListener<>() {
            @Override
            protected void onResponse(AppResponse<ActivationAnswer> response, boolean secondCallback) {
                progressBar.setVisibility(View.GONE);
                final ActivationAnswer answer = response.getData();
                ILog.d("Activation answer = " + answer);
                if (ModelUtils.isValid(answer.getReturnError())) {
                    showPage2();
                } else {
                    editTextLicensePlate.setError(answer.getReturnError());
                }
            }

            @Override
            protected void onErrorResponse(VolleyError volleyError, boolean secondCallback, boolean unauthorized) {
                showErrorMessage(VolleyResponseListener.responseParser(volleyError));
            }
        };

        final String deviceUuid = Utils.getDeviceUuid(QuickDisarmApplication.getAppContext());
        ILog.d("Using device uuid = " + deviceUuid);
        IturanServerAPI.get().verifyDriver(licensePlate, phoneNumber, deviceUuid, activationResponseListener, activationResponseListener);
    }

    private void showErrorMessage(String errorMessage) {
        progressBar.setVisibility(View.GONE);

        if (Utils.isConnectedToNetwork(RegisterActivity.this)) {
            ILog.e(errorMessage);
            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(RegisterActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPage2() {
        page1.setVisibility(View.GONE);
        page2.setVisibility(View.VISIBLE);
        setupPage2();
    }

    private void setupPage2() {
        final Button buttonNext2 = findViewById(R.id.buttonNext2);
        buttonNext2.setOnClickListener(view -> {
            verificationCode = editTextVerificationCode.getText().toString().trim();
            if (validatePage2()) {
                progressBar.setVisibility(View.VISIBLE);
                validateSmsVerificationCode();
            }
        });
    }

    private void validateSmsVerificationCode() {
        Log.d("RegisterActivity", "Validating SMS verification code " + verificationCode + "...");

        final VolleyResponseListener<AppResponse<SerializationAnswer>> serializationResponseListener = new VolleyResponseListener<>() {
            @Override
            protected void onResponse(AppResponse<SerializationAnswer> response, boolean secondCallback) {
                progressBar.setVisibility(View.GONE);
                final SerializationAnswer answer = response.getData();
                ILog.d("Serialization answer = " + answer);
                if (ModelUtils.isValid(answer.getReturnError())) {
                    saveDriverData(answer.getStarlinkMacAddress(), answer.getStarlinkSerial());
                    showPage3();
                } else {
                    editTextVerificationCode.setError(answer.getReturnError());
                }
            }

            @Override
            protected void onErrorResponse(VolleyError volleyError, boolean secondCallback, boolean unauthorized) {
                showErrorMessage(VolleyResponseListener.responseParser(volleyError));
            }
        };
        IturanServerAPI.get().validateSmsVerificationCode(licensePlate, phoneNumber, verificationCode, serializationResponseListener, serializationResponseListener);
    }

    private void saveDriverData(String starlinkMacAddress, int starlinkSerial) {
        final Car car = new Car(licensePlate, starlinkMacAddress, starlinkSerial, ituranCode);
        final String carBluetoothMac = getIntent().getStringExtra(EXTRA_CAR_BLUETOOTH_MAC);
        PreferenceCache.get(this).putCar(carBluetoothMac, car);
        ILog.d("Added " + car);
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
        final String carBluetoothName = getIntent().getStringExtra(EXTRA_CAR_BLUETOOTH_NAME);
        textViewSummary.setText("Successfully registered\n\nCar's Bluetooth: " + carBluetoothName + "\nLicense Plate: " + licensePlate + "\nPhone Number: " + phoneNumber);

        final Button buttonDone = findViewById(R.id.buttonDone);
        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
                        final SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        final String messageBody = smsMessage.getMessageBody();
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
