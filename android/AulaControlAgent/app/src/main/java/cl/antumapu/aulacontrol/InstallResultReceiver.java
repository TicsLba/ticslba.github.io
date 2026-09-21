package cl.antumapu.aulacontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

public class InstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        int status=i.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
        String msg=i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        String ch="install";
        if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(ch)==null)nm.createNotificationChannel(new NotificationChannel(ch,"Aula Móvil · instalaciones",NotificationManager.IMPORTANCE_DEFAULT));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,ch):new Notification.Builder(c);
        b.setSmallIcon(status==PackageInstaller.STATUS_SUCCESS?android.R.drawable.stat_sys_download_done:android.R.drawable.stat_notify_error)
         .setContentTitle(status==PackageInstaller.STATUS_SUCCESS?"Aplicación instalada":"Instalación pendiente o fallida")
         .setContentText(msg==null?"Aula Móvil recibió el resultado del instalador":msg);
        nm.notify(3101,b.build());
    }
}
