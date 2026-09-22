package cl.antumapu.pangi.v205;

import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.List;

final class ImeGuard {
    private ImeGuard(){}

    static boolean ensure(Context c){
        try{
            InputMethodManager imm=(InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm==null)return false;
            List<InputMethodInfo> enabled=imm.getEnabledInputMethodList();
            List<InputMethodInfo> all=imm.getInputMethodList();
            InputMethodInfo candidate=(enabled!=null&&!enabled.isEmpty())
                    ? enabled.get(0)
                    : ((all!=null&&!all.isEmpty())?all.get(0):null);
            if(candidate==null)return false;

            try{Managed.dpm(c).setApplicationHidden(Managed.admin(c),candidate.getPackageName(),false);}catch(Exception ignored){}
            try{Managed.dpm(c).setPackagesSuspended(Managed.admin(c),new String[]{candidate.getPackageName()},false);}catch(Exception ignored){}

            String def=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            if(def==null||def.trim().isEmpty()){
                try{Managed.dpm(c).setSecureSetting(Managed.admin(c),Settings.Secure.DEFAULT_INPUT_METHOD,candidate.getId());}
                catch(Exception ignored){}
            }

            enabled=imm.getEnabledInputMethodList();
            return enabled!=null&&!enabled.isEmpty();
        }catch(Exception e){return false;}
    }

    static String summary(Context c){
        try{
            InputMethodManager imm=(InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabled=imm==null?null:imm.getEnabledInputMethodList();
            String def=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            return (def==null||def.isEmpty()?"IME sin predeterminado":def)
                    +" · habilitados "+(enabled==null?0:enabled.size());
        }catch(Exception e){return "IME no disponible";}
    }
}
