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
import com.quick.disarm.ReportAnalytics;
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
    public static final String EXTRA_TRIGGER_BLUETOOTH_NAME = "com.quick.disarm.extra.TRIGGER_BLUETOOTH_NAME";
    public static final String EXTRA_TRIGGER_BLUETOOTH_ADDRESS = "com.quick.disarm.extra.TRIGGER_BLUETOOTH_ADDRESS";

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
    private Button buttonNext2;
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
        buttonNext2 = findViewById(R.id.buttonNext2);
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
            ReportAnalytics.reportSelectButtonEvent("next_page1", "Next");
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
            editTextLicensePlate.setError(getString(R.string.license_plate_is_required));
            return false;
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            editTextPhoneNumber.setError(getString(R.string.phone_number_is_required));
            return false;
        }
        if (TextUtils.isEmpty(ituranCode)) {
            editTextIturanCode.setError(getString(R.string.ituran_code_is_required));
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
                ILog.d("Got from server: " + answer);
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
            ILog.logException(new RuntimeException(errorMessage));
            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(RegisterActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPage2() {
        page1.setVisibility(View.GONE);
        page2.setVisibility(View.VISIBLE);
        setupPage2();
    }

    private void setupPage2() {
        buttonNext2.setOnClickListener(view -> {
            ReportAnalytics.reportSelectButtonEvent("next_page2", "Next");
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
                ILog.d("Got from server: " + answer);
                if (ModelUtils.isValid(answer.getReturnError())) {
                    final String triggerBluetoothName = getIntent().getStringExtra(EXTRA_TRIGGER_BLUETOOTH_NAME);
                    final String triggerBluetoothAddress = getIntent().getStringExtra(EXTRA_TRIGGER_BLUETOOTH_ADDRESS);

                    saveDriverData(triggerBluetoothName, triggerBluetoothAddress, answer.getStarlinkMacAddress(), answer.getStarlinkSerial());
                    showPage3(triggerBluetoothName, triggerBluetoothAddress);
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

    private void saveDriverData(String triggerBluetoothName, String triggerBluetoothAddress, String starlinkMacAddress, int starlinkSerial) {
        final Car car = new Car(phoneNumber, triggerBluetoothName, triggerBluetoothAddress, licensePlate, starlinkMacAddress, starlinkSerial, ituranCode);
        PreferenceCache.get(this).addCar(car);

        // PENDING: For now logging as exception for increased visibility
        ILog.logException(new RuntimeException("Successfully registered " + car.toStringExtended()));
    }

    private boolean validatePage2() {
        if (TextUtils.isEmpty(verificationCode)) {
            editTextVerificationCode.setError(getString(R.string.verification_code_is_required));
            return false;
        }
        return true;
    }

    private void showPage3(String triggerBluetoothName, String triggerBluetoothAddress) {
        page2.setVisibility(View.GONE);
        page3.setVisibility(View.VISIBLE);
        final String carBluetoothName = getIntent().getStringExtra(EXTRA_TRIGGER_BLUETOOTH_NAME);
        textViewSummary.setText(getString(R.string.summary_message, carBluetoothName, licensePlate, phoneNumber));

        // Update analytics after a successful car registration
        QuickDisarmApplication.initAnalytics(this);

        final Button buttonDone = findViewById(R.id.buttonDone);
        buttonDone.setOnClickListener(v -> {
            ReportAnalytics.reportSelectButtonEvent("done", "Done");
            finish();
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
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        final SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        final String messageBody = smsMessage.getMessageBody();

                        // Extract verification code from the SMS message
                        final StringBuilder smsCode = new StringBuilder();
                        for (int i = 0; i < messageBody.length(); i++) {
                            while (i < messageBody.length() && Character.isDigit(messageBody.charAt(i))) {
                                smsCode.append(messageBody.charAt(i++));
                            }
                        }

                        // Insert extracted code if not empty
                        if (smsCode.length() > 0) {
                            ILog.d("Auto inserting OTP received via sms: " + smsCode);
                            editTextVerificationCode.setText(smsCode.toString());
                            buttonNext2.callOnClick();
                        } else {
                            ILog.w("Failed to parse OTP received via sms: " + messageBody);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ILog.d("Sms permission granted");
            Toast.makeText(this, R.string.sms_permission_granted, Toast.LENGTH_SHORT).show();
        } else {
            ILog.d("Sms permission not granted");
            Toast.makeText(this, R.string.sms_permission_not_granted, Toast.LENGTH_SHORT).show();
        }
    }
}
