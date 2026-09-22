package cl.antumapu.pangi.v205;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.Locale;

final class LocationTracker {
    private LocationTracker(){}
    static final String K_LAT="loc_lat",K_LON="loc_lon",K_ACC="loc_acc",K_TS="loc_ts";

    static void request(Context c){
        try{
            if(c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
               c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return;
            LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            if(lm==null)return;
            Location best=null;
            for(String p:lm.getProviders(true)){
                try{Location x=lm.getLastKnownLocation(p);if(better(x,best))best=x;}catch(Exception ignored){}
            }
            if(best!=null)save(c,best);
            Criteria cr=new Criteria();cr.setAccuracy(Criteria.ACCURACY_FINE);cr.setPowerRequirement(Criteria.POWER_MEDIUM);
            String p=lm.getBestProvider(cr,true);
            if(p==null){if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))p=LocationManager.NETWORK_PROVIDER;else if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))p=LocationManager.GPS_PROVIDER;}
            if(p==null)return;
            final String provider=p;
            Handler h=new Handler(Looper.getMainLooper());
            h.post(()->{
                try{lm.requestSingleUpdate(provider,new LocationListener(){
                    @Override public void onLocationChanged(Location l){save(c,l);}
                    @Override public void onProviderEnabled(String p){}
                    @Override public void onProviderDisabled(String p){}
                    @Override public void onStatusChanged(String p,int s,Bundle b){}
                },Looper.getMainLooper());}catch(Exception ignored){}
            });
        }catch(Exception ignored){}
    }

    static boolean available(Context c){return Core.sp(c).getLong(K_TS,0)>0;}
    static String lat(Context c){return Core.sp(c).getString(K_LAT,"");}
    static String lon(Context c){return Core.sp(c).getString(K_LON,"");}
    static String accuracy(Context c){return Core.sp(c).getString(K_ACC,"");}
    static long time(Context c){return Core.sp(c).getLong(K_TS,0);}

    static void clear(Context c){Core.sp(c).edit().remove(K_LAT).remove(K_LON).remove(K_ACC).remove(K_TS).apply();}

    private static void save(Context c,Location l){
        if(l==null)return;
        long ts=l.getTime()>0?l.getTime():System.currentTimeMillis();
        Core.sp(c).edit()
                .putString(K_LAT,String.format(Locale.US,"%.6f",l.getLatitude()))
                .putString(K_LON,String.format(Locale.US,"%.6f",l.getLongitude()))
                .putString(K_ACC,String.format(Locale.US,"%.1f",l.hasAccuracy()?l.getAccuracy():-1f))
                .putLong(K_TS,ts).apply();
    }

    private static boolean better(Location a,Location b){
        if(a==null)return false;if(b==null)return true;
        if(a.getTime()>b.getTime()+120000)return true;
        if(b.getTime()>a.getTime()+120000)return false;
        return !a.hasAccuracy() || !b.hasAccuracy() ? a.getTime()>b.getTime() : a.getAccuracy()<b.getAccuracy();
    }
}
