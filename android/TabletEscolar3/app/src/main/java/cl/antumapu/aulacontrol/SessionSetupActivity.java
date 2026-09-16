package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class SessionSetupActivity extends Activity {
    static final int CAPTURE_REQUEST=42;
    final Handler ui=new Handler(Looper.getMainLooper());
    long captureAt;

    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);bootstrap();}
    @Override public void onBackPressed(){if(Store.STATE_ACTIVE.equals(Store.state(this)))openHome();}
    @Override public void onUserInteraction(){super.onUserInteraction();if(Store.STATE_ACTIVE.equals(Store.state(this)))Store.touch(this);}

    void bootstrap(){
        if(!PolicyManager.profileOwner(this)){startActivity(new Intent(this,SplashActivity.class));finish();return;}
        if(!Store.guest(this)||Store.user(this).isEmpty()){invalidSession();return;}
        if(Store.supervisionStarted(this)&&!CaptureService.active){closeForLostSupervision();return;}
        if(Store.STATE_ACTIVE.equals(Store.state(this))&&CaptureService.active){activePanel();return;}
        preparing();
        new Thread(()->{PolicyManager.applyGuestOnce(this);runOnUiThread(()->{PolicyManager.startSessionLock(this);startAgent();consentScreen();});},"TabletEscolarPolicySetup").start();
    }

    void startAgent(){try{Intent i=new Intent(this,AgentService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}}

    void preparing(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Preparando tu espacio","Tablet Escolar está configurando una sesión privada antes de entregarte Android.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Identidad confirmada",Store.user(this),Ui.TEAL,true));c.addView(Ui.statusRow(this,"Usuario temporal","Separado del usuario Propietario",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Protección institucional","Aplicando políticas…",Ui.COBALT,false));c.addView(Ui.statusRow(this,"Supervisión","Pendiente de autorización",Ui.GOLD,false));r.addView(c);setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);
    }

    void consentScreen(){
        Store.state(this,Store.STATE_SETUP);LinearLayout r=Ui.root(this);Ui.header(this,r,"Una última autorización","La sesión ya está aislada. Android debe autorizar la pantalla en vivo antes de abrir el escritorio normal.");
        LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"SESIÓN TEMPORAL",Ui.TEAL,Color.rgb(229,250,245)));c.addView(Ui.statusRow(this,Store.user(this),Store.roleLabel(this)+(Store.course(this).isEmpty()?"":" · "+Store.course(this)),Ui.TEAL,true));TextView p=Ui.text(this,"La consola autorizada puede ver la pantalla actual. Tablet Escolar no guarda una grabación histórica por defecto, no registra teclas y no solicita contraseñas personales.",13,false,Ui.MUTED);p.setPadding(0,Ui.dp(this,12),0,0);c.addView(p);r.addView(c);
        Button go=Ui.primary(this,"Iniciar supervisión"),cancel=Ui.secondary(this,"Cancelar y cerrar sesión");go.setOnClickListener(v->requestCapture());cancel.setOnClickListener(v->confirmLogout());r.addView(go);r.addView(cancel);setContentView(Ui.scroll(this,r));Ui.animateIn(c,70);Ui.animateIn(go,160);Ui.animateIn(cancel,220);
    }

    void requestCapture(){
        try{captureAt=System.currentTimeMillis();Store.state(this,Store.STATE_WAITING);MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),CAPTURE_REQUEST);}catch(Exception e){toast("No fue posible abrir la autorización de Android");consentScreen();}
    }

    @Override protected void onActivityResult(int rq,int rc,Intent data){
        super.onActivityResult(rq,rc,data);if(rq!=CAPTURE_REQUEST)return;
        if(rc!=RESULT_OK||data==null){consentScreen();return;}
        waitingForFrame();Intent s=new Intent(this,CaptureService.class).putExtra("resultCode",rc).putExtra("data",data);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);waitFrame();
    }

    void waitingForFrame(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Confirmando supervisión","Android autorizó la captura. Tablet Escolar espera una imagen real antes de habilitar el dispositivo.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Autorización Android","Aceptada",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Primer cuadro de pantalla","Comprobando…",Ui.COBALT,false));c.addView(Ui.statusRow(this,"Android normal","Aún bloqueado",Ui.GOLD,false));r.addView(c);setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);
    }

    void waitFrame(){
        Runnable[] q=new Runnable[1];q[0]=()->{
            if(CaptureService.active&&CaptureService.firstFrameAt>=captureAt){Store.supervisionStarted(this,true);Store.state(this,Store.STATE_ACTIVE);Store.touch(this);readyToEnter();return;}
            if(System.currentTimeMillis()-captureAt>12000){CaptureService.stop(this,true);new AlertDialog.Builder(this).setTitle("Supervisión no confirmada").setMessage("Android no entregó una imagen real a tiempo. La sesión seguirá bloqueada y puedes intentarlo nuevamente.").setPositiveButton("Aceptar",(d,w)->consentScreen()).show();return;}
            ui.postDelayed(q[0],250);
        };ui.post(q[0]);
    }

    void readyToEnter(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Todo listo, "+firstName(Store.user(this)),"Tu sesión está protegida y Android puede funcionar normalmente.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Identidad",Store.user(this),Ui.TEAL,true));c.addView(Ui.statusRow(this,"Sesión privada","Usuario temporal activo",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Protección","Desinstalaciones y cambios críticos bloqueados",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Supervisión","Pantalla en vivo confirmada",Ui.TEAL,true));r.addView(c);Button enter=Ui.primary(this,"Entrar al dispositivo");enter.setOnClickListener(v->openHome());r.addView(enter);setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);Ui.animateIn(enter,190);Ui.pulse(enter);
    }

    void activePanel(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Sesión activa","Tablet Escolar funciona en segundo plano. El dispositivo utiliza su launcher y aplicaciones normales.");LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"SUPERVISIÓN ACTIVA",Ui.TEAL,Color.rgb(229,250,245)));c.addView(Ui.statusRow(this,Store.user(this),Store.roleLabel(this)+(Store.course(this).isEmpty()?"":" · "+Store.course(this)),Ui.TEAL,true));r.addView(c);Button home=Ui.primary(this,"Volver a Android"),settings=Ui.secondary(this,"Abrir Ajustes"),logout=Ui.danger(this,"Cerrar sesión y eliminar datos temporales");home.setOnClickListener(v->openHome());settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}});logout.setOnClickListener(v->confirmLogout());r.addView(home);r.addView(settings);r.addView(logout);setContentView(Ui.scroll(this,r));Ui.animateIn(c,60);Ui.animateIn(home,140);Ui.animateIn(settings,190);Ui.animateIn(logout,240);
    }

    void openHome(){
        if(!Store.supervisionStarted(this)||!CaptureService.active){toast("La supervisión ya no está activa");closeForLostSupervision();return;}
        PolicyManager.stopLock(this);
        try{
            Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            ResolveInfo def=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);
            if(def!=null&&def.activityInfo!=null&&!getPackageName().equals(def.activityInfo.packageName))home.setComponent(new ComponentName(def.activityInfo.packageName,def.activityInfo.name));
            else{
                List<ResolveInfo> all=getPackageManager().queryIntentActivities(home,PackageManager.MATCH_ALL);for(ResolveInfo ri:all)if(ri.activityInfo!=null&&!getPackageName().equals(ri.activityInfo.packageName)){home.setComponent(new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name));break;}
            }
            startActivity(home);finish();
        }catch(Exception e){PolicyManager.startSessionLock(this);toast("No se encontró el inicio normal de Android");}
    }

    void confirmLogout(){new AlertDialog.Builder(this).setTitle("Cerrar sesión").setMessage("El usuario temporal y sus datos locales serán eliminados. Las cuentas personales abiertas durante esta sesión desaparecerán con el usuario.").setNegativeButton("Cancelar",null).setPositiveButton("Cerrar sesión",(d,w)->logout()).show();}
    void logout(){Store.state(this,Store.STATE_CLOSING);closing();CaptureService.stop(this,true);ui.postDelayed(()->{Store.clearGuestIdentity(this);if(!SessionUsers.logoutGuest(this))toast("No fue posible cerrar automáticamente. Reinicia la tablet.");},650);}
    void closeForLostSupervision(){Store.state(this,Store.STATE_CLOSING);CaptureService.stop(this,true);Store.clearGuestIdentity(this);SessionUsers.logoutGuest(this);}
    void closing(){LinearLayout r=Ui.root(this);Ui.header(this,r,"Cerrando tu sesión…","Estamos eliminando el espacio temporal y devolviendo la tablet al acceso institucional.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Supervisión","Finalizando",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Datos personales","Separados en el usuario temporal",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Usuario temporal","Eliminando…",Ui.COBALT,false));r.addView(c);setContentView(Ui.scroll(this,r));Ui.animateIn(c,60);}
    void invalidSession(){LinearLayout r=Ui.root(this);Ui.header(this,r,"Sesión no válida","La identidad institucional no llegó completa a este usuario temporal.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Protección","La sesión no se habilitará",Ui.CORAL,false));r.addView(c);Button out=Ui.danger(this,"Cerrar usuario temporal");out.setOnClickListener(v->SessionUsers.logoutGuest(this));r.addView(out);setContentView(Ui.scroll(this,r));}
    String firstName(String s){if(s==null||s.trim().isEmpty())return "";String[] p=s.trim().split("\\s+");return p[0];}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
