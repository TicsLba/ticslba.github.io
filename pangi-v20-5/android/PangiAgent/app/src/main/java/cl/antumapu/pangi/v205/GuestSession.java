package cl.antumapu.pangi.v205;

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

final class GuestSession {
    private static final String PREF = "aulacontrol_guest";
    private static final String CLEANING = "cleaning";
    private static final String STATUS = "status";
    private static final String LAST_OK = "last_ok";
    private static final Set<String> ALWAYS_CLEAN = new HashSet<>(Arrays.asList(
            "com.android.chrome","com.google.android.gm","com.google.android.apps.docs",
            "com.sec.android.app.sbrowser","org.mozilla.firefox","com.microsoft.emmx",
            "com.microsoft.office.outlook","com.google.android.apps.classroom"
    ));
    private GuestSession() {}

    private static android.content.SharedPreferences sp(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    static boolean cleaning(Context c){return sp(c).getBoolean(CLEANING,false);}
    static String status(Context c){return sp(c).getString(STATUS,"Preparando una sesión limpia…");}
    static boolean lastCleanupOk(Context c){return sp(c).getBoolean(LAST_OK,true);}
    static boolean requiresIntervention(Context c){return Managed.owner(c)&&Build.VERSION.SDK_INT>=28&&!lastCleanupOk(c);}
    static String capability(Context c){
        if(!Managed.owner(c)) return "Protección parcial";
        return Build.VERSION.SDK_INT>=28 ? "Modo invitado reforzado" : "Modo invitado compatible";
    }

    static void begin(Context c){
        Context app=c.getApplicationContext();
        if(cleaning(app)) return;
        sp(app).edit().putBoolean(CLEANING,true).putBoolean(LAST_OK,true).putString(STATUS,"Cerrando sesión y eliminando datos temporales…").apply();
        clearLocal(app);
        if(!Managed.owner(app)){
            new Handler(Looper.getMainLooper()).postDelayed(()->finish(app,true,"Sesión local restablecida · protección parcial"),700);
            return;
        }
        if(Build.VERSION.SDK_INT<28){
            new Handler(Looper.getMainLooper()).postDelayed(()->finish(app,true,"Sesión restablecida · Android 8 modo compatible"),700);
            return;
        }
        Set<String> packages=packagesToClear(app);
        if(packages.isEmpty()){finish(app,true,"Sesión restablecida");return;}
        DevicePolicyManager d=Managed.dpm(app);
        AtomicInteger left=new AtomicInteger(packages.size());
        AtomicInteger ok=new AtomicInteger();
        AtomicInteger failed=new AtomicInteger();
        ExecutorService ex=Executors.newFixedThreadPool(Math.min(3,Math.max(1,packages.size())));
        Handler main=new Handler(Looper.getMainLooper());
        main.postDelayed(()->{if(cleaning(app)){try{ex.shutdownNow();}catch(Exception ignored){}finish(app,false,"No se pudo confirmar la limpieza completa. Reintenta antes de otro usuario.");}},18000);
        for(String p:packages){
            try{
                d.clearApplicationUserData(Managed.admin(app),p,ex,(pkg,succeeded)->{
                    if(succeeded)ok.incrementAndGet(); else failed.incrementAndGet();
                    sp(app).edit().putString(STATUS,"Restableciendo aplicaciones · "+ok.get()+" correctas · "+failed.get()+" pendientes").apply();
                    if(left.decrementAndGet()==0){ex.shutdown();boolean success=failed.get()==0;main.post(()->finish(app,success,success?"Sesión restablecida":"Una o más aplicaciones no pudieron restablecerse. Reintenta la limpieza."));}
                });
            }catch(Exception e){failed.incrementAndGet();if(left.decrementAndGet()==0){ex.shutdown();main.post(()->finish(app,false,"Una o más aplicaciones no pudieron restablecerse. Reintenta la limpieza."));}}
        }
    }

    private static void clearLocal(Context c){
        try{android.content.ClipboardManager cm=(android.content.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("",""));}catch(Exception ignored){}
        try{CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();}catch(Exception ignored){}
        try{WebStorage.getInstance().deleteAllData();}catch(Exception ignored){}
        try{((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();}catch(Exception ignored){}
    }

    private static Set<String> packagesToClear(Context c){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        PackageManager pm=c.getPackageManager();
        HashSet<String> homes=new HashSet<>();
        try{Intent hi=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);for(ResolveInfo r:pm.queryIntentActivities(hi,PackageManager.MATCH_ALL))if(r.activityInfo!=null)homes.add(r.activityInfo.packageName);}catch(Exception ignored){}
        try{
            Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo r:pm.queryIntentActivities(q,PackageManager.MATCH_ALL)){
                if(r.activityInfo==null)continue;String p=r.activityInfo.packageName;if(p==null||skip(c,p,homes))continue;
                try{ApplicationInfo ai=pm.getApplicationInfo(p,0);boolean system=(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0;if(!system||ALWAYS_CLEAN.contains(p))out.add(p);}catch(Exception ignored){out.add(p);}
            }
        }catch(Exception ignored){}
        out.addAll(ALWAYS_CLEAN);
        out.removeIf(p->{try{pm.getPackageInfo(p,0);return false;}catch(Exception e){return true;}});
        return out;
    }

    private static boolean skip(Context c,String p,Set<String> homes){
        if(p.equals(c.getPackageName())||homes.contains(p))return true;
        if(p.equals("com.android.settings")||p.equals("com.android.systemui")||p.equals("com.android.vending"))return true;
        return p.contains("packageinstaller")||p.contains("permissioncontroller")||p.contains("inputmethod");
    }

    private static void finish(Context c,boolean ok,String text){
        if(!cleaning(c))return;
        sp(c).edit().putBoolean(CLEANING,false).putBoolean(LAST_OK,ok).putString(STATUS,text).apply();
        Intent i=new Intent(c,PrivacyGateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try{c.startActivity(i);}catch(Exception ignored){}
    }
}
