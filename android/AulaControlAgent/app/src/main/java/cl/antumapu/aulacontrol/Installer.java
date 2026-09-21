package cl.antumapu.aulacontrol;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

final class Installer {
    private Installer(){}

    static void installAsync(Context c,String payload){
        new Thread(()->install(c.getApplicationContext(),payload),"AulaMovilInstaller").start();
    }

    static void install(Context c,String payload){
        File tmp=null;
        try{
            JSONObject j=new JSONObject(payload==null?"{}":payload);
            String url=j.optString("url","");
            String expected=j.optString("sha256","").toLowerCase();
            if(!(url.startsWith("http://")||url.startsWith("https://"))||expected.length()!=64)return;

            tmp=new File(c.getCacheDir(),"remote-"+System.currentTimeMillis()+".apk");
            HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();
            h.setConnectTimeout(12000);h.setReadTimeout(30000);h.setInstanceFollowRedirects(true);
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=h.getInputStream();OutputStream out=new FileOutputStream(tmp)){
                byte[] b=new byte[64*1024];int n;
                while((n=in.read(b))>0){out.write(b,0,n);md.update(b,0,n);}
            }finally{h.disconnect();}
            String got=hex(md.digest());
            if(!got.equalsIgnoreCase(expected)){tmp.delete();return;}

            PackageInstaller pi=c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams p=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            p.setInstallReason(PackageManager.INSTALL_REASON_POLICY);
            if(Build.VERSION.SDK_INT>=31)p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
            int id=pi.createSession(p);
            try(PackageInstaller.Session s=pi.openSession(id);
                InputStream in=new FileInputStream(tmp);
                OutputStream out=s.openWrite("base.apk",0,tmp.length())){
                byte[] b=new byte[64*1024];int n;while((n=in.read(b))>0)out.write(b,0,n);s.fsync(out);
                Intent result=new Intent(c,InstallResultReceiver.class).setAction("cl.antumapu.aulacontrol.INSTALL_RESULT");
                PendingIntent pending=PendingIntent.getBroadcast(c,id,result,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
                s.commit(pending.getIntentSender());
            }
        }catch(Exception ignored){}finally{if(tmp!=null)tmp.delete();}
    }

    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}
}
