package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GateActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);if(!PolicyManager.owner(this)||!Store.configured(this)){startActivity(new Intent(this,SetupActivity.class));finish();return;}Store.state(this,Store.STATE_GATE);PolicyManager.applyOwnerGate(this);PolicyManager.startGateLock(this);startAgent();new Thread(()->SessionUsers.cleanupSecondaryUsers(this),"TabletEscolarCleanup").start();showGate();}
    @Override protected void onResume(){super.onResume();if(PolicyManager.owner(this)&&Store.configured(this)&&!Store.STATE_CREATING.equals(Store.state(this)))PolicyManager.startGateLock(this);}
    @Override public void onBackPressed(){showGate();}

    void startAgent(){try{Intent i=new Intent(this,AgentService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}}
    void showGate(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Acceso al dispositivo","Identifícate para comenzar. Android seguirá usando su launcher normal cuando tu sesión esté lista.");
        LinearLayout device=Ui.card(this);device.addView(Ui.chip(this,"DISPOSITIVO GESTIONADO",Ui.NAVY,Color.rgb(232,237,255)));device.addView(Ui.statusRow(this,Store.deviceName(this),Store.deviceId(this),Ui.TEAL,true));r.addView(device);
        LinearLayout st=Ui.roleCard(this,"E","Estudiante","Nombre, apellido y curso · sesión temporal protegida",Ui.COBALT);LinearLayout te=Ui.roleCard(this,"P","Profesor","Nombre y apellido · sesión temporal protegida",Ui.TEAL);r.addView(st);r.addView(te);
        TextView priv=Ui.text(this,"Supervisión en tiempo real, no vigilancia histórica. Sin keylogging, sin contraseñas personales y sin grabación por defecto.",12,false,Ui.MUTED);priv.setPadding(Ui.dp(this,4),Ui.dp(this,10),Ui.dp(this,4),Ui.dp(this,12));r.addView(priv);
        Button admin=Ui.secondary(this,"Administración del dispositivo");r.addView(admin);
        st.setOnClickListener(v->identity(Store.ROLE_STUDENT));te.setOnClickListener(v->identity(Store.ROLE_TEACHER));admin.setOnClickListener(v->adminLogin());setContentView(Ui.scroll(this,r));Ui.animateIn(device,60);Ui.animateIn(st,130);Ui.animateIn(te,200);Ui.animateIn(admin,290);
    }
    void identity(String role){
        boolean teacher=Store.ROLE_TEACHER.equals(role);LinearLayout r=Ui.root(this);Ui.header(this,r,teacher?"Sesión de profesor":"Sesión de estudiante",teacher?"Ingresa nombre y apellido. No se solicitan credenciales personales.":"Ingresa nombre, apellido y curso para identificar esta sesión temporal.");
        LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,teacher?"PROFESOR":"ESTUDIANTE",teacher?Ui.TEAL:Ui.COBALT,teacher?Color.rgb(229,250,245):Color.rgb(232,237,255)));EditText name=Ui.input(this,"Nombre y apellido",false);c.addView(name);EditText course=null;if(!teacher){course=Ui.input(this,"Curso · Ej. 8° Básico",false);c.addView(course);}r.addView(c);Button go=Ui.primary(this,"Preparar mi sesión"),back=Ui.secondary(this,"Volver");r.addView(go);r.addView(back);EditText finalCourse=course;
        go.setOnClickListener(v->{String n=name.getText().toString().trim(),co=finalCourse==null?"":finalCourse.getText().toString().trim();if(n.length()<3||(!teacher&&co.isEmpty())){toast("Completa los datos de la sesión");return;}prepare(role,n,co);});back.setOnClickListener(v->showGate());setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);Ui.animateIn(go,160);Ui.animateIn(back,220);
    }
    void prepare(String role,String name,String course){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Preparando tu espacio","Crearemos un usuario Android temporal separado del usuario Propietario.");LinearLayout c=Ui.card(this);c.addView(Ui.statusRow(this,"Identidad confirmada",name,Ui.TEAL,true));c.addView(Ui.statusRow(this,"Sesión privada","Creando usuario temporal…",Ui.COBALT,false));c.addView(Ui.statusRow(this,"Protección institucional","Se aplicará dentro de la sesión",Ui.GOLD,false));r.addView(c);setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);PolicyManager.stopLock(this);Store.state(this,Store.STATE_CREATING);
        new Thread(()->{boolean ok=SessionUsers.createAndSwitch(this,role,name,course);runOnUiThread(()->{if(!ok){Store.state(this,Store.STATE_GATE);PolicyManager.startGateLock(this);toast("Este modelo no pudo crear la sesión temporal");showGate();}});},"TabletEscolarCreateUser").start();
    }
    void adminLogin(){
        EditText p=Ui.input(this,"Contraseña administrativa",true);new AlertDialog.Builder(this).setTitle("Administración de Tablet Escolar").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",(d,w)->{if(Store.checkPassword(this,p.getText().toString())){PolicyManager.stopLock(this);PolicyManager.enterMaintenance(this);startActivity(new Intent(this,AdminActivity.class));}else toast("Contraseña incorrecta");}).show();
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
