package cl.antumapu.pangi.v205;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

final class FileTransfer {
    private FileTransfer(){}

    static void downloadAsync(Context c,String payload){
        new Thread(()->download(c.getApplicationContext(),payload),"AulaMovilFile").start();
    }

    static void download(Context c,String payload){
        File tmp=null;
        try{
            JSONObject j=new JSONObject(payload==null?"{}":payload);
            String url=j.optString("url",""),sha=j.optString("sha256","").toLowerCase(),name=safe(j.optString("name","archivo"));
            if(!(url.startsWith("https://")||url.startsWith("http://"))||sha.length()!=64||name.isEmpty())return;
            tmp=new File(c.getCacheDir(),"file-"+System.currentTimeMillis());
            HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(12000);h.setReadTimeout(45000);h.setInstanceFollowRedirects(true);
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=h.getInputStream();OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[64*1024];int n;while((n=in.read(b))>0){out.write(b,0,n);md.update(b,0,n);}}finally{h.disconnect();}
            if(!hex(md.digest()).equalsIgnoreCase(sha))return;

            if(Build.VERSION.SDK_INT>=29){
                ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,"application/octet-stream");v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/AulaMovil");v.put(MediaStore.Downloads.IS_PENDING,1);
                Uri u=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)return;
                try(OutputStream out=c.getContentResolver().openOutputStream(u);InputStream in=new FileInputStream(tmp)){if(out==null)return;byte[] b=new byte[64*1024];int n;while((n=in.read(b))>0)out.write(b,0,n);}
                v.clear();v.put(MediaStore.Downloads.IS_PENDING,0);c.getContentResolver().update(u,v,null,null);
            }else{
                File dir=new File(c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"AulaMovil");dir.mkdirs();File out=new File(dir,name);
                try(InputStream in=new FileInputStream(tmp);OutputStream o=new FileOutputStream(out)){byte[] b=new byte[64*1024];int n;while((n=in.read(b))>0)o.write(b,0,n);}
            }
        }catch(Exception ignored){}finally{if(tmp!=null)tmp.delete();}
    }

    static String safe(String n){
        if(n==null)return"";n=n.replace('\\','_').replace('/','_').replace(':','_').trim();return n.length()>120?n.substring(n.length()-120):n;
    }
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}
}
