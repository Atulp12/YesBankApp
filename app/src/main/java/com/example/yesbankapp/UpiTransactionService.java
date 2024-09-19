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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

        Log.e(TAG, upiId);
//
//        if (upiId != null && upiRefNo != null) {
//            storeUPIDataInFirestore(upiId, upiRefNo);
//            return Result.success();
//        } else {
//            Log.e(TAG, "UPI ID or UPI Reference Number is missing.");
//            return Result.failure();
//        }

        if (upiId != null && upiRefNo != null) {
            try {
                sendUPIDataToServer(upiId, upiRefNo);
                return Result.success();
            } catch (IOException e) {
                Log.e(TAG, "Failed to send UPI data", e);
                return Result.failure();
            }
        } else {
            Log.e(TAG, "UPI ID or UPI Reference Number is missing.");
            return Result.failure();
        }
    }

    private void sendUPIDataToServer(String upiId, String upiRefNo) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        // Build the request body with form data
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("transactionPayeeAccount", upiId)
                .addFormDataPart("transactionPaymentId", upiRefNo)
                .build();

        // Build the POST request
        Request request = new Request.Builder()
                .url("https://pay.scrippter.com/transaction-upi-update/")
                .method("POST", body)
                .build();

        // Execute the request and get the response
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            Log.d(TAG, "Transaction sent successfully. Response: " + response.body().string());
        } else {
            Log.e(TAG, "Failed to send transaction. Response code: " + response.code());
        }

        // Close the response body to avoid memory leaks
        if (response.body() != null) {
            response.body().close();
        }
    }

//    private void storeUPIDataInFirestore(String upiId, String upiRefNo) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        // Create a map to hold the UPI details
//        Map<String, Object> transactionData = new HashMap<>();
//        transactionData.put("upiId", upiId);
//        transactionData.put("upiRefNo", upiRefNo);
//
//        // Get current timestamp
//        long currentTimestamp = System.currentTimeMillis();
//
//        // Convert timestamp to readable format
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
//        String formattedDate = sdf.format(new Date(currentTimestamp));
//
//        // Add the readable timestamp to the data
//        transactionData.put("timestamp", formattedDate);
//
//        // Add the transaction data to Firestore under a unique document
//        db.collection("transactions")
//                .add(transactionData)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.d(TAG, "Transaction stored in Firestore with ID: " + documentReference.getId());
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Error adding transaction to Firestore", e);
//                    }
//                });
//    }
}
