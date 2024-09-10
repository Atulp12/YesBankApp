package com.example.yesbankapp;


import static android.Manifest.permission.RECEIVE_SMS;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.Manifest;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements SmsReceiver.OTPListener {

    private static final int SMS_PERMISSION_CODE = 100;
    private TextView otpTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        otpTextView = findViewById(R.id.upiTextView);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Check for SMS permissions
        if (checkPermission(RECEIVE_SMS)) {
            SmsReceiver.bindListener(this); // Bind the listener if permission is granted
        } else {
            ActivityCompat.requestPermissions(this, new String[]{RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }

        // Check for battery optimization
        checkBatteryOptimization();
    }

    @Override
    public void onOTPReceived(String otp) {
        otpTextView.setText("OTP: " + otp); // Update the TextView with OTP
        Toast.makeText(this, "OTP received: " + otp, Toast.LENGTH_SHORT).show();
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(this, permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SmsReceiver.bindListener(this); // Bind listener if permission is granted
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Other methods remain unchanged
}
