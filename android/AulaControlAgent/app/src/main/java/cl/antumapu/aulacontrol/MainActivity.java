package cl.antumapu.aulacontrol;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    static final int CAP=42;
    static final int BLUE=Color.rgb(49,89,255),NAVY=Color.rgb(23,43,77),TEAL=Color.rgb(32,199,164),
            GOLD=Color.rgb(245,180,61),RED=Color.rgb(202,61,71),INK=Color.rgb(23,32,51),
            MUTED=Color.rgb(102,112,133),BG=Color.rgb(244,247,251),LINE=Color.rgb(221,228,239),WHITE=Color.WHITE;
    Handler ui=new Handler(Looper.getMainLooper());
    long captureAt;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);
        route();
    }

    @Override protected void onResume(){super.onResume();if(Core.ready(this))route();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);route();}
    @Override public void onUserInteraction(){super.onUserInteraction();if(Core.guest(this)&&!Core.user(this).isEmpty())Core.touch(this);}

    @Override public void onBackPressed(){
        if(Managed.owner(this)&&Core.adminMode(this)){closeAdmin();return;}
        if(Managed.profileOwner(this)&&ScreenCaptureService.active){openHome();return;}
        route();
    }

    void route(){
        if(Managed.profileOwner(this)){
            if(!Core.ready(this)){guestBootstrapErrorUi();return;}
            agent();Managed.applyGuest(this);guestUi();return;
        }
        if(!Core.ready(this)){setupUi();return;}
        agent();Managed.apply(this);
        if(Core.adminMode(this)){adminPanel();return;}
        if(!Managed.owner(this)){managedRequiredUi();return;}
        Managed.enterGate(this);
        roleGate();
    }

    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int fill,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable outline(int fill,int stroke,int width,int radius){GradientDrawable g=shape(fill,radius);g.setStroke(dp(width),stroke);return g;}
    TextView text(String s,int size,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
    void margins(View v,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);}

    LinearLayout root(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(22),dp(18),dp(22),dp(30));r.setBackgroundColor(BG);return r;
    }
    LinearLayout card(){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(18),dp(20),dp(18));c.setBackground(outline(WHITE,LINE,1,20));margins(c,0,8,0,10);return c;
    }
    TextView chip(String s,int fg,int bg){
        TextView v=text(s,11,true,fg);v.setAllCaps(true);v.setLetterSpacing(.08f);v.setPadding(dp(10),dp(6),dp(10),dp(6));v.setBackground(shape(bg,14));return v;
    }
    Button button(String s,int fill,int fg,int stroke){
        Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(fg);b.setTypeface(null,1);b.setAllCaps(false);b.setPadding(dp(16),dp(13),dp(16),dp(13));
        b.setBackground(stroke==0?shape(fill,14):outline(fill,stroke,1,14));margins(b,0,5,0,5);return b;
    }
    Button primary(String s){return button(s,BLUE,WHITE,0);}
    Button secondary(String s){return button(s,WHITE,NAVY,LINE);}
    Button danger(String s){return button(s,Color.rgb(255,244,245),RED,Color.rgb(244,197,201));}

    EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(true);e.setTextColor(INK);e.setHintTextColor(Color.rgb(145,154,173));e.setPadding(dp(15),dp(13),dp(15),dp(13));e.setBackground(outline(WHITE,LINE,1,13));
        if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);margins(e,0,5,0,9);return e;
    }

    void brandHeader(LinearLayout r,String title,String subtitle){
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.HORIZONTAL);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(0,0,0,dp(20));
        LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(BLUE,18));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(58),dp(58));mark.setLayoutParams(mp);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_tablet_school_mark);icon.setContentDescription("Tablet Escolar");mark.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));brand.addView(mark);
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(14),0,0,0);names.addView(text("Tablet Escolar",26,true,NAVY));names.addView(text("Gestión de aula y dispositivos",13,false,MUTED));brand.addView(names);r.addView(brand);
        r.addView(text(title,27,true,INK));TextView s=text(subtitle,14,false,MUTED);s.setLineSpacing(0,1.08f);s.setPadding(0,dp(6),0,dp(10));r.addView(s);
    }

    LinearLayout infoRow(String label,String value,int accent){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,dp(4),0,dp(7));row.addView(text(label,11,true,accent));row.addView(text(value,16,true,INK));return row;
    }

    LinearLayout roleCard(String tag,String title,String description,int accent){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.HORIZONTAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(18),dp(16),dp(16),dp(16));box.setBackground(outline(WHITE,LINE,1,18));margins(box,0,6,0,6);
        TextView badge=text(tag,13,true,WHITE);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(accent,14));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(48),dp(48));badge.setLayoutParams(bp);box.addView(badge);
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(14),0,dp(8),0);words.addView(text(title,18,true,INK));TextView d=text(description,13,false,MUTED);d.setMaxLines(2);words.addView(d);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);box.addView(words,wp);
        TextView arrow=text("›",30,false,accent);box.addView(arrow);box.setClickable(true);box.setFocusable(true);return box;
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    void setupUi(){
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Preparar esta tablet","Configura una sola vez el equipo institucional. Después, estudiantes y profesores usarán sesiones temporales separadas del usuario Propietario.");
        LinearLayout c=card();c.addView(chip("CONFIGURACIÓN INICIAL",BLUE,Color.rgb(232,237,255)));TextView gap=text("Identidad del dispositivo",19,true,INK);gap.setPadding(0,dp(14),0,dp(5));c.addView(gap);
        EditText dn=input("Nombre del dispositivo · Ej. TABLET-08",false),key=input("Clave técnica del establecimiento",false),p1=input("Contraseña administrativa",true),p2=input("Repetir contraseña",true);
        c.addView(dn);c.addView(key);c.addView(p1);c.addView(p2);r.addView(c);
        Button ok=primary("Activar Tablet Escolar");r.addView(ok);
        TextView n=text("La clave técnica se cifra con Android Keystore. La contraseña administrativa se almacena únicamente como hash derivado en Propietario.",12,false,MUTED);n.setPadding(dp(3),dp(8),dp(3),0);r.addView(n);
        ok.setOnClickListener(v->{String d=dn.getText().toString().trim(),k=key.getText().toString().trim(),p=p1.getText().toString();if(d.length()<2||k.length()<10||p.length()<6){toast("Completa los datos requeridos");return;}if(!p.equals(p2.getText().toString())){toast("Las contraseñas no coinciden");return;}Core.setup(this,d,k,p);agent();route();});
        setContentView(sc);
    }

    void guestBootstrapErrorUi(){
        LinearLayout r=root();brandHeader(r,"No se pudo abrir la sesión","La información institucional no llegó completa al usuario temporal. Por seguridad no se permite configurar la aplicación aquí.");
        LinearLayout c=card();c.addView(chip("SESIÓN BLOQUEADA",RED,Color.rgb(255,236,238)));c.addView(text("Vuelve a Propietario y crea la sesión nuevamente. Nunca ingreses la clave técnica ni la contraseña administrativa dentro de una sesión temporal.",14,false,MUTED));r.addView(c);
        Button out=secondary("Cerrar sesión temporal");r.addView(out);out.setOnClickListener(v->{if(!SessionUsers.logoutGuest(this))toast("No fue posible cerrar automáticamente. Reinicia la tablet.");});setContentView(r);
    }

    void managedRequiredUi(){
        LinearLayout r=root();brandHeader(r,"Administración pendiente","La instalación está correcta, pero esta tablet aún no está aprovisionada como Device Owner.");
        LinearLayout c=card();c.addView(chip("PROTECCIÓN INCOMPLETA",RED,Color.rgb(255,236,238)));c.addView(text("Las sesiones temporales y la protección de aplicaciones permanecen deshabilitadas hasta completar el aprovisionamiento institucional.",14,false,MUTED));r.addView(c);setContentView(r);
    }

    void roleGate(){
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Acceso al dispositivo","Identifícate para comenzar. Cada sesión de estudiante o profesor se crea en un usuario Android temporal independiente.");
        LinearLayout device=card();device.addView(chip("DISPOSITIVO GESTIONADO",NAVY,Color.rgb(232,237,255)));device.addView(infoRow("EQUIPO",Core.dn(this),BLUE));device.addView(text(Core.id(this),12,false,MUTED));r.addView(device);
        LinearLayout st=roleCard("E","Estudiante","Nombre, apellido y curso. Cierre automático tras 10 min de inactividad.",BLUE);
        LinearLayout te=roleCard("P","Profesor","Nombre y apellido. Sesión temporal con 30 min de inactividad.",TEAL);r.addView(st);r.addView(te);
        TextView priv=text("Privacidad: supervisión en tiempo real, no vigilancia histórica. Sin keylogging, sin contraseñas personales y sin grabación por defecto.",12,false,MUTED);priv.setPadding(dp(4),dp(10),dp(4),dp(12));r.addView(priv);
        Button ad=secondary("Administración del dispositivo");r.addView(ad);
        st.setOnClickListener(v->identityForm(Core.ROLE_STUDENT));te.setOnClickListener(v->identityForm(Core.ROLE_TEACHER));ad.setOnClickListener(v->askAdmin());setContentView(sc);
    }

    void identityForm(String role){
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);boolean teacher=Core.ROLE_TEACHER.equals(role);
        brandHeader(r,teacher?"Sesión de profesor":"Sesión de estudiante",teacher?"Ingresa tu nombre y apellido. No se solicitarán credenciales personales.":"Ingresa tu nombre, apellido y curso para identificar esta sesión temporal.");
        LinearLayout c=card();c.addView(chip(teacher?"PROFESOR":"ESTUDIANTE",teacher?TEAL:BLUE,teacher?Color.rgb(229,250,245):Color.rgb(232,237,255)));
        EditText name=input("Nombre y apellido",false);c.addView(name);EditText course=null;if(!teacher){course=input("Curso · Ej. 8° Básico",false);c.addView(course);}r.addView(c);
        Button go=primary("Crear sesión temporal");Button back=secondary("Volver");r.addView(go);r.addView(back);EditText finalCourse=course;
        go.setOnClickListener(v->{String n=name.getText().toString().trim(),co=finalCourse==null?"":finalCourse.getText().toString().trim();if(n.length()<3||(!teacher&&co.isEmpty())){toast("Completa los datos de la sesión");return;}Managed.exitGate(this);if(!SessionUsers.createAndSwitch(this,role,n,co)){toast("No fue posible crear el usuario temporal");Managed.enterGate(this);}});
        back.setOnClickListener(v->roleGate());setContentView(sc);
    }

    void guestUi(){
        if(Core.user(this).isEmpty()){
            LinearLayout r=root();brandHeader(r,"Sesión temporal no válida","No se encontró una identidad de sesión válida.");Button out=secondary("Cerrar usuario temporal");r.addView(out);out.setOnClickListener(v->SessionUsers.logoutGuest(this));setContentView(r);return;
        }
        Managed.applyGuest(this);
        if(!ScreenCaptureService.active){guestStartUi();return;}
        sessionUi();
    }

    void guestStartUi(){
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Sesión lista","Antes de habilitar el inicio normal de Android, Tablet Escolar debe confirmar la supervisión en vivo de esta sesión.");
        LinearLayout c=card();c.addView(chip("SESIÓN TEMPORAL",TEAL,Color.rgb(229,250,245)));c.addView(infoRow("USUARIO",Core.user(this),TEAL));c.addView(text(Core.roleLabel(this)+(Core.course(this).isEmpty()?"":" · "+Core.course(this)),14,true,NAVY));r.addView(c);
        Button go=primary("Iniciar sesión segura");r.addView(go);
        TextView p=text("Android mostrará su autorización oficial de captura. El inicio del sistema sólo se habilita después de confirmar al menos un cuadro real de pantalla.",13,false,MUTED);p.setPadding(dp(3),dp(9),dp(3),dp(9));r.addView(p);
        Button out=secondary("Cancelar y cerrar sesión");r.addView(out);go.setOnClickListener(v->capture());out.setOnClickListener(v->endGuest());setContentView(sc);
    }

    void capture(){captureAt=System.currentTimeMillis();MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),CAP);}

    @Override protected void onActivityResult(int rq,int rc,Intent data){
        super.onActivityResult(rq,rc,data);
        if(rq==CAP){
            if(rc==RESULT_OK&&data!=null){Intent s=new Intent(this,ScreenCaptureService.class);s.putExtra("rc",rc);s.putExtra("data",data);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);waitFrame();}
            else guestStartUi();
        }
    }

    void waitFrame(){
        Runnable[] q=new Runnable[1];
        q[0]=()->{
            if(ScreenCaptureService.active&&ScreenCaptureService.lastFrameAt>=captureAt){Core.supervisionStarted(this,true);Core.touch(this);Managed.enableGuestLauncher(this);agent();openHome();return;}
            if(System.currentTimeMillis()-captureAt>9000){stopService(new Intent(this,ScreenCaptureService.class));new AlertDialog.Builder(this).setTitle("Supervisión no confirmada").setMessage("La sesión seguirá bloqueada hasta que Android entregue una imagen real de la pantalla.").setPositiveButton("Aceptar",(d,w)->guestStartUi()).show();return;}
            ui.postDelayed(q[0],250);
        };
        ui.post(q[0]);
    }

    void sessionUi(){
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Sesión activa","Tablet Escolar protege la sesión en segundo plano mientras utilizas el launcher y las aplicaciones normales del dispositivo.");
        LinearLayout c=card();c.addView(chip("SUPERVISIÓN ACTIVA",TEAL,Color.rgb(229,250,245)));c.addView(infoRow("USUARIO",Core.user(this),TEAL));c.addView(text(Core.roleLabel(this)+(Core.course(this).isEmpty()?"":" · "+Core.course(this)),14,true,NAVY));TextView secure=text("Desinstalaciones bloqueadas · sesión temporal · pantalla en vivo",12,false,MUTED);secure.setPadding(0,dp(7),0,0);c.addView(secure);r.addView(c);
        Button home=primary("Ir al inicio de Android"),settings=secondary("Abrir Ajustes"),logout=danger("Cerrar sesión y eliminar datos temporales");r.addView(home);r.addView(settings);r.addView(logout);
        home.setOnClickListener(v->openHome());settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}});logout.setOnClickListener(v->confirmLogout());setContentView(sc);
    }

    void openHome(){try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){toast("No se encontró el inicio del sistema");}}
    void confirmLogout(){new AlertDialog.Builder(this).setTitle("Cerrar sesión").setMessage("Se eliminará este usuario temporal y sus datos locales. Las cuentas personales que permanezcan abiertas desaparecerán junto con la sesión.").setNegativeButton("Cancelar",null).setPositiveButton("Cerrar sesión",(d,w)->endGuest()).show();}
    void endGuest(){Core.clearIdentity(this);stopService(new Intent(this,ScreenCaptureService.class));if(!SessionUsers.logoutGuest(this))toast("No fue posible cerrar automáticamente. Reinicia la tablet.");}

    void askAdmin(){
        if(!Managed.owner(this)){toast("La administración está disponible sólo en Propietario");return;}
        EditText p=input("Contraseña administrativa",true);
        new AlertDialog.Builder(this).setTitle("Administración de Tablet Escolar").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",(d,w)->{
            if(Core.checkPw(this,p.getText().toString())){Core.adminMode(this,true);Managed.exitGate(this);Managed.adminUnlock(this);adminPanel();}else toast("Clave incorrecta");
        }).show();
    }

    void adminPanel(){
        if(!Managed.owner(this)){route();return;}
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Administración","Modo de mantenimiento del usuario Propietario. Las restricciones se relajan temporalmente hasta cerrar este panel.");
        LinearLayout s=card();s.addView(chip("MANTENIMIENTO ACTIVO",NAVY,Color.rgb(232,237,255)));s.addView(text("Desde aquí puedes modificar Ajustes, aplicaciones y parámetros técnicos. Las sesiones de estudiante y profesor nunca tienen acceso a este modo.",14,false,MUTED));r.addView(s);
        Button settings=secondary("Ajustes completos de Android"),store=secondary("Abrir Play Store"),apps=secondary("Administrar aplicaciones"),cfg=secondary("Configuración de Tablet Escolar"),clean=secondary("Limpiar sesiones temporales residuales"),release=danger("Liberar Device Owner"),restore=primary("Cerrar administración y restaurar protección");
        r.addView(settings);r.addView(store);r.addView(apps);r.addView(cfg);r.addView(clean);r.addView(release);r.addView(restore);
        settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}});
        store.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+getPackageName())));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));}catch(Exception ignored){}}});
        apps.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));}catch(Exception ignored){}});
        cfg.setOnClickListener(v->configUi());clean.setOnClickListener(v->{SessionUsers.cleanupSecondaryUsers(this);toast("Limpieza solicitada");});release.setOnClickListener(v->confirmRelease());restore.setOnClickListener(v->closeAdmin());setContentView(sc);
    }

    void configUi(){
        if(!Managed.owner(this)){route();return;}
        ScrollView sc=new ScrollView(this);LinearLayout r=root();sc.addView(r);
        brandHeader(r,"Configuración","Modifica la identidad técnica del dispositivo o establece una nueva contraseña administrativa.");
        LinearLayout c=card();EditText dn=input("Nombre del dispositivo",false);dn.setText(Core.dn(this));EditText key=input("Clave técnica",false);key.setText(Core.key(this));EditText pw=input("Nueva contraseña administrativa · opcional",true);c.addView(dn);c.addView(key);c.addView(pw);r.addView(c);
        Button save=primary("Guardar cambios"),back=secondary("Volver");r.addView(save);r.addView(back);
        save.setOnClickListener(v->{if(dn.getText().toString().trim().length()<2||key.getText().toString().trim().length()<10){toast("Revisa los campos");return;}Core.config(this,dn.getText().toString(),key.getText().toString(),pw.getText().toString());toast("Configuración guardada");adminPanel();});back.setOnClickListener(v->adminPanel());setContentView(sc);
    }

    void closeAdmin(){Core.adminMode(this,false);Managed.restoreProtection(this);if(Managed.owner(this))Managed.enterGate(this);route();}

    void confirmRelease(){
        if(!Managed.owner(this))return;
        EditText p=input("Repite la contraseña administrativa",true);
        new AlertDialog.Builder(this).setTitle("Liberar administración del dispositivo").setMessage("Se quitará Tablet Escolar como Device Owner y se reducirán las protecciones institucionales. Para recuperarlas será necesario aprovisionar nuevamente la tablet.").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Liberar",(d,w)->{
            if(Core.checkPw(this,p.getText().toString())){if(Managed.release(this)){Core.adminMode(this,false);toast("Device Owner liberado");route();}else toast("No fue posible liberar Device Owner");}else toast("Clave incorrecta");
        }).show();
    }

    void agent(){try{Intent i=new Intent(this,AgentService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}}
}
