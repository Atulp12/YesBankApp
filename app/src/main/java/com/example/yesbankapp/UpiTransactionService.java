package com.example.yesbankapp;



import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UpiTransactionService extends Service {

    private static final String TAG = "UpiTransactionService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Extract UPI ID and Reference from Intent
            String upiId = intent.getStringExtra("upiId");
            String upiRefNo = intent.getStringExtra("upiRefNo");

            // Log the received values
            Log.d(TAG, "UPI ID: " + upiId + ", UPI Ref No: " + upiRefNo);

            // Store UPI data in Firestore
            storeUPIDataInFirestore(upiId, upiRefNo);
        }

        // Stop service after work is done
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Store UPI data in Firestore
    private void storeUPIDataInFirestore(String upiId, String upiRefNo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Prepare UPI data to be stored
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("upiId", upiId);
        transactionData.put("upiRefNo", upiRefNo);
        transactionData.put("timestamp", System.currentTimeMillis());

        // Store in the 'transactions' collection
        db.collection("transactions").add(transactionData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "UPI Data stored successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error storing UPI data", e));
    }
}

