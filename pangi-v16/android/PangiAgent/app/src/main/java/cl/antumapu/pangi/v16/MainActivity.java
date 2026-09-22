package cl.antumapu.pangi.v16;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OBSIDIAN=Color.rgb(17,24,32);
    private static final int PETROL=Color.rgb(18,56,68);
    private static final int GREEN=Color.rgb(24,167,124);
    private static final int GREEN_SOFT=Color.rgb(90,216,177);
    private static final int IVORY=Color.rgb(245,247,247);
    private static final int MUTED=Color.rgb(168,184,185);
    private static final int AMBER=Color.rgb(242,184,75);
    private static final int CORAL=Color.rgb(231,101,101);
    private static final int CARD=Color.rgb(25,35,43);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(OBSIDIAN);
        getWindow().setNavigationBarColor(OBSIDIAN);
        route();
    }

    @Override protected void onResume(){
        super.onResume();
        if(!isFinishing())route();
    }

    @Override public void onBackPressed(){
        if(PangiPolicy.isProfileOwner(this)){
            if(PangiStore.STATE_ACTIVE.equals(PangiStore.state(this)))openAndroid();
            else route();
            return;
        }
        if(PangiPolicy.isOwner(this)&&PangiStore.adminMode(this)){
            adminPanel();
            return;
        }
        route();
    }

    private void route(){
        if(PangiPolicy.isProfileOwner(this)){
            if(PangiStore.STATE_ACTIVE.equals(PangiStore.state(this)))guestActive();
            else if(PangiStore.STATE_CLOSING.equals(PangiStore.state(this)))closing();
            else guestPrepared();
            return;
        }

        if(!PangiStore.configured(this)){
            setup();
            return;
        }

        if(!PangiPolicy.isOwner(this)){
            notManaged();
            return;
        }

        if(PangiStore.adminMode(this)){
            adminPanel();
            return;
        }

        PangiPolicy.prepareOwnerGate(this);
        PangiPolicy.startGate(this);
        roleGate();
    }

    private ScrollView shell(){
        ScrollView s=new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(OBSIDIAN);
        LinearLayout r=new LinearLayout(this);
        r.setId(android.R.id.content);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(24),dp(22),dp(24),dp(36));
        s.addView(r);
        return s;
    }

    private LinearLayout root(ScrollView s){return (LinearLayout)s.getChildAt(0);}

    private void brand(LinearLayout r,String eyebrow,String title,String subtitle){
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.CENTER);
        top.setPadding(dp(20),dp(22),dp(20),dp(22));
        GradientDrawable g=new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,new int[]{PETROL,OBSIDIAN});
        g.setCornerRadius(dp(24));
        top.setBackground(g);

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.pangi_mark);
        top.addView(logo,new LinearLayout.LayoutParams(dp(92),dp(92)));

        TextView n=text("PANGI",31,true,IVORY);
        n.setGravity(Gravity.CENTER);
        top.addView(n);
        TextView v=text("V16",12,true,GREEN_SOFT);
        v.setGravity(Gravity.CENTER);
        v.setLetterSpacing(.20f);
        top.addView(v);

        TextView e=text(eyebrow,11,true,GREEN_SOFT);
        e.setAllCaps(true);
        e.setLetterSpacing(.08f);
        e.setGravity(Gravity.CENTER);
        e.setPadding(0,dp(16),0,dp(5));
        top.addView(e);

        TextView t=text(title,25,true,IVORY);t.setGravity(Gravity.CENTER);top.addView(t);
        TextView sub=text(subtitle,13,false,MUTED);sub.setGravity(Gravity.CENTER);
        sub.setPadding(dp(8),dp(8),dp(8),0);top.addView(sub);
        r.addView(top,fullMargins(0,0,0,16));
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18),dp(17),dp(18),dp(17));
        GradientDrawable g=new GradientDrawable();
        g.setColor(CARD);g.setCornerRadius(dp(18));
        g.setStroke(dp(1),Color.rgb(45,61,68));
        c.setBackground(g);
        return c;
    }

    private Button button(String label,int fill,int fg){
        Button b=new Button(this);
        b.setText(label);b.setTextSize(15);b.setTextColor(fg);b.setTypeface(null,1);
        b.setAllCaps(false);b.setPadding(dp(14),dp(13),dp(14),dp(13));
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(15));b.setBackground(g);
        b.setLayoutParams(fullMargins(0,6,0,6));
        return b;
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        if(bold)v.setTypeface(null,1);return v;
    }

    private EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(118,139,143));
        e.setTextColor(IVORY);e.setSingleLine(true);e.setTextSize(16);e.setPadding(dp(14),dp(12),dp(14),dp(12));
        GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(18,27,34));g.setCornerRadius(dp(12));
        g.setStroke(dp(1),Color.rgb(52,70,77));e.setBackground(g);
        if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setLayoutParams(fullMargins(0,5,0,7));
        return e;
    }

    private void setup(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"CONFIGURACIÓN INICIAL","Preparar PANGI","Nueva instalación institucional. Esta Alpha 1 valida Device Owner, HOME protegido, usuarios temporales y teclado.");

        LinearLayout c=card();
        EditText device=input("Nombre del dispositivo · ej. PANGI-TEST-01",false);
        EditText key=input("Clave técnica institucional",true);
        EditText p1=input("Contraseña de administrador",true);
        EditText p2=input("Repetir contraseña",true);
        c.addView(device);c.addView(key);c.addView(p1);c.addView(p2);
        r.addView(c,fullMargins(0,0,0,12));

        Button save=button("Activar PANGI V16",GREEN,Color.WHITE);
        r.addView(save);
        save.setOnClickListener(v->{
            String d=device.getText().toString().trim();
            String k=key.getText().toString();
            String a=p1.getText().toString();
            if(d.length()<3||k.length()<10||a.length()<6){toast("Completa los campos requeridos.");return;}
            if(!a.equals(p2.getText().toString())){toast("Las contraseñas no coinciden.");return;}
            PangiStore.configureOwner(this,d,k,a);
            if(PangiPolicy.isOwner(this))PangiPolicy.prepareOwnerGate(this);
            route();
        });
        setContentView(s);
    }

    private void notManaged(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"DEVICE OWNER","Falta activar la administración","PANGI está configurado, pero Android aún no lo reconoce como Device Owner.");
        LinearLayout c=card();
        c.addView(text("Paquete",12,true,GREEN_SOFT));
        c.addView(text(getPackageName(),16,true,IVORY));
        TextView cmd=text("Activa Device Owner antes de continuar con el piloto.",13,false,MUTED);
        cmd.setPadding(0,dp(10),0,0);c.addView(cmd);r.addView(c);
        setContentView(s);
    }

    private void roleGate(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"DISPOSITIVO PROTEGIDO","¿Quién usará esta tablet?","PANGI prepara primero un usuario Android aislado. El launcher no se libera durante la transición.");

        LinearLayout status=card();
        status.addView(text(PangiStore.deviceName(this),17,true,IVORY));
        status.addView(text("PANGI V16 Alpha 1 · Device Owner activo",12,false,GREEN_SOFT));
        r.addView(status,fullMargins(0,0,0,12));

        Button student=button("Estudiante  ·  sesión temporal",GREEN,Color.WHITE);
        Button teacher=button("Profesor/a  ·  sesión temporal",PETROL,Color.WHITE);
        Button admin=button("Administración",Color.rgb(48,58,65),IVORY);
        r.addView(student);r.addView(teacher);r.addView(admin);
        student.setOnClickListener(v->identity(PangiStore.ROLE_STUDENT));
        teacher.setOnClickListener(v->identity(PangiStore.ROLE_TEACHER));
        admin.setOnClickListener(v->askAdmin());
        setContentView(s);
    }

    private void identity(String role){
        boolean teacher=PangiStore.ROLE_TEACHER.equals(role);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),0,dp(18),0);
        EditText name=input("Nombre y apellido",false);box.addView(name);
        EditText course=null;
        if(!teacher){course=input("Curso · ej. 8° Básico",false);box.addView(course);}
        final EditText fcourse=course;

        new AlertDialog.Builder(this)
                .setTitle(teacher?"Sesión Profesor/a":"Sesión Estudiante")
                .setView(box)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Preparar",(d,w)->{
                    String n=name.getText().toString().trim();
                    String co=fcourse==null?"":fcourse.getText().toString().trim();
                    if(n.length()<3||(!teacher&&co.isEmpty())){toast("Faltan datos.");return;}
                    prepare(role,n,co);
                }).show();
    }

    private void prepare(String role,String name,String course){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"PREPARACIÓN SEGURA","Preparando sesión","PANGI está creando el usuario en segundo plano. No cambiará de usuario hasta recibir confirmación real de HOME y teclado.");
        LinearLayout c=card();
        c.addView(text("● Creando usuario temporal",15,true,GREEN_SOFT));
        c.addView(text("● Aplicando políticas del perfil",14,false,MUTED));
        c.addView(text("● Verificando HOME PANGI",14,false,MUTED));
        c.addView(text("● Verificando teclado / IME",14,false,MUTED));
        r.addView(c);
        setContentView(s);

        new Thread(()->{
            SessionCoordinator.Result result=SessionCoordinator.createAndSwitch(this,role,name,course);
            if(!result.ok)runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle("Sesión no iniciada")
                    .setMessage(result.message+"\n\nPANGI mantuvo el usuario propietario protegido.")
                    .setPositiveButton("Aceptar",(d,w)->route()).show());
        },"PangiCreateSession").start();
    }

    private void guestPrepared(){
        PangiPolicy.pinHome(this);
        PangiPolicy.startGate(this);
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"SESIÓN TEMPORAL","Sesión preparada","PANGI tomó HOME antes de mostrar este usuario. Ahora podemos validar que el launcher no apareció durante el cambio.");

        LinearLayout c=card();
        c.addView(text("✓ HOME PANGI preparado antes del cambio",14,true,GREEN_SOFT));
        c.addView(text("✓ Usuario temporal y efímero",14,true,GREEN_SOFT));
        c.addView(text("✓ "+PangiPolicy.imeSummary(this),13,false,IVORY));
        c.addView(text("Usuario: "+PangiStore.userName(this),13,false,MUTED));
        if(!PangiStore.course(this).isEmpty())c.addView(text("Curso: "+PangiStore.course(this),13,false,MUTED));
        r.addView(c,fullMargins(0,0,0,12));

        Button enter=button("Entrar a Android",GREEN,Color.WHITE);
        Button close=button("Cancelar y eliminar sesión",Color.rgb(74,45,48),Color.WHITE);
        r.addView(enter);r.addView(close);
        enter.setOnClickListener(v->{
            if(!PangiPolicy.ensureIme(this)){toast("PANGI no liberará Android: no hay teclado utilizable.");return;}
            PangiStore.state(this,PangiStore.STATE_ACTIVE);
            openAndroid();
        });
        close.setOnClickListener(v->confirmCloseGuest());
        setContentView(s);
    }

    private void guestActive(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"SESIÓN ACTIVA","PANGI continúa activo","Abrir nuevamente la aplicación no destruye la sesión ni devuelve al selector.");
        LinearLayout c=card();
        c.addView(text("Usuario: "+PangiStore.userName(this),15,true,IVORY));
        c.addView(text(PangiPolicy.imeSummary(this),12,false,GREEN_SOFT));
        c.addView(text("Estado persistente: ACTIVE",12,false,MUTED));
        r.addView(c,fullMargins(0,0,0,12));
        Button home=button("Volver a Android",GREEN,Color.WHITE);
        Button settings=button("Abrir Ajustes",Color.rgb(48,58,65),IVORY);
        Button close=button("Cerrar sesión y eliminar datos",Color.rgb(95,49,53),Color.WHITE);
        r.addView(home);r.addView(settings);r.addView(close);
        home.setOnClickListener(v->openAndroid());
        settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}});
        close.setOnClickListener(v->confirmCloseGuest());
        setContentView(s);
    }

    private void openAndroid(){
        if(!PangiPolicy.isProfileOwner(this))return;
        if(!PangiStore.STATE_ACTIVE.equals(PangiStore.state(this)))return;
        PangiPolicy.stopGate(this);
        PangiPolicy.releaseHome(this);
        try{
            Intent h=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(h);
            moveTaskToBack(true);
        }catch(Exception e){toast("Android no encontró un launcher disponible.");}
    }

    private void confirmCloseGuest(){
        new AlertDialog.Builder(this).setTitle("Cerrar sesión")
                .setMessage("Android eliminará este usuario temporal junto con sus cuentas y datos locales.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Cerrar y eliminar",(d,w)->closeGuest()).show();
    }

    private void closeGuest(){
        if(!PangiPolicy.isProfileOwner(this))return;
        PangiStore.state(this,PangiStore.STATE_CLOSING);
        PangiPolicy.pinHome(this);
        PangiPolicy.startGate(this);
        closing();
        getWindow().getDecorView().postDelayed(()->new Thread(()->{
            try{
                int result=PangiPolicy.dpm(this).logoutUser(PangiPolicy.admin(this));
                if(result!=UserManager.USER_OPERATION_SUCCESS)runOnUiThread(()->toast("Android respondió "+result+" al cerrar la sesión."));
            }catch(Exception e){runOnUiThread(()->toast("No se pudo cerrar la sesión."));}
        },"PangiLogout").start(),350);
    }

    private void closing(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"CIERRE SEGURO","Eliminando sesión","PANGI mantiene el HOME protegido mientras Android cierra el usuario efímero.");
        LinearLayout c=card();
        c.addView(text("● Cerrando usuario temporal",15,true,AMBER));
        c.addView(text("● Eliminando cuentas y datos locales",13,false,MUTED));
        r.addView(c);setContentView(s);
    }

    private void askAdmin(){
        EditText pass=input("Contraseña administrativa",true);
        new AlertDialog.Builder(this).setTitle("Administración PANGI")
                .setView(pass).setNegativeButton("Cancelar",null)
                .setPositiveButton("Entrar",(d,w)->{
                    if(!PangiStore.checkAdmin(this,pass.getText().toString())){toast("Contraseña incorrecta.");return;}
                    PangiStore.adminMode(this,true);
                    PangiPolicy.stopGate(this);
                    PangiPolicy.releaseHome(this);
                    adminPanel();
                }).show();
    }

    private void adminPanel(){
        ScrollView s=shell();LinearLayout r=root(s);
        brand(r,"ADMINISTRACIÓN","Modo administrador","Alpha 1 mantiene este panel simple: acceso Android, diagnóstico y retorno protegido.");
        LinearLayout c=card();
        c.addView(text("Device Owner: "+(PangiPolicy.isOwner(this)?"Sí":"No"),14,true,GREEN_SOFT));
        c.addView(text("Paquete: "+getPackageName(),12,false,MUTED));
        c.addView(text("Versión: 16.0.0-alpha1",12,false,MUTED));
        r.addView(c,fullMargins(0,0,0,12));
        Button android=button("Abrir Android",GREEN,Color.WHITE);
        Button cleanup=button("Eliminar sesiones temporales residuales",Color.rgb(48,58,65),IVORY);
        Button done=button("Cerrar administración y proteger",PETROL,Color.WHITE);
        r.addView(android);r.addView(cleanup);r.addView(done);
        android.setOnClickListener(v->{
            PangiPolicy.stopGate(this);PangiPolicy.releaseHome(this);
            try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));moveTaskToBack(true);}catch(Exception ignored){}
        });
        cleanup.setOnClickListener(v->new Thread(()->SessionCoordinator.cleanupSecondaryUsers(this),"PangiCleanup").start());
        done.setOnClickListener(v->{
            PangiStore.adminMode(this,false);
            PangiPolicy.prepareOwnerGate(this);
            PangiPolicy.startGate(this);
            roleGate();
        });
        setContentView(s);
    }

    private LinearLayout.LayoutParams fullMargins(int l,int t,int r,int b){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;
    }

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
