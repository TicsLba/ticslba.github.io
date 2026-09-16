package cl.antumapu.aulacontrol;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import java.util.List;

final class LocationTracker {
    private LocationTracker(){}
    static void request(Context c){
        try{
            if(c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
            LocationManager m=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);if(m==null)return;
            Location best=null;for(String p:m.getProviders(true)){try{Location x=m.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}catch(Exception ignored){}}
            if(best!=null)save(c,best);
            LocationListener l=new LocationListener(){@Override public void onLocationChanged(Location x){save(c,x);try{m.removeUpdates(this);}catch(Exception ignored){}}@Override public void onStatusChanged(String p,int s,Bundle e){}@Override public void onProviderEnabled(String p){}@Override public void onProviderDisabled(String p){}};
            try{m.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,l,null);}catch(Exception e){try{m.requestSingleUpdate(LocationManager.GPS_PROVIDER,l,null);}catch(Exception ignored){}}
        }catch(Exception ignored){}
    }
    static void save(Context c,Location x){Store.sp(c).edit().putString("lat",Double.toString(x.getLatitude())).putString("lon",Double.toString(x.getLongitude())).putString("loc_acc",Float.toString(x.getAccuracy())).putLong("loc_time",x.getTime()).apply();}
    static String lat(Context c){return Store.sp(c).getString("lat","");}
    static String lon(Context c){return Store.sp(c).getString("lon","");}
    static String accuracy(Context c){return Store.sp(c).getString("loc_acc","");}
    static long time(Context c){return Store.sp(c).getLong("loc_time",0);}
}
