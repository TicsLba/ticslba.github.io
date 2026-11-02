package cl.antumapu.guardlite;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

public class PangiFleetService extends Service implements LocationListener {
    private static final String CHANNEL = "pangi_fleet";
    private static final int NOTIFICATION_ID = 3101;
    private static final long HEARTBEAT_MS = 60000L;
    private static final long LOCATION_MS = 120000L;
    private static final float LOCATION_METERS = 25f;

    private final Handler handler = new Handler();
    private LocationManager locationManager;
    private volatile Location lastLocation;
    private volatile boolean exchangeRunning = false;

    private final Runnable heartbeat = new Runnable() {
        @Override public void run() {
            exchange();
            handler.postDelayed(this, HEARTBEAT_MS);
        }
    };

    public static void start(Context context) {
        if (!Prefs.fleetEnabled(context) || !Prefs.consoleConfigured(context)) return;

        Intent intent = new Intent(context, PangiFleetService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, PangiFleetService.class));
        } catch (Exception ignored) {
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        beginLocation();
        handler.post(heartbeat);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Prefs.fleetEnabled(this) || !Prefs.consoleConfigured(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void beginLocation() {
        if (locationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gps != null) lastLocation = gps;
        } catch (Exception ignored) {
        }

        try {
            Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (net != null && (lastLocation == null || net.getTime() > lastLocation.getTime())) {
                lastLocation = net;
            }
        } catch (Exception ignored) {
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_MS,
                    LOCATION_METERS,
                    this);
        } catch (Exception ignored) {
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_MS * 2,
                    LOCATION_METERS,
                    this);
        } catch (Exception ignored) {
        }
    }

    private void exchange() {
        if (exchangeRunning) return;
        exchangeRunning = true;

        new Thread(() -> {
            try {
                FleetClient.Command command = FleetClient.heartbeat(this, lastLocation);
                if (command.hasCommand()) {
                    execute(command);
                }
            } finally {
                exchangeRunning = false;
            }
        }, "PangiFleet").start();
    }

    private void execute(FleetClient.Command command) {
        if ("LOCK".equalsIgnoreCase(command.name)) {
            boolean ok = PangiPolicyManager.lockDevice(this);
            FleetClient.ack(this, command.id, ok ? "DONE" : "FAILED");
            return;
        }

        if ("LOCATE".equalsIgnoreCase(command.name)) {
            beginLocation();
            FleetClient.heartbeat(this, lastLocation);
            FleetClient.ack(this, command.id, "DONE");
            return;
        }

        FleetClient.ack(this, command.id, "UNKNOWN_COMMAND");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL,
                "Pangi · Supervisión institucional",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Estado y ubicación de esta tablet institucional.");
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        return b.setSmallIcon(R.drawable.ic_pangi)
                .setContentTitle("Pangi · supervisión institucional")
                .setContentText("Estado y ubicación de esta tablet están siendo reportados.")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override public void onLocationChanged(Location location) {
        if (location == null) return;
        lastLocation = location;
        Prefs.saveLocation(
                this,
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                System.currentTimeMillis());
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (locationManager != null) {
            try { locationManager.removeUpdates(this); }
            catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
