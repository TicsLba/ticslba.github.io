package cl.antumapu.aulacontrol;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.view.*;import android.widget.*;

public class PrivacyGateActivity extends Activity{
 static final int GREEN=Color.rgb(15,90,60),DARK=Color.rgb(8,56,38),ORANGE=Color.rgb(243,107,33),INK=Color.rgb(28,43,36),MUTED=Color.rgb(94,111,103),BG=Color.rgb(244,248,246);
 @Override public void onCreate(Bundle b){super.onCreate(b);if((Managed.profileOwner(this)&&Core.ready(this))||Core.privacyAccepted(this)){go();return;}showNotice();}
 int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}TextView t(String s,int z,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
 void showNotice(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(26),dp(22),dp(28));root.setBackgroundColor(BG);sc.addView(root);
  TextView brand=t("AulaControl",31,true,DARK);root.addView(brand);TextView sub=t("Privacidad, seguridad y sesión temporal",18,true,ORANGE);sub.setPadding(0,dp(3),0,dp(18));root.addView(sub);
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(20),dp(20),dp(20));card.setBackground(bg(Color.WHITE,20));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
  TextView h=t("Tu seguridad y tus credenciales son privadas",23,true,DARK);card.addView(h);TextView intro=t("AulaControl supervisa dispositivos institucionales sin capturar claves personales y trabaja con sesiones temporales para reducir la permanencia de datos del usuario.",15,false,MUTED);intro.setPadding(0,dp(8),0,dp(16));card.addView(intro);
  add(card,"✓ No captura contraseñas", "No solicita, registra, almacena ni transmite contraseñas personales, PIN, credenciales, tokens de autenticación ni claves ingresadas por el usuario.");
  add(card,"✓ Sin keylogging", "AulaControl no registra pulsaciones de teclado ni construye historiales de textos escritos.");
  add(card,"✓ Supervisión local", "Durante una sesión activa, la consola autorizada recibe la imagen de pantalla en vivo y datos técnicos mínimos dentro de la red institucional configurada.");
  add(card,"✓ Sesión temporal", "Al cerrar sesión se elimina la identidad local, se detiene la supervisión y el usuario temporal de Android se cierra y elimina cuando la plataforma lo permite.");
  add(card,"✓ Sin historial personal por defecto", "La consola conserva sólo eventos técnicos mínimos por dispositivo; no necesita almacenar el nombre, curso ni contenido de pantalla después de finalizar la sesión.");
  add(card,"✓ Sin grabación por defecto", "La imagen se utiliza para supervisión en vivo. AulaControl no crea ni conserva grabaciones o capturas históricas por defecto.");
  add(card,"✓ Cuidado del equipo institucional", "El dispositivo pertenece a la institución y puede ser identificado y supervisado técnicamente para su seguridad, custodia y disponibilidad.");
  TextView note=t("AulaControl no envía contraseñas ni credenciales personales a servidores del desarrollador. La supervisión se realiza entre la tablet y la consola autorizada del establecimiento.",14,true,GREEN);note.setPadding(0,dp(16),0,dp(10));card.addView(note);
  Button ok=new Button(this);ok.setText("Entendido · continuar");ok.setTextColor(Color.WHITE);ok.setTextSize(16);ok.setTypeface(null,1);ok.setAllCaps(false);ok.setBackground(bg(GREEN,14));ok.setPadding(dp(16),dp(13),dp(16),dp(13));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,dp(18),0,dp(6));root.addView(ok,bp);ok.setOnClickListener(v->{Core.privacyAccepted(this,true);go();});
  TextView foot=t("AulaControl 1.3.1 · Desarrollado por Eduardo Pérez González",12,false,MUTED);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(12),0,0);root.addView(foot);setContentView(sc);}
 void add(LinearLayout c,String title,String body){TextView a=t(title,16,true,DARK);a.setPadding(0,dp(9),0,dp(3));c.addView(a);TextView b=t(body,14,false,MUTED);b.setLineSpacing(0,1.08f);c.addView(b);}
 void go(){Class<?> target=GuestSession.requiresIntervention(this)?CleanupRecoveryActivity.class:MainActivity.class;Intent i=new Intent(this,target);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();}
 @Override public void onBackPressed(){if(Core.privacyAccepted(this))super.onBackPressed();}
}
