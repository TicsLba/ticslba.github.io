package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AdminActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);if(!PolicyManager.owner(this)||!Store.configured(this)){finish();return;}PolicyManager.enterMaintenance(this);panel();}
    @Override public void onBackPressed(){closeAdmin();}

    void panel(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Administración","Modo de mantenimiento del usuario Propietario. Las restricciones críticas se restauran al salir.");LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"MANTENIMIENTO ACTIVO",Ui.GOLD,Color.rgb(255,247,225)));c.addView(Ui.statusRow(this,Store.deviceName(this),Store.deviceId(this),Ui.TEAL,true));r.addView(c);
        Button settings=Ui.secondary(this,"Ajustes completos de Android"),apps=Ui.secondary(this,"Administrar aplicaciones"),compat=Ui.secondary(this,"Diagnóstico de compatibilidad"),config=Ui.secondary(this,"Configuración de Tablet Escolar"),relay=Ui.secondary(this,"Configurar relay remoto"),clean=Ui.secondary(this,"Limpiar sesiones temporales residuales"),release=Ui.danger(this,"Liberar Device Owner"),close=Ui.primary(this,"Cerrar administración y proteger");
        settings.setOnClickListener(v->open(Settings.ACTION_SETTINGS));apps.setOnClickListener(v->open(Settings.ACTION_APPLICATION_SETTINGS));compat.setOnClickListener(v->startActivity(new Intent(this,CompatibilityActivity.class)));config.setOnClickListener(v->configDialog());relay.setOnClickListener(v->relayDialog());clean.setOnClickListener(v->{new Thread(()->SessionUsers.cleanupSecondaryUsers(this)).start();toast("Limpieza solicitada");});release.setOnClickListener(v->releaseDialog());close.setOnClickListener(v->closeAdmin());
        r.addView(settings);r.addView(apps);r.addView(compat);r.addView(config);r.addView(relay);r.addView(clean);r.addView(release);r.addView(close);setContentView(Ui.scroll(this,r));Ui.animateIn(c,50);Ui.animateIn(settings,120);Ui.animateIn(close,260);
    }
    void open(String action){try{startActivity(new Intent(action));}catch(Exception ignored){}}
    void configDialog(){
        LinearLayout box=Ui.root(this);box.setPadding(Ui.dp(this,6),Ui.dp(this,4),Ui.dp(this,6),0);EditText dn=Ui.input(this,"Nombre del dispositivo",false),key=Ui.input(this,"Clave técnica",false),pw=Ui.input(this,"Nueva contraseña · opcional",true);dn.setText(Store.deviceName(this));key.setText(Store.technicalKey(this));box.addView(dn);box.addView(key);box.addView(pw);
        new AlertDialog.Builder(this).setTitle("Configuración").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{String n=dn.getText().toString().trim(),k=key.getText().toString().trim(),p=pw.getText().toString();if(n.length()<2||k.length()<10){toast("Revisa los campos");return;}if(p.isEmpty())p="__KEEP__";updateConfig(n,k,p);panel();}).show();
    }
    void updateConfig(String name,String key,String password){
        if("__KEEP__".equals(password)){String salt=Store.sp(this).getString("pw_salt",""),hash=Store.sp(this).getString("pw_hash","");Store.sp(this).edit().putString("device_name",name).putString("technical_key",Crypto.encryptLocal(key)).putString("pw_salt",salt).putString("pw_hash",hash).apply();}
        else if(password.length()>=6)Store.configure(this,name,key,password);else toast("La nueva contraseña debe tener al menos 6 caracteres");
    }
    void relayDialog(){EditText e=Ui.input(this,"https://… · vacío para desactivar",false);e.setText(Store.relay(this));new AlertDialog.Builder(this).setTitle("Relay remoto cifrado").setMessage("El relay transporta telemetría cifrada y comandos autorizados; no transmite la pantalla en vivo.").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{String u=e.getText().toString().trim();if(!u.isEmpty()&&!u.toLowerCase().startsWith("https://")){toast("El relay debe usar HTTPS");return;}Store.relay(this,u);toast(u.isEmpty()?"Relay desactivado":"Relay guardado");}).show();}
    void releaseDialog(){EditText p=Ui.input(this,"Contraseña administrativa",true);new AlertDialog.Builder(this).setTitle("Liberar Device Owner").setMessage("Se quitará la administración institucional de Android. Tablet Escolar seguirá instalada, pero sus protecciones dejarán de ser obligatorias.").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Liberar",(d,w)->{if(!Store.checkPassword(this,p.getText().toString())){toast("Contraseña incorrecta");return;}if(PolicyManager.releaseOwner(this)){toast("Device Owner liberado");startActivity(new Intent(this,SetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}else toast("No fue posible liberar Device Owner");}).show();}
    void closeAdmin(){PolicyManager.applyOwnerGate(this);startActivity(new Intent(this,GateActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
