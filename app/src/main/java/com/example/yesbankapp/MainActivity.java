package com.example.yesbankapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;



public class MainActivity extends AppCompatActivity implements SmsReceiver.UPIListener {

    private static final int SMS_PERMISSION_CODE = 100;
    private TextView upiTextView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        upiTextView = findViewById(R.id.upiTextView);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Check for SMS permissions
        if (checkPermission(Manifest.permission.RECEIVE_SMS)) {
            SmsReceiver.bindListener(this); // Bind the listener if permission is granted
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // Check if a specific permission is granted
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

    // Implementing UPIListener callback
    @Override
    public void onUPIReceived(String upiId, String upiReferenceNumber) {
        String upiDetails = "UPI ID: " + upiId + "\nReference Number: " + upiReferenceNumber;
        upiTextView.setText(upiDetails); // Update the TextView with UPI details
        Toast.makeText(this, "UPI details received", Toast.LENGTH_SHORT).show();
    }
}
