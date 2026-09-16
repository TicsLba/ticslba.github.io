package cl.antumapu.aulacontrol;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int COBALT=Color.rgb(49,89,255),NAVY=Color.rgb(20,33,61),TEAL=Color.rgb(32,199,164),GOLD=Color.rgb(245,180,61),CORAL=Color.rgb(244,104,116),INK=Color.rgb(29,38,58),MUTED=Color.rgb(104,115,139),BG=Color.rgb(244,247,252),LINE=Color.rgb(221,228,239),WHITE=Color.WHITE;
    private Ui(){}
    static int dp(Context c,int n){return(int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    static GradientDrawable shape(Context c,int fill,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(c,radius));return g;}
    static GradientDrawable outline(Context c,int fill,int stroke,int width,int radius){GradientDrawable g=shape(c,fill,radius);g.setStroke(dp(c,width),stroke);return g;}
    static TextView text(Context c,String s,int size,boolean bold,int color){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    static LinearLayout root(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(c,22),dp(c,20),dp(c,22),dp(c,32));r.setBackgroundColor(BG);return r;}
    static ScrollView scroll(Context c,LinearLayout child){ScrollView s=new ScrollView(c);s.setFillViewport(true);s.addView(child);return s;}
    static LinearLayout card(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(c,20),dp(c,18),dp(c,20),dp(c,18));v.setBackground(outline(c,WHITE,LINE,1,20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,7),0,dp(c,7));v.setLayoutParams(p);return v;}
    static Button button(Context c,String s,int fill,int fg,int stroke){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(fg);b.setPadding(dp(c,16),dp(c,13),dp(c,16),dp(c,13));b.setBackground(stroke==0?shape(c,fill,14):outline(c,fill,stroke,1,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,5),0,dp(c,5));b.setLayoutParams(p);return b;}
    static Button primary(Context c,String s){return button(c,s,COBALT,WHITE,0);}
    static Button secondary(Context c,String s){return button(c,s,WHITE,NAVY,LINE);}
    static Button danger(Context c,String s){return button(c,s,Color.rgb(255,244,245),CORAL,Color.rgb(246,199,204));}
    static EditText input(Context c,String hint,boolean password){EditText e=new EditText(c);e.setHint(hint);e.setTextSize(16);e.setSingleLine(true);e.setTextColor(INK);e.setHintTextColor(Color.rgb(145,154,173));e.setPadding(dp(c,15),dp(c,13),dp(c,15),dp(c,13));e.setBackground(outline(c,WHITE,LINE,1,13));if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,5),0,dp(c,9));e.setLayoutParams(p);return e;}
    static TextView chip(Context c,String s,int fg,int bg){TextView t=text(c,s,11,true,fg);t.setAllCaps(true);t.setLetterSpacing(.08f);t.setPadding(dp(c,10),dp(c,6),dp(c,10),dp(c,6));t.setBackground(shape(c,bg,14));return t;}

    static void header(Context c,LinearLayout root,String title,String subtitle){
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,0,0,dp(c,20));
        LinearLayout mark=new LinearLayout(c);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(c,COBALT,18));ImageView iv=new ImageView(c);iv.setImageResource(cl.antumapu.aulacontrol.R.drawable.ic_tablet_school);mark.addView(iv,new LinearLayout.LayoutParams(dp(c,48),dp(c,48)));row.addView(mark,new LinearLayout.LayoutParams(dp(c,58),dp(c,58)));
        LinearLayout labels=new LinearLayout(c);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(c,14),0,0,0);labels.addView(text(c,"Tablet Escolar",26,true,NAVY));labels.addView(text(c,"Gestión segura de dispositivos",13,false,MUTED));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);
        root.addView(text(c,title,28,true,INK));TextView sub=text(c,subtitle,14,false,MUTED);sub.setPadding(0,dp(c,7),0,dp(c,9));sub.setLineSpacing(0,1.08f);root.addView(sub);
    }

    static LinearLayout roleCard(Context c,String badge,String title,String body,int accent){
        LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.HORIZONTAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(c,18),dp(c,17),dp(c,16),dp(c,17));box.setBackground(outline(c,WHITE,LINE,1,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,7),0,dp(c,7));box.setLayoutParams(p);box.setClickable(true);box.setFocusable(true);
        TextView b=text(c,badge,14,true,WHITE);b.setGravity(Gravity.CENTER);b.setBackground(shape(c,accent,15));box.addView(b,new LinearLayout.LayoutParams(dp(c,50),dp(c,50)));
        LinearLayout words=new LinearLayout(c);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(c,14),0,dp(c,8),0);words.addView(text(c,title,19,true,INK));TextView d=text(c,body,13,false,MUTED);d.setLineSpacing(0,1.06f);words.addView(d);box.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text(c,"›",31,false,accent);box.addView(arrow);return box;
    }

    static LinearLayout statusRow(Context c,String label,String detail,int accent,boolean done){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,dp(c,7),0,dp(c,7));TextView dot=text(c,done?"✓":"●",18,true,accent);dot.setGravity(Gravity.CENTER);r.addView(dot,new LinearLayout.LayoutParams(dp(c,34),dp(c,34)));LinearLayout tx=new LinearLayout(c);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(text(c,label,15,true,INK));if(detail!=null&&!detail.isEmpty())tx.addView(text(c,detail,12,false,MUTED));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));return r;}

    static void animateIn(View v,long delay){v.setAlpha(0f);v.setTranslationY(dp(v.getContext(),18));v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(330).start();}
    static void pulse(View v){ObjectAnimator sx=ObjectAnimator.ofFloat(v,View.SCALE_X,1f,1.06f,1f),sy=ObjectAnimator.ofFloat(v,View.SCALE_Y,1f,1.06f,1f);AnimatorSet set=new AnimatorSet();set.playTogether(sx,sy);set.setDuration(480);set.start();}
    static void immersive(Activity a){a.getWindow().setStatusBarColor(NAVY);a.getWindow().setNavigationBarColor(NAVY);}
}
