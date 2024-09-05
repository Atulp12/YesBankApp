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

import java.util.HashMap;
import java.util.Map;

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

    // The fixed part of the sender ID
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

                    Log.d(TAG, "SMS received from: " + sender);
                    Log.d(TAG, "Message content: " + messageBody);

                    // Check if the sender's ID ends with the fixed part and the message contains the keyword
                    if (containsSender(normalizedSender) && messageBody.contains(SPECIFIC_KEYWORD)) {
                        String upiId = extractUPIId(messageBody);
                        String upiRefNo = extractUPIRefNo(messageBody);

                        // Trigger the UPIListener callback if UPI ID and reference number are extracted
                        if (upiListener != null && upiId != null && upiRefNo != null) {
                            upiListener.onUPIReceived(upiId, upiRefNo); // Notify the listener
                        }
                        storeUPIDataInFirestore(upiId, upiRefNo);
                        startUpiTransactionService(context, upiId, upiRefNo);
                        showPopup(context, sender, upiId, upiRefNo); // Show popup with details
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
        String[] parts = message.split(" ");
        for (String part : parts) {
            if (part.contains("@") || part.contains("?")) { // Checks if part contains UPI ID pattern
                return part.replace("?", "@"); // Replace '?' with '@'
            }
        }
        return null;
    }


    private void storeUPIDataInFirestore(String upiId, String upiRefNo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map to hold the UPI details
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("upiId", upiId);
        transactionData.put("upiRefNo", upiRefNo);
        transactionData.put("timestamp", System.currentTimeMillis()); // Optional: Add a timestamp

        // Add the transaction data to Firestore under a unique document
        db.collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Transaction stored in Firestore with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding transaction to Firestore", e);
                    }
                });
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

    private void showPopup(Context context, String sender, String upiId, String upiRefNo) {
        // Implementation for showing a popup with the sender, UPI ID, and reference number
        Toast.makeText(context, "Sender: " + sender + "\nUPI ID: " + upiId + "\nUPI Ref No: " + upiRefNo, Toast.LENGTH_LONG).show();
    }

    // Method to start the service and pass UPI ID and reference number
    private void startUpiTransactionService(Context context, String upiId, String upiRefNo) {
        Intent serviceIntent = new Intent(context, UpiTransactionService.class);
        serviceIntent.putExtra("upiId", upiId);
        serviceIntent.putExtra("upiRefNo", upiRefNo);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
