package com.quick.disarm;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.quick.disarm.infra.ILog;

public class AuthenticationActivity extends AppCompatActivity {

    private String mConnectedCarBluetoothMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnectedCarBluetoothMac = getIntent().getStringExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH);

        authenticateBiometrically();
    }

    private void authenticateBiometrically() {
        final int allowedAuthenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG;
        final BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authenticate to disarm Ituran")
                    // .setSubtitle("Authenticate to disarm Ituran")
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build();

            final BiometricPrompt biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            ILog.d("Authentication successful");
                            // Authentication successful - start DisarmJobIntentService
                            DisarmJobIntentService.enqueueWork(AuthenticationActivity.this, mConnectedCarBluetoothMac);
                            finish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            authenticationFailed("Authentication failed");
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            authenticationFailed(errString.toString());
                        }

                        private void authenticationFailed(String errorMessage) {
                            errorMessage += " - Ituran not disarmed";
                            ILog.e(errorMessage);
                            Toast.makeText(AuthenticationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });

            biometricPrompt.authenticate(promptInfo);
        }
    }
}
