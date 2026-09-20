package cl.antumapu.aulacontrol;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class PrivacyGateActivity extends Activity{
 static final int BLUE=Color.rgb(49,89,255),NAVY=Color.rgb(23,43,77),TEAL=Color.rgb(32,199,164),INK=Color.rgb(23,32,51),MUTED=Color.rgb(102,112,133),BG=Color.rgb(244,247,251),LINE=Color.rgb(221,228,239);
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);if((Managed.profileOwner(this)&&Core.ready(this))||Core.privacyAccepted(this)){go();return;}showNotice();}
 int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}GradientDrawable outline(int fill,int stroke,int r){GradientDrawable g=shape(fill,r);g.setStroke(dp(1),stroke);return g;}TextView t(String s,int z,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
 void showNotice(){
  ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(20),dp(22),dp(28));root.setBackgroundColor(BG);sc.addView(root);
  LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.HORIZONTAL);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(0,0,0,dp(20));
  LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(BLUE,18));ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_tablet_school_mark);mark.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));brand.addView(mark,new LinearLayout.LayoutParams(dp(58),dp(58)));
  LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(14),0,0,0);names.addView(t("Tablet Escolar",26,true,NAVY));names.addView(t("Privacidad y seguridad de la sesión",13,false,MUTED));brand.addView(names);root.addView(brand);
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(19),dp(20),dp(19));card.setBackground(outline(Color.WHITE,LINE,20));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
  TextView h=t("Supervisión visible, credenciales privadas",23,true,INK);card.addView(h);TextView intro=t("Tablet Escolar administra equipos institucionales con sesiones temporales y supervisión en tiempo real, sin construir un historial personal del usuario.",14,false,MUTED);intro.setPadding(0,dp(8),0,dp(12));intro.setLineSpacing(0,1.08f);card.addView(intro);
  add(card,"Sin contraseñas personales","No solicita, registra, almacena ni transmite contraseñas personales, PIN, tokens ni credenciales ingresadas por el usuario.");
  add(card,"Sin keylogging","No registra pulsaciones de teclado ni crea historiales de textos escritos.");
  add(card,"Supervisión en tiempo real","Durante una sesión activa, la consola autorizada puede ver la pantalla en vivo dentro de la red institucional configurada.");
  add(card,"Sin grabación por defecto","La imagen se usa para supervisión en vivo y no se conserva como grabación histórica por defecto.");
  add(card,"Sesión temporal","Al cerrar sesión se elimina la identidad local y el usuario temporal de Android se cierra y elimina cuando la plataforma lo permite.");
  add(card,"Protección institucional","El dispositivo puede bloquear desinstalaciones y aplicar restricciones de seguridad mientras está administrado.");
  TextView note=t("Supervisión en tiempo real, no vigilancia histórica.",14,true,TEAL);note.setPadding(0,dp(16),0,dp(8));card.addView(note);
  Button ok=new Button(this);ok.setText("Entendido · continuar");ok.setTextColor(Color.WHITE);ok.setTextSize(16);ok.setTypeface(null,1);ok.setAllCaps(false);ok.setBackground(shape(BLUE,14));ok.setPadding(dp(16),dp(13),dp(16),dp(13));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,dp(18),0,dp(6));root.addView(ok,bp);ok.setOnClickListener(v->{Core.privacyAccepted(this,true);go();});
  TextView foot=t("Tablet Escolar 4.0 · Gestión de aula y dispositivos",12,false,MUTED);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(12),0,0);root.addView(foot);setContentView(sc);
 }
 void add(LinearLayout c,String title,String body){TextView a=t(title,15,true,NAVY);a.setPadding(0,dp(10),0,dp(3));c.addView(a);TextView b=t(body,13,false,MUTED);b.setLineSpacing(0,1.08f);c.addView(b);}
 void go(){Class<?> target=GuestSession.requiresIntervention(this)?CleanupRecoveryActivity.class:MainActivity.class;Intent i=new Intent(this,target);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();}
 @Override public void onBackPressed(){if(Core.privacyAccepted(this))super.onBackPressed();}
}
