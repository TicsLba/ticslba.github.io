package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CompatibilityActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);showReport();}
    boolean usageAccess(){try{AppOpsManager a=(AppOpsManager)getSystemService(APP_OPS_SERVICE);int m=a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName());return m==AppOpsManager.MODE_ALLOWED;}catch(Exception e){return false;}}
    void row(LinearLayout c,String title,String detail,boolean ok,boolean required){c.addView(Ui.statusRow(this,title,detail,ok?Ui.TEAL:(required?Ui.CORAL:Ui.GOLD),ok));}
    void showReport(){
        boolean api=Build.VERSION.SDK_INT>=26,owner=PolicyManager.owner(this)||PolicyManager.profileOwner(this),projection=getSystemService(MediaProjectionManager.class)!=null,usage=usageAccess();
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Compatibilidad del dispositivo",Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT);
        LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"DIAGNÓSTICO",Ui.COBALT,Color.rgb(232,237,255)));row(c,"Android 8 o superior",api,api?"API compatible":"Se requiere Android 8 o superior",true);row(c,"Administración empresarial",owner,owner?"Device/Profile Owner activo":"Aún no aprovisionado",true);row(c,"Usuarios temporales",api,Build.VERSION.SDK_INT>=28?"Usuario efímero nativo":"Eliminación explícita al cerrar",true);row(c,"Supervisión en vivo",projection,projection?"MediaProjection disponible":"No disponible",true);row(c,"Detección detallada de actividad",usage,usage?"Usage Access activo":"Se usará modo seguro de respaldo",false);row(c,"Ubicación institucional",true,"Compatible con proveedores Android",false);r.addView(c);
        TextView n=Ui.text(this,"Tablet Escolar no depende del fabricante ni reemplaza el launcher. La creación real de una sesión temporal es la prueba definitiva de compatibilidad multiusuario del modelo.",13,false,Ui.MUTED);n.setPadding(Ui.dp(this,4),Ui.dp(this,10),Ui.dp(this,4),Ui.dp(this,10));r.addView(n);
        Button usageBtn=Ui.secondary(this,"Abrir acceso de uso"),back=Ui.primary(this,"Volver");usageBtn.setOnClickListener(v->{try{startActivity(new android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}catch(Exception ignored){}});back.setOnClickListener(v->finish());r.addView(usageBtn);r.addView(back);setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);Ui.animateIn(usageBtn,160);Ui.animateIn(back,220);
    }
    void row(LinearLayout c,String title,boolean ok,String detail,boolean required){row(c,title,detail,ok,required);}
}
