package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * Catálogo por perfil.
 * Regla por defecto: toda app de usuario instalada por Administrador se
 * comparte con Estudiante/Profesor, salvo tiendas y exclusiones explícitas.
 */
final class AppPolicy {
    static final String K_APPS="appsForSession";
    private static final String K_POLICY="app_policy_json";
    private static final Set<String> STORES=new HashSet<>(Arrays.asList(
            "com.android.vending","com.amazon.venezia","com.huawei.appmarket",
            "com.sec.android.app.samsungapps","com.xiaomi.mipicks"
    ));
    private AppPolicy(){}

    static boolean saveOwnerPolicy(Context c,String json){
        if(!Managed.owner(c))return false;
        try{
            JSONObject j=new JSONObject(json==null?"{}":json);
            // Normaliza para rechazar JSON inválido, sin almacenar texto arbitrario.
            Core.sp(c).edit().putString(K_POLICY,j.toString()).apply();
            return true;
        }catch(Exception e){return false;}
    }

    static String ownerPolicy(Context c){return Core.sp(c).getString(K_POLICY,"{}");}

    static String appsForRole(Context owner,String role){
        JSONArray out=new JSONArray();
        Set<String> deny=denied(owner,role);
        try{
            for(PackageInfo p:owner.getPackageManager().getInstalledPackages(0)){
                if(p==null||p.applicationInfo==null)continue;
                String pkg=p.packageName;
                if(pkg==null||pkg.isEmpty()||pkg.equals(owner.getPackageName())||deny.contains(pkg)||STORES.contains(pkg))continue;
                boolean system=(p.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0;
                boolean updatedSystem=(p.applicationInfo.flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0;
                if(system&&!updatedSystem)continue; // ya existe por LEAVE_ALL_SYSTEM_APPS_ENABLED
                if(owner.getPackageManager().getLaunchIntentForPackage(pkg)==null)continue;
                out.put(pkg);
            }
        }catch(Exception ignored){}
        return out.toString();
    }

    static void storeGuestApps(Context c,String json){
        Core.sp(c).edit().putString(K_APPS,json==null?"[]":json).apply();
    }

    static void applyGuest(Context c){
        if(!Managed.profileOwner(c))return;
        try{
            DevicePolicyManager d=Managed.dpm(c);
            JSONArray a=new JSONArray(Core.sp(c).getString(K_APPS,"[]"));
            for(int i=0;i<a.length();i++){
                String pkg=a.optString(i,"").trim();
                if(pkg.isEmpty()||pkg.equals(c.getPackageName())||STORES.contains(pkg))continue;
                try{d.installExistingPackage(Managed.admin(c),pkg);}catch(Exception ignored){}
                try{d.setApplicationHidden(Managed.admin(c),pkg,false);}catch(Exception ignored){}
                try{d.setPackagesSuspended(Managed.admin(c),new String[]{pkg},false);}catch(Exception ignored){}
            }
            for(String store:STORES){
                try{d.setPackagesSuspended(Managed.admin(c),new String[]{store},true);}catch(Exception ignored){}
                try{d.setApplicationHidden(Managed.admin(c),store,true);}catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }

    private static Set<String> denied(Context c,String role){
        Set<String> r=new HashSet<>(STORES);
        try{
            JSONObject p=new JSONObject(ownerPolicy(c));
            String key=Core.ROLE_TEACHER.equals(role)?"teacherDeny":"studentDeny";
            JSONArray a=p.optJSONArray(key);
            if(a!=null)for(int i=0;i<a.length();i++){String x=a.optString(i,"").trim();if(!x.isEmpty())r.add(x);}
        }catch(Exception ignored){}
        return r;
    }
}
