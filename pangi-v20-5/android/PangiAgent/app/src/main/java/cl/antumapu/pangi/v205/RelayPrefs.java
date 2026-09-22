package cl.antumapu.pangi.v205;

import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.PersistableBundle;

final class RelayPrefs {
    static final String EXTRA="relayUrl";
    private RelayPrefs(){}
    static String url(Context c){return Core.sp(c).getString("relay_url","").trim();}
    static void url(Context c,String v){Core.sp(c).edit().putString("relay_url",v==null?"":v.trim()).apply();}
    static void setupGuest(Context c,Intent intent){
        if(intent==null)return;
        try{
            PersistableBundle b=intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
            if(b!=null){String v=b.getString(EXTRA,"");if(!v.isEmpty())url(c,v);return;}
        }catch(Exception ignored){}
        try{String v=intent.getStringExtra(EXTRA);if(v!=null&&!v.isEmpty())url(c,v);}catch(Exception ignored){}
    }
}
