package com.quick.disarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.quick.disarm.infra.ILog;

public class AuthenticationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authenticateBiometrically();
    }

    private void authenticateBiometrically() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Car's bluetooth detected")
                    .setSubtitle("Authenticate to disarm Ituran")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();

            final BiometricPrompt biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            // Authentication successful, send broadcast to JobIntentService
                            final Intent intent = new Intent(DisarmJobIntentService.ACTION_DISARM);
                            sendBroadcast(intent);
                            finish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            final String errorMessage = "Authentication failed - Ituran not disarmed";
                            ILog.e(errorMessage);
                            Toast.makeText(AuthenticationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

            biometricPrompt.authenticate(promptInfo);
        }
    }
}
