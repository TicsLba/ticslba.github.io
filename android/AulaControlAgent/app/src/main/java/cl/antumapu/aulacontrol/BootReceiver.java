package cl.antumapu.aulacontrol;

import android.content.*;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){
  if(!Core.ready(c))return;
  try{var s=new Intent(c,AgentService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}catch(Exception ignored){}
  if(RecoveryPrefs.lost(c)){
   try{c.startActivity(new Intent(c,LostModeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}
  }
 }
}
