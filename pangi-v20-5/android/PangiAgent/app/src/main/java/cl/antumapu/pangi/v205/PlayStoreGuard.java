package cl.antumapu.pangi.v205;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.content.pm.PackageManager;

/**
 * PANGI 11 policy:
 * - Propietario/Administrador: Google Play disponible.
 * - Estudiante y Profesor: Google Play oculto y suspendido únicamente dentro
 *   del usuario Android temporal. Ese usuario se elimina al cerrar sesión.
 *
 * Nunca se oculta Google Play en el usuario Propietario.
 */
final class PlayStoreGuard {
    static final String PACKAGE = "com.android.vending";
    private PlayStoreGuard(){}

    static boolean available(Context c){
        try{
            if(Build.VERSION.SDK_INT>=33)c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES));
            else c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return true;
        }catch(Exception e){return false;}
    }

    static boolean lock(Context c){
        if(!Managed.profileOwner(c))return false;
        DevicePolicyManager d=Managed.dpm(c); ComponentName a=Managed.admin(c);
        boolean ok=true;
        try{d.setPackagesSuspended(a,new String[]{PACKAGE},true);}catch(Exception e){ok=false;}
        try{d.setApplicationHidden(a,PACKAGE,true);}catch(Exception e){ok=false;}
        return ok;
    }

    static boolean unlock(Context c){
        if(!Managed.managed(c))return false;
        DevicePolicyManager d=Managed.dpm(c); ComponentName a=Managed.admin(c);
        boolean ok=true;
        try{d.setApplicationHidden(a,PACKAGE,false);}catch(Exception ignored){}
        try{d.setPackagesSuspended(a,new String[]{PACKAGE},false);}catch(Exception e){ok=false;}
        return ok;
    }

    static boolean unlockForAdmin(Context c){
        return Managed.owner(c)&&Core.adminMode(c)&&available(c)&&unlock(c);
    }

    static long remaining(Context c){return 0;}
    static void relockIfExpired(Context c){}

    static boolean launch(Context c){
        if(!Managed.owner(c)||!Core.adminMode(c))return false;
        unlock(c);
        try{
            Intent i=c.getPackageManager().getLaunchIntentForPackage(PACKAGE);
            if(i==null)i=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+PACKAGE)).setPackage(PACKAGE);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }
}
