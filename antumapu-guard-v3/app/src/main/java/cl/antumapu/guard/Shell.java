package cl.antumapu.guard;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class Shell {
    private Shell() {}

    public static Result su(String command) {
        try {
            Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append('\n');
            r.close();
            int code = p.waitFor();
            return new Result(code == 0, code, out.toString());
        } catch (Exception e) {
            return new Result(false, -1, e.toString());
        }
    }

    public static boolean hasRoot() {
        Result r = su("id");
        return r.ok && r.output.contains("uid=0");
    }

    public static final class Result {
        public final boolean ok;
        public final int code;
        public final String output;
        Result(boolean ok, int code, String output) {
            this.ok = ok; this.code = code; this.output = output;
        }
    }
}
