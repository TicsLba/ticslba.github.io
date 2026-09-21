package cl.antumapu.aulacontrol;

import android.content.Context;
import android.content.pm.PackageInfo;
import org.json.JSONArray;
import org.json.JSONObject;

/** Sends package names/versions only; never application data. */
final class InventoryReporter {
    private InventoryReporter(){}
    static void send(Context c){
        try{
            JSONArray a=new JSONArray();
            for(PackageInfo p:c.getPackageManager().getInstalledPackages(0)){
                JSONObject x=new JSONObject().put("package",p.packageName)
                        .put("version",p.versionName==null?"":p.versionName);
                if(android.os.Build.VERSION.SDK_INT>=28)x.put("versionCode",p.getLongVersionCode());
                else x.put("versionCode",p.versionCode);
                a.put(x);
                if(a.length()>=300)break;
            }
            RemoteRelay.eventAsync(c,"inventory",new JSONObject().put("apps",a));
        }catch(Exception ignored){}
    }
}
