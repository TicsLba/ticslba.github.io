package cl.antumapu.aulacontrol;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
    static final int COBALT=Color.rgb(49,89,255),NAVY=Color.rgb(23,43,77),TEAL=Color.rgb(32,199,164),
            SUN=Color.rgb(247,185,85),CORAL=Color.rgb(244,91,105),VIOLET=Color.rgb(124,92,252),
            INK=Color.rgb(24,32,51),MUTED=Color.rgb(101,112,134),BG=Color.rgb(244,247,253),
            LINE=Color.rgb(222,229,241),WHITE=Color.WHITE;
    final Handler ui=new Handler(Looper.getMainLooper());
    long captureAt;
    boolean awaitingCapture;
    boolean recaptureMode;
    boolean pendingRemoteCapture;
    boolean playAccessSession;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);
        pendingRemoteCapture=getIntent()!=null&&getIntent().getBooleanExtra("request_supervision",false);
        route();
    }

    @Override protected void onResume(){
        super.onResume();
        if(!awaitingCapture&&Core.ready(this))route();
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);pendingRemoteCapture=i!=null&&i.getBooleanExtra("request_supervision",false);route();}
    @Override public void onUserInteraction(){super.onUserInteraction();if(Core.guest(this)&&SessionState.isActive(this))Core.touch(this);}
    @Override public void onBackPressed(){
        if(Managed.owner(this)&&Core.adminMode(this)){closeAdmin();return;}
        if(Managed.profileOwner(this)&&SessionState.isActive(this)){openHome();return;}
        route();
    }

    void route(){
        if(Managed.profileOwner(this)){
            if(!Core.ready(this)){bootstrapError();return;}
            agent();
            SessionState.State s=SessionState.get(this);
            if(s==SessionState.State.ACTIVE){
                // Una sesión válida nunca vuelve al selector porque el proceso de
                // Aula Móvil se reinicie o MediaProjection se interrumpa.
                if(!ScreenCaptureService.active)Core.supervisionStarted(this,false);
                Managed.homeGuardOff(this);Managed.exitGate(this);sessionUi();
                if(pendingRemoteCapture&&!ScreenCaptureService.active){
                    pendingRemoteCapture=false;
                    ui.postDelayed(this::capture,300);
                }else pendingRemoteCapture=false;
                return;
            }
            if(s==SessionState.State.CLOSING){Managed.homeGuardOn(this);Managed.enterGate(this);closingUi();return;}
            Managed.homeGuardOn(this);Managed.enterGate(this);guestStartUi(null);return;
        }

        if(!Core.ready(this)){setupUi();return;}
        agent();
        if(Core.adminMode(this)){Managed.exitGate(this);adminPanel();return;}
        if(!Managed.owner(this)){managedRequiredUi();return;}
        SessionState.ownerGate(this);
        Managed.enterGate(this);
        roleGate();
    }

    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int fill,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable outline(int fill,int stroke,int width,int radius){GradientDrawable g=shape(fill,radius);g.setStroke(dp(width),stroke);return g;}
    GradientDrawable gradient(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(radius));return g;}
    TextView text(String s,int size,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
    void margins(View v,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);}

    ScrollView shell(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);return s;}
    LinearLayout root(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(22),dp(18),dp(22),dp(30));r.setBackgroundColor(BG);return r;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(18),dp(20),dp(18));c.setBackground(outline(WHITE,LINE,1,21));margins(c,0,8,0,10);return c;}
    TextView chip(String s,int fg,int bg){TextView v=text(s,11,true,fg);v.setAllCaps(true);v.setLetterSpacing(.08f);v.setPadding(dp(10),dp(6),dp(10),dp(6));v.setBackground(shape(bg,14));return v;}
    Button button(String s,int fill,int fg,int stroke){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(fg);b.setTypeface(null,1);b.setAllCaps(false);b.setPadding(dp(16),dp(13),dp(16),dp(13));b.setBackground(stroke==0?shape(fill,15):outline(fill,stroke,1,15));margins(b,0,5,0,5);return b;}
    Button primary(String s){return button(s,COBALT,WHITE,0);} Button secondary(String s){return button(s,WHITE,NAVY,LINE);} Button danger(String s){return button(s,Color.rgb(255,239,241),CORAL,Color.rgb(246,199,205));}
    EditText input(String hint,boolean pw){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(true);e.setTextColor(INK);e.setHintTextColor(Color.rgb(145,154,173));e.setPadding(dp(15),dp(13),dp(15),dp(13));e.setBackground(outline(WHITE,LINE,1,13));if(pw)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);margins(e,0,5,0,9);return e;}
    void setAnimated(View v){setContentView(v);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0).setDuration(260).start();}

    void hero(LinearLayout r,String eyebrow,String title,String subtitle,int accent){
        LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setPadding(dp(20),dp(19),dp(20),dp(20));h.setBackground(gradient(NAVY,accent,24));margins(h,0,0,0,12);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.lba_logo);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(WHITE,17));mark.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));brand.addView(mark,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(12),0,0,0);names.addView(text("Aula Móvil",22,true,WHITE));names.addView(text("11.0 · Gestión institucional",12,false,Color.rgb(221,230,255)));brand.addView(names);h.addView(brand);
        TextView e=text(eyebrow,11,true,Color.rgb(221,235,255));e.setAllCaps(true);e.setLetterSpacing(.08f);e.setPadding(0,dp(17),0,dp(5));h.addView(e);
        h.addView(text(title,29,true,WHITE));TextView s=text(subtitle,14,false,Color.rgb(234,239,255));s.setPadding(0,dp(7),0,0);s.setLineSpacing(0,1.08f);h.addView(s);r.addView(h);
    }

    LinearLayout info(String label,String value,int accent){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(0,dp(4),0,dp(8));x.addView(text(label,11,true,accent));x.addView(text(value,16,true,INK));return x;}
    LinearLayout role(String badge,String title,String desc,int accent){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.HORIZONTAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(17),dp(16),dp(15),dp(16));box.setBackground(outline(WHITE,LINE,1,19));margins(box,0,6,0,6);
        TextView b=text(badge,14,true,WHITE);b.setGravity(Gravity.CENTER);b.setBackground(gradient(accent,Color.rgb(Math.min(255,Color.red(accent)+25),Math.min(255,Color.green(accent)+25),Math.min(255,Color.blue(accent)+25)),15));box.addView(b,new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setPadding(dp(14),0,dp(8),0);w.addView(text(title,18,true,INK));TextView d=text(desc,13,false,MUTED);d.setMaxLines(2);w.addView(d);box.addView(w,new LinearLayout.LayoutParams(0,-2,1));box.addView(text("›",31,false,accent));box.setClickable(true);box.setFocusable(true);return box;
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    void setupUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"CONFIGURACIÓN INICIAL","Preparar esta tablet","Una sola configuración para convertir el equipo en un dispositivo institucional compartido.",COBALT);
        LinearLayout compat=card();compat.addView(chip(Compatibility.sessionsSupported(this)?"COMPATIBLE":"REVISAR COMPATIBILIDAD",Compatibility.sessionsSupported(this)?TEAL:CORAL,Compatibility.sessionsSupported(this)?Color.rgb(229,250,245):Color.rgb(255,239,241)));compat.addView(text(Compatibility.summary(this),16,true,NAVY));compat.addView(text("Usuarios administrados: "+yes(Compatibility.managedUsers(this))+" · Captura: "+yes(Compatibility.screenCapture(this))+" · Sesión efímera nativa: "+yes(Compatibility.ephemeralNative()),12,false,MUTED));r.addView(compat);
        LinearLayout c=card();c.addView(text("Identidad institucional",19,true,INK));TextView gap=text("Estos datos permanecen en el usuario Propietario.",13,false,MUTED);gap.setPadding(0,dp(4),0,dp(10));c.addView(gap);
        EditText dn=input("Nombre del dispositivo · Ej. TABLET-08",false),key=input("Clave técnica del establecimiento",false),p1=input("Contraseña administrativa",true),p2=input("Repetir contraseña",true);c.addView(dn);c.addView(key);c.addView(p1);c.addView(p2);r.addView(c);
        Button ok=primary("Activar Aula Móvil");r.addView(ok);ok.setOnClickListener(v->{String d=dn.getText().toString().trim(),k=key.getText().toString().trim(),p=p1.getText().toString();if(d.length()<2||k.length()<10||p.length()<6){toast("Completa los datos requeridos");return;}if(!p.equals(p2.getText().toString())){toast("Las contraseñas no coinciden");return;}Core.setup(this,d,k,p);SessionState.ownerGate(this);Managed.homeGuardOn(this);Managed.applyOwner(this);route();});
        setAnimated(sc);
    }

    String yes(boolean x){return x?"Sí":"No";}

    void managedRequiredUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"ADMINISTRACIÓN PENDIENTE","Falta activar la gestión del dispositivo","La aplicación está instalada, pero Android todavía no la reconoce como Device Owner.",CORAL);
        LinearLayout c=card();c.addView(chip("PROTECCIÓN INCOMPLETA",CORAL,Color.rgb(255,239,241)));c.addView(text("Completa el aprovisionamiento institucional desde las herramientas de instalación. No se crearán sesiones temporales hasta entonces.",14,false,MUTED));r.addView(c);setAnimated(sc);
    }

    void roleGate(){
        if(!Compatibility.sessionsSupported(this)) { incompatibleUi(); return; }
        new Thread(()->SessionUsers.cleanupSecondaryUsers(this),"AulaMovilCleanup").start();
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"ACCESO PRIORITARIO","¿Quién utilizará esta tablet?","Este acceso se protege antes de habilitar Android. Identifícate para crear un espacio temporal separado.",COBALT);
        LinearLayout d=card();d.addView(chip("DISPOSITIVO GESTIONADO",NAVY,Color.rgb(232,237,255)));d.addView(info("EQUIPO",Core.dn(this),COBALT));d.addView(text(Core.id(this)+" · "+Compatibility.summary(this),12,false,MUTED));r.addView(d);
        LinearLayout st=role("E","Estudiante","Nombre, apellido y curso · cierre por inactividad a los "+Core.idleMinutes(this,Core.ROLE_STUDENT)+" min",COBALT),te=role("P","Profesor","Nombre y apellido · cierre por inactividad a los "+Core.idleMinutes(this,Core.ROLE_TEACHER)+" min",TEAL);r.addView(st);r.addView(te);
        TextView privacy=text("Supervisión en vivo, no grabación histórica. Aula Móvil jamás registra contraseñas ni el texto que escribes.",12,false,MUTED);privacy.setPadding(dp(3),dp(10),dp(3),dp(12));r.addView(privacy);
        Button admin=secondary("Administración del dispositivo");r.addView(admin);st.setOnClickListener(v->identityForm(Core.ROLE_STUDENT));te.setOnClickListener(v->identityForm(Core.ROLE_TEACHER));admin.setOnClickListener(v->askAdmin());setAnimated(sc);
    }

    void incompatibleUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"COMPATIBILIDAD","Este modelo necesita revisión","Aula Móvil no iniciará sesiones compartidas si Android no ofrece las capacidades de aislamiento necesarias.",CORAL);
        LinearLayout c=card();c.addView(info("MODELO",Compatibility.summary(this),CORAL));c.addView(text("Usuarios administrados: "+yes(Compatibility.managedUsers(this)),14,false,INK));c.addView(text("MediaProjection: "+yes(Compatibility.screenCapture(this)),14,false,INK));c.addView(text("Administración empresarial: "+yes(Compatibility.deviceAdmin(this)),14,false,INK));r.addView(c);Button ad=secondary("Administración del dispositivo");r.addView(ad);ad.setOnClickListener(v->askAdmin());setAnimated(sc);
    }

    void identityForm(String role){
        boolean teacher=Core.ROLE_TEACHER.equals(role);ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,teacher?"PROFESOR":"ESTUDIANTE",teacher?"Tu sesión de profesor":"Tu sesión de estudiante",teacher?"Ingresa tu nombre y apellido. No se solicitarán credenciales personales.":"Ingresa nombre, apellido y curso para identificar el uso del dispositivo.",teacher?TEAL:COBALT);
        LinearLayout c=card();EditText name=input("Nombre y apellido",false);c.addView(name);EditText course=null;if(!teacher){course=input("Curso · Ej. 8° Básico",false);c.addView(course);}r.addView(c);Button go=primary("Preparar mi sesión"),back=secondary("Volver");r.addView(go);r.addView(back);EditText fcourse=course;
        go.setOnClickListener(v->{String n=name.getText().toString().trim(),co=fcourse==null?"":fcourse.getText().toString().trim();if(n.length()<3||(!teacher&&co.isEmpty())){toast("Completa los datos de la sesión");return;}prepareSession(role,n,co);});back.setOnClickListener(v->roleGate());setAnimated(sc);
    }

    void prepareSession(String role,String name,String course){
        SessionState.set(this,SessionState.State.CREATING_USER);preparingUi(role,name,course);
        new Thread(()->{
            boolean ok=SessionUsers.createAndSwitch(this,role,name,course);
            if(!ok)ui.post(()->{SessionState.ownerGate(this);Managed.enterGate(this);new AlertDialog.Builder(this).setTitle("No se pudo crear la sesión").setMessage("Android no permitió crear o cambiar al usuario temporal. Ejecuta el diagnóstico del dispositivo antes de intentarlo de nuevo.").setPositiveButton("Aceptar",(d,w)->roleGate()).show();});
        },"TabletEscolarCreateUser").start();
    }

    void preparingUi(String role,String name,String course){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"PREPARANDO TU ESPACIO","Un momento, "+first(name),"Aula Móvil está creando un usuario Android temporal y aplicando protección institucional.",VIOLET);
        LinearLayout c=card();c.addView(step("✓","Identidad confirmada",TEAL));c.addView(step("●","Creando usuario temporal",COBALT));c.addView(step("○","Aplicando protección",VIOLET));c.addView(step("○","Preparando supervisión",SUN));r.addView(c);TextView n=text((Core.ROLE_TEACHER.equals(role)?"Profesor":"Estudiante")+(course.isEmpty()?"":" · "+course),13,true,NAVY);n.setGravity(Gravity.CENTER);r.addView(n);setAnimated(sc);
    }

    LinearLayout step(String icon,String label,int color){LinearLayout x=new LinearLayout(this);x.setGravity(Gravity.CENTER_VERTICAL);x.setPadding(0,dp(7),0,dp(7));TextView i=text(icon,20,true,color);i.setGravity(Gravity.CENTER);x.addView(i,new LinearLayout.LayoutParams(dp(38),dp(38)));x.addView(text(label,15,true,INK));return x;}
    String first(String n){int x=n.indexOf(' ');return x>0?n.substring(0,x):n;}

    void bootstrapError(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"SESIÓN BLOQUEADA","No llegó la identidad de la sesión","Por seguridad no se permite configurar credenciales técnicas dentro de un usuario temporal.",CORAL);Button close=danger("Cerrar usuario temporal");r.addView(close);close.setOnClickListener(v->endGuest());setAnimated(sc);
    }

    void guestStartUi(String warning){
        Managed.homeGuardOn(this);
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"SESIÓN TEMPORAL","Hola, "+first(Core.user(this)),"Tu espacio privado está preparado. Falta iniciar la supervisión visible de esta sesión.",TEAL);
        LinearLayout c=card();c.addView(chip("ESPACIO AISLADO",TEAL,Color.rgb(229,250,245)));c.addView(info("USUARIO",Core.user(this),TEAL));c.addView(info("ROL",Core.roleLabel(this)+(Core.course(this).isEmpty()?"":" · "+Core.course(this)),COBALT));c.addView(text("Al cerrar la sesión, Android elimina este usuario temporal con sus cuentas y datos locales.",13,false,MUTED));r.addView(c);
        if(warning!=null){TextView w=text(warning,13,true,CORAL);w.setPadding(dp(14),dp(12),dp(14),dp(12));w.setBackground(shape(Color.rgb(255,239,241),14));margins(w,0,2,0,8);r.addView(w);}
        Button start=primary("Iniciar supervisión"),cancel=secondary("Cancelar y cerrar sesión");r.addView(start);r.addView(cancel);
        TextView p=text("Android mostrará su autorización oficial de captura. Aula Móvil no intenta saltar ni ocultar este consentimiento.",12,false,MUTED);p.setPadding(dp(3),dp(9),dp(3),0);r.addView(p);
        start.setOnClickListener(v->capture());cancel.setOnClickListener(v->confirmLogout());setAnimated(sc);
    }

    void capture(){
        recaptureMode=SessionState.isActive(this);
        awaitingCapture=true;captureAt=System.currentTimeMillis();
        if(!recaptureMode)SessionState.set(this,SessionState.State.WAITING_CAPTURE);
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        if(m==null){awaitingCapture=false;SessionState.guestSetup(this);guestStartUi("Este dispositivo no ofrece MediaProjection.");return;}
        try{startActivityForResult(m.createScreenCaptureIntent(),CAP);}catch(Exception e){awaitingCapture=false;SessionState.guestSetup(this);guestStartUi("Android no pudo abrir la autorización de supervisión.");}
    }

    @Override protected void onActivityResult(int rq,int rc,Intent data){
        super.onActivityResult(rq,rc,data);awaitingCapture=false;if(rq!=CAP)return;
        if(rc==RESULT_OK&&data!=null){
            Intent s=new Intent(this,ScreenCaptureService.class);s.putExtra("rc",rc);s.putExtra("data",data);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);waitingFrameUi();waitFrame();
        }else{
            Core.supervisionStarted(this,false);
            if(recaptureMode){SessionState.active(this);sessionUi();}
            else{SessionState.guestSetup(this);guestStartUi("La supervisión inicial no fue autorizada. La sesión permanece protegida.");}
        }
    }

    void waitingFrameUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"VERIFICANDO SUPERVISIÓN","Casi listo","Esperamos una imagen real de pantalla antes de entregar el control al Android normal.",TEAL);LinearLayout c=card();c.addView(step("✓","Identidad confirmada",TEAL));c.addView(step("✓","Usuario temporal creado",TEAL));c.addView(step("✓","Protección activa",TEAL));c.addView(step("●","Confirmando primer cuadro",COBALT));r.addView(c);setAnimated(sc);
    }

    void waitFrame(){
        Runnable[] q=new Runnable[1];q[0]=()->{
            if(ScreenCaptureService.active&&ScreenCaptureService.lastFrameAt>=captureAt){
                Core.supervisionStarted(this,true);Core.touch(this);SessionState.active(this);
                if(recaptureMode){sessionUi();ui.postDelayed(this::openHome,450);}else readyUi();
                return;
            }
            if(System.currentTimeMillis()-captureAt>12000){
                stopService(new Intent(this,ScreenCaptureService.class));Core.supervisionStarted(this,false);
                if(recaptureMode){SessionState.active(this);sessionUi();}
                else{SessionState.guestSetup(this);guestStartUi("Android no entregó un cuadro de pantalla válido. Inténtalo nuevamente.");}
                return;
            }
            ui.postDelayed(q[0],250);
        };ui.post(q[0]);
    }

    void readyUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"TODO LISTO","Tu sesión está protegida","Android se abrirá normalmente. Aula Móvil continuará trabajando en segundo plano.",TEAL);LinearLayout c=card();c.addView(step("✓","Identidad confirmada",TEAL));c.addView(step("✓","Sesión privada creada",TEAL));c.addView(step("✓","Protección institucional activa",TEAL));c.addView(step("✓","Supervisión en tiempo real activa",TEAL));r.addView(c);Button enter=primary("Entrar al dispositivo");r.addView(enter);enter.setOnClickListener(v->openHome());setAnimated(sc);
    }

    void sessionUi(){
        boolean live=ScreenCaptureService.active;
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"SESIÓN ACTIVA","Aula Móvil está protegiendo esta sesión",live?"Supervisión en vivo disponible.":"La sesión continúa; la consola puede solicitar nuevamente supervisión.",live?TEAL:SUN);
        LinearLayout c=card();c.addView(chip(live?"SUPERVISIÓN ACTIVA":"SIN SUPERVISIÓN",live?TEAL:SUN,live?Color.rgb(229,250,245):Color.rgb(255,247,230)));c.addView(info("USUARIO",Core.user(this),TEAL));c.addView(info("ROL",Core.roleLabel(this)+(Core.course(this).isEmpty()?"":" · "+Core.course(this)),COBALT));c.addView(text("Instalaciones bloqueadas · cuentas personales permitidas · datos aislados y temporales",12,false,MUTED));r.addView(c);
        Button home=primary("Volver a Android"),settings=secondary("Abrir Ajustes");r.addView(home);r.addView(settings);
        if(!live){Button screen=secondary("Autorizar supervisión");r.addView(screen);screen.setOnClickListener(v->capture());}
        Button logout=danger("Cerrar sesión y eliminar datos temporales");r.addView(logout);
        home.setOnClickListener(v->openHome());settings.setOnClickListener(v->openSettings());logout.setOnClickListener(v->confirmLogout());setAnimated(sc);
    }

    void openHome(){
        if(Managed.profileOwner(this)&&!SessionState.isActive(this)){SessionState.guestSetup(this);Managed.homeGuardOn(this);Managed.enterGate(this);guestStartUi("La sesión aún no está lista para abrir Android.");return;}
        if(Managed.profileOwner(this))Managed.homeGuardOff(this);
        Managed.exitGate(this);try{Intent h=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);startActivity(h);moveTaskToBack(true);}catch(Exception e){toast("No se encontró el inicio de Android");}
    }
    void openSettings(){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception e){toast("No se pudieron abrir Ajustes");}}
    void confirmLogout(){new AlertDialog.Builder(this).setTitle("Cerrar sesión").setMessage("Se eliminará el usuario Android temporal y con él las cuentas y datos locales de esta sesión.").setNegativeButton("Cancelar",null).setPositiveButton("Cerrar sesión",(d,w)->endGuest()).show();}
    void endGuest(){SessionState.set(this,SessionState.State.CLOSING);Core.supervisionStarted(this,false);Core.remoteScreenUntil(this,0);stopService(new Intent(this,ScreenCaptureService.class));Managed.homeGuardOn(this);Managed.enterGate(this);closingUi();ui.postDelayed(()->new Thread(()->SessionUsers.logoutGuest(this),"TabletEscolarLogout").start(),500);}
    void closingUi(){ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"CERRANDO SESIÓN","Protegiendo tus datos","Android está eliminando el usuario temporal antes de volver al acceso institucional.",VIOLET);LinearLayout c=card();c.addView(step("✓","Supervisión finalizada",TEAL));c.addView(step("✓","Sesión bloqueada",TEAL));c.addView(step("●","Eliminando usuario temporal",COBALT));c.addView(step("○","Volviendo a acceso institucional",VIOLET));r.addView(c);setAnimated(sc);}

    void askAdmin(){
        if(!Managed.owner(this)){toast("La administración sólo está disponible en Propietario");return;}
        EditText p=input("Contraseña administrativa",true);new AlertDialog.Builder(this).setTitle("Administración del dispositivo").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",(d,w)->{if(Core.checkPw(this,p.getText().toString())){Core.adminMode(this,true);Managed.exitGate(this);Managed.adminUnlock(this);adminPanel();}else toast("Clave incorrecta");}).show();
    }

    void adminPanel(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"MANTENIMIENTO ACTIVO","Administración del dispositivo","Las restricciones institucionales están relajadas temporalmente hasta cerrar este panel.",VIOLET);
        LinearLayout diag=card();diag.addView(chip(Compatibility.sessionsSupported(this)?"COMPATIBILIDAD COMPLETA":"COMPATIBILIDAD PARCIAL",Compatibility.sessionsSupported(this)?TEAL:CORAL,Compatibility.sessionsSupported(this)?Color.rgb(229,250,245):Color.rgb(255,239,241)));diag.addView(info("MODELO",Compatibility.summary(this),VIOLET));diag.addView(text("Usuarios administrados: "+yes(Compatibility.managedUsers(this))+" · Sesión efímera nativa: "+yes(Compatibility.ephemeralNative()),13,false,MUTED));r.addView(diag);
        Button settings=secondary("Ajustes completos de Android"),usage=secondary("Acceso de uso / inactividad"),play=secondary("Abrir Google Play"),apps=secondary("Administrar aplicaciones"),clean=secondary("Limpiar sesiones temporales residuales"),cfg=secondary("Configuración de Aula Móvil"),reset=danger("Restablecer tablet de fábrica"),release=danger("Liberar Device Owner"),done=primary("Cerrar administración y restaurar protección");r.addView(settings);r.addView(usage);r.addView(play);r.addView(apps);r.addView(clean);r.addView(cfg);r.addView(reset);r.addView(release);r.addView(done);
        settings.setOnClickListener(v->openSettings());usage.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}catch(Exception ignored){}});play.setOnClickListener(v->askPlayStore());apps.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));}catch(Exception ignored){}});clean.setOnClickListener(v->new Thread(()->SessionUsers.cleanupSecondaryUsers(this),"AulaMovilCleanup").start());cfg.setOnClickListener(v->configUi());reset.setOnClickListener(v->confirmFactoryReset());release.setOnClickListener(v->confirmRelease());done.setOnClickListener(v->closeAdmin());setAnimated(sc);
    }

    void askPlayStore(){
        if(!Managed.owner(this)||!Core.adminMode(this)){toast("Entra primero al modo Administrador");return;}
        if(!PlayStoreGuard.available(this)){toast("Google Play no está instalado en este dispositivo");return;}
        if(!PlayStoreGuard.unlockForAdmin(this)){toast("Android no permitió habilitar Google Play");return;}
        if(!PlayStoreGuard.launch(this))toast("No se pudo abrir Google Play");
    }

    void configUi(){
        ScrollView sc=shell();LinearLayout r=root();sc.addView(r);hero(r,"CONFIGURACIÓN","Identidad técnica","Modifica el nombre del equipo o establece una nueva contraseña administrativa.",COBALT);LinearLayout c=card();EditText dn=input("Nombre del dispositivo",false);dn.setText(Core.dn(this));EditText key=input("Clave técnica",false);key.setText(Core.key(this));EditText pw=input("Nueva contraseña administrativa · opcional",true);c.addView(dn);c.addView(key);c.addView(pw);r.addView(c);Button save=primary("Guardar cambios"),back=secondary("Volver");r.addView(save);r.addView(back);save.setOnClickListener(v->{if(dn.getText().toString().trim().length()<2||key.getText().toString().trim().length()<10){toast("Revisa los campos");return;}Core.config(this,dn.getText().toString(),key.getText().toString(),pw.getText().toString());toast("Configuración guardada");adminPanel();});back.setOnClickListener(v->adminPanel());setAnimated(sc);
    }

    void confirmFactoryReset(){
        if(!Managed.owner(this)||!Core.adminMode(this))return;
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(6),dp(18),0);
        EditText pw=input("Contraseña administrativa",true),phrase=input("Escribe: BORRAR TABLET",false);box.addView(pw);box.addView(phrase);
        new AlertDialog.Builder(this).setTitle("Restablecer tablet de fábrica").setMessage("Esta acción elimina usuarios, cuentas, archivos y la administración de la tablet. No se puede deshacer.").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("BORRAR",(d,w)->{
            if(!Core.checkPw(this,pw.getText().toString())){toast("Clave incorrecta");return;}
            if(!"BORRAR TABLET".equals(phrase.getText().toString().trim())){toast("Confirmación incorrecta");return;}
            RemoteRelay.eventAsync(this,"factory_reset",new org.json.JSONObject());
            if(!Managed.factoryReset(this))toast("Android no permitió el restablecimiento");
        }).show();
    }

    void closeAdmin(){PlayStoreGuard.unlock(this);Core.adminMode(this,false);Managed.restoreProtection(this);SessionState.ownerGate(this);Managed.enterGate(this);roleGate();}
    void confirmRelease(){if(!Managed.owner(this))return;EditText p=input("Repite la contraseña administrativa",true);new AlertDialog.Builder(this).setTitle("Liberar administración del dispositivo").setMessage("Se quitará Aula Móvil como Device Owner. La aplicación seguirá instalada, pero las protecciones institucionales dejarán de estar activas.").setView(p).setNegativeButton("Cancelar",null).setPositiveButton("Liberar",(d,w)->{if(!Core.checkPw(this,p.getText().toString())){toast("Clave incorrecta");return;}if(Managed.release(this)){Core.adminMode(this,false);toast("Device Owner liberado");route();}else toast("No fue posible liberar Device Owner");}).show();}
    void agent(){try{Intent i=new Intent(this,AgentService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}}
}
