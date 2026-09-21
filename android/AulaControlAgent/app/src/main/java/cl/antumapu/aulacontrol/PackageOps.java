package cl.antumapu.aulacontrol;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Remote package removal. Only AgentService in the Device Owner user calls this. */
final class PackageOps {
    private PackageOps(){}

    static void uninstallAsync(Context c,String packageName){
        new Thread(()->uninstall(c.getApplicationContext(),packageName),"AulaMovilUninstall").start();
    }

    private static void uninstall(Context c,String packageName){
        try{
            String p=packageName==null?"":packageName.trim();
            if(p.isEmpty()||p.equals(c.getPackageName())||p.equals("android")||p.equals(PlayStoreGuard.PACKAGE))return;
            PackageInstaller pi=c.getPackageManager().getPackageInstaller();
            Intent result=new Intent(c,InstallResultReceiver.class).setAction("cl.antumapu.aulacontrol.PACKAGE_RESULT").putExtra("package",p);
            PendingIntent pending=PendingIntent.getBroadcast(c,(int)(System.currentTimeMillis()&0x7fffffff),result,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            pi.uninstall(p,pending.getIntentSender());
        }catch(Exception ignored){}
    }
}
