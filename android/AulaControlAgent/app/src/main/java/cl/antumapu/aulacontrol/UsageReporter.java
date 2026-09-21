package cl.antumapu.aulacontrol;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Auditoría de aplicaciones en primer plano.
 * No usa AccessibilityService, no registra teclas, texto, campos ni contenido.
 */
final class UsageReporter {
    private UsageReporter(){}
    private static final String K_LAST="usage_report_last";

    static void tick(Context c){
        if(c==null||!Core.guest(c)||!SessionState.isActive(c)||!Core.usageAccess(c))return;
        try{
            long now=System.currentTimeMillis();
            long from=Math.max(Core.sp(c).getLong(K_LAST,now-65_000L),now-10*60_000L);
            UsageStatsManager m=(UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE);
            if(m==null)return;
            UsageEvents es=m.queryEvents(from,now);
            UsageEvents.Event e=new UsageEvents.Event();
            JSONArray rows=new JSONArray();
            String lastPkg="";
            while(es!=null&&es.hasNextEvent()){
                es.getNextEvent(e);
                int t=e.getEventType();
                if(t!=UsageEvents.Event.ACTIVITY_RESUMED&&t!=UsageEvents.Event.MOVE_TO_FOREGROUND)continue;
                String pkg=e.getPackageName();
                if(pkg==null||pkg.isEmpty()||pkg.equals(lastPkg)||pkg.equals(c.getPackageName()))continue;
                lastPkg=pkg;
                rows.put(new JSONObject().put("ts",e.getTimeStamp()).put("package",pkg));
                if(rows.length()>=40)break;
            }
            Core.sp(c).edit().putLong(K_LAST,now).apply();
            if(rows.length()>0)RemoteRelay.eventAsync(c,"app_usage",new JSONObject().put("events",rows));
        }catch(Exception ignored){}
    }

    static void managedUrl(Context c,String url){
        try{
            if(url==null||url.isEmpty())return;
            android.net.Uri u=android.net.Uri.parse(url);
            String host=u.getHost();
            RemoteRelay.eventAsync(c,"managed_url",new JSONObject()
                    .put("ts",System.currentTimeMillis())
                    .put("host",host==null?"":host)
                    .put("url",url));
        }catch(Exception ignored){}
    }
}
