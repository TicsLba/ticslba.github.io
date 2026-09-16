package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SetupActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);route();}
    void route(){
        if(PolicyManager.profileOwner(this)){startActivity(new Intent(this,SessionSetupActivity.class));finish();return;}
        if(!PolicyManager.owner(this)){pending();return;}
        if(Store.configured(this)){startActivity(new Intent(this,GateActivity.class));finish();return;}
        form();
    }
    void pending(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Preparación institucional","Tablet Escolar está instalada, pero Android todavía no la reconoce como administradora del dispositivo.");
        LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"DEVICE OWNER REQUERIDO",Ui.CORAL,Color.rgb(255,236,239)));c.addView(Ui.statusRow(this,"Aplicación instalada","Lista para aprovisionar",Ui.TEAL,true));c.addView(Ui.statusRow(this,"Administración empresarial","Pendiente",Ui.CORAL,false));TextView x=Ui.text(this,"Activa Device Owner desde el instalador institucional o ADB y vuelve a abrir Tablet Escolar. No es necesario convertirla en launcher.",14,false,Ui.MUTED);x.setPadding(0,Ui.dp(this,12),0,0);c.addView(x);r.addView(c);
        Button check=Ui.secondary(this,"Volver a comprobar");check.setOnClickListener(v->route());r.addView(check);setContentView(Ui.scroll(this,r));Ui.animateIn(c,90);Ui.animateIn(check,180);
    }
    void form(){
        LinearLayout r=Ui.root(this);Ui.header(this,r,"Configurar esta tablet","Define la identidad institucional. Esta configuración se hace una sola vez en el usuario Propietario.");
        LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"CONFIGURACIÓN INICIAL",Ui.COBALT,Color.rgb(232,237,255)));
        EditText dn=Ui.input(this,"Nombre del dispositivo · Ej. TABLET-01",false),key=Ui.input(this,"Clave técnica del establecimiento",false),p1=Ui.input(this,"Contraseña administrativa",true),p2=Ui.input(this,"Repetir contraseña",true);c.addView(dn);c.addView(key);c.addView(p1);c.addView(p2);r.addView(c);
        Button diag=Ui.secondary(this,"Comprobar compatibilidad"),go=Ui.primary(this,"Activar Tablet Escolar");r.addView(diag);r.addView(go);
        TextView note=Ui.text(this,"Tablet Escolar 3.0 no reemplaza el launcher. Durante una sesión autorizada Android funciona con su inicio normal; la app queda como agente institucional en segundo plano.",12,false,Ui.MUTED);note.setPadding(Ui.dp(this,3),Ui.dp(this,10),Ui.dp(this,3),0);r.addView(note);
        diag.setOnClickListener(v->startActivity(new Intent(this,CompatibilityActivity.class)));
        go.setOnClickListener(v->{String d=dn.getText().toString().trim(),k=key.getText().toString().trim(),a=p1.getText().toString(),b=p2.getText().toString();if(d.length()<2||k.length()<10||a.length()<6){toast("Completa los datos requeridos");return;}if(!a.equals(b)){toast("Las contraseñas no coinciden");return;}Store.configure(this,d,k,a);PolicyManager.applyOwnerGate(this);startActivity(new Intent(this,GateActivity.class));finish();});
        setContentView(Ui.scroll(this,r));Ui.animateIn(c,80);Ui.animateIn(diag,160);Ui.animateIn(go,230);
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
