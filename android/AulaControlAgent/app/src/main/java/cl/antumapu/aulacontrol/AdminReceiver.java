package cl.antumapu.aulacontrol;

import android.app.admin.*;import android.content.*;import android.os.*;

public class AdminReceiver extends DeviceAdminReceiver{
 @Override public void onEnabled(Context c,Intent i){super.onEnabled(c,i);PersistableBundle b=null;try{b=i.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);}catch(Exception ignored){}if(Managed.profileOwner(c)&&b!=null)Core.setupGuest(c,b);Managed.apply(c);start(c);}
 @Override public void onProfileProvisioningComplete(Context c,Intent i){super.onProfileProvisioningComplete(c,i);PersistableBundle b=null;try{b=i.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);}catch(Exception ignored){}if(b!=null)Core.setupGuest(c,b);Managed.apply(c);start(c);}
 void start(Context c){try{Intent s=new Intent(c,AgentService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}catch(Exception ignored){}try{c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}}
}
