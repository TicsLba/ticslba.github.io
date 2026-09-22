package cl.antumapu.pangi.v205;

import android.content.Context;

final class SessionState {
    enum State { GATE, CREATING_USER, SESSION_SETUP, WAITING_CAPTURE, ACTIVE, CLOSING }
    private static final String KEY = "session_state_v3";
    private SessionState() {}

    static State get(Context c) {
        String raw = Core.sp(c).getString(KEY, State.GATE.name());
        try { return State.valueOf(raw); } catch (Exception ignored) { return State.GATE; }
    }

    static void set(Context c, State state) {
        Core.sp(c).edit().putString(KEY, state.name()).apply();
    }

    static void ownerGate(Context c) { set(c, State.GATE); }
    static void guestSetup(Context c) { set(c, State.SESSION_SETUP); }
    static void active(Context c) { set(c, State.ACTIVE); }
    static boolean isActive(Context c) { return get(c) == State.ACTIVE; }
}
