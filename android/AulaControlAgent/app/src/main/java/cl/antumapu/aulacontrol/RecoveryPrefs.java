package cl.antumapu.aulacontrol;

import android.content.Context;

final class RecoveryPrefs {
    private RecoveryPrefs(){}
    static boolean lost(Context c){return Core.sp(c).getBoolean("recovery_lost",false);}
    static void lost(Context c,boolean v){Core.sp(c).edit().putBoolean("recovery_lost",v).apply();}
}
