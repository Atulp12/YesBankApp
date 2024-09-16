package com.example.yesbankapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    public interface UPIListener {
        void onUPIReceived(String upiId, String upiReferenceNumber);
    }

    private static UPIListener upiListener;

    // Bind the listener to the activity
    public static void bindListener(UPIListener listener) {
        upiListener = listener;
    }

    private static final String TAG = "SmsReceiver";
    private static final String SPECIFIC_KEYWORD = "credited with"; // Keyword to identify the correct message
    private static final String FIXED_PART = "CANBNK";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        if (data != null) {
            Object[] pdus = (Object[]) data.get("pdus");
            String format = intent.getStringExtra("format");

            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                    String sender = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();

                    // Normalize the sender ID by removing hyphens for comparison
                    String normalizedSender = sender.replace("-", "");

                    // Check if the sender's ID ends with the fixed part and the message contains the keyword
                    if (containsSender(normalizedSender) && messageBody.contains(SPECIFIC_KEYWORD)) {
                        String upiId = extractUPIId(messageBody);
                        String upiRefNo = extractUPIRefNo(messageBody);

                        // Trigger the UPIListener callback if UPI ID and reference number are extracted
                        if (upiListener != null) {
                            upiListener.onUPIReceived(upiId, upiRefNo); // Notify the listener
                        } else {
                            Log.w(TAG, "UPIListener is not set. Unable to notify listener.");
                        }

                        // Start WorkManager task to process UPI data
                        startUpiTransactionService(context, upiId, upiRefNo);
                    }
                }
            }
        }
    }

    // Helper method to check if the sender ends with the fixed part
    private boolean containsSender(String normalizedSender) {
        return normalizedSender.endsWith(FIXED_PART);
    }

    // Extract UPI ID from the message
    private String extractUPIId(String message) {
        // Regex pattern to match email-like UPI IDs, numeric/alpha-numeric followed by bank code, and number followed by bank code
        String regex = "\\b[\\w\\.\\-]+@[\\w\\.\\-]+\\b|\\b([\\w]+)\\s*([a-zA-Z]+)\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            // Check for email-like UPI IDs
            if (matcher.group(0).contains("@")) {
                return matcher.group(0).replace("?", "@");
            }
            // Check for alphanumeric UPI IDs followed by bank code (e.g., atul4050 ybl)
            else if (matcher.group(1) != null && matcher.group(2) != null) {
                // Ensure we're handling both number + bank and alphanumeric + bank cases
                String upiID = matcher.group(1) + "@" + matcher.group(2);
                return upiID;
            }
        }

        return null; // Return null if no valid UPI ID is found
    }








    // Extract UPI Reference Number from the message
    private String extractUPIRefNo(String message) {
        // Find the substring starting with "UPI Ref no"
        int refNoIndex = message.indexOf("UPI Ref no");
        if (refNoIndex != -1) {
            // Extract the reference number part, which should follow "UPI Ref no"
            String refPart = message.substring(refNoIndex);
            String[] parts = refPart.split(" ");
            if (parts.length > 3) {
                // The reference number is usually the fourth part
                String refNo = parts[3];
                // Ensure only digits are included by trimming non-digit characters
                return refNo.replaceAll("[^\\d]", "");
            }
        }
        return null; // Return null if the reference number couldn't be extracted
    }

    // Use WorkManager to process UPI transaction in the background
    private void startUpiTransactionService(Context context, String upiId, String upiRefNo) {
        // Create input data for the Worker
        Data inputData = new Data.Builder()
                .putString("upiId", upiId)
                .putString("upiRefNo", upiRefNo)
                .build();

        // Create a OneTimeWorkRequest to trigger the worker
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(UpiTransactionService.class)
                .setInputData(inputData)
                .build();

        // Enqueue the work request to WorkManager
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
