package com.example.yesbankapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpiTransactionService extends Worker {

    private static final String TAG = "UPIWorker";

    public UpiTransactionService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }



    @NonNull
    @Override
    public Result doWork() {
        String upiId = getInputData().getString("upiId");
        String upiRefNo = getInputData().getString("upiRefNo");

        if (upiId != null && upiRefNo != null) {
            storeUPIDataInFirestore(upiId, upiRefNo);
            return Result.success();
        } else {
            Log.e(TAG, "UPI ID or UPI Reference Number is missing.");
            return Result.failure();
        }
    }

    private void storeUPIDataInFirestore(String upiId, String upiRefNo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map to hold the UPI details
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("upiId", upiId);
        transactionData.put("upiRefNo", upiRefNo);

        // Get current timestamp
        long currentTimestamp = System.currentTimeMillis();

        // Convert timestamp to readable format
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentTimestamp));

        // Add the readable timestamp to the data
        transactionData.put("timestamp", formattedDate);

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
}
