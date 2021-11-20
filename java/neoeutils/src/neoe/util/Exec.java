package neoe.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Exec {

	public List<String> sb;
	public boolean verbose = false;
	private String name;

	public Exec() {

	}

	public Exec(String name) {
		this.name = name;
	}

	public void setCmd(String executable) {
		sb = new ArrayList<>();
		sb.add(executable);
	}

	public void addArg(String s) {
		sb.add(s);
	}

	public void addArg(String s1, String s2) {
		sb.add(s1);
		sb.add(s2);
	}

	public String toCmdString() {
		StringBuilder b = new StringBuilder();
		for (String s : sb) {
			if (!sb.isEmpty()) {
				b.append(' ');
			}
			if (s.indexOf(' ') >= 0) {
				b.append('"').append(s).append('"');
			} else {
				b.append(s);
			}
		}
		return b.toString();
	}

	public int execute(OutputStream err, OutputStream out, InputStream in, Map env, File dir) throws Exception {
		if (verbose)
			Log.log("Exec: " + String.join(" ", sb));
		ProcessBuilder pb = new ProcessBuilder().command(sb);
		if (dir != null)
			pb.directory(dir);
		if (env != null)
			pb.environment().putAll(env);
		Process p = pb.start();
		if (err != null) {
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "stderr", err);
			errorGobbler.start();
		}
		if (out != null) {
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "stdout", out);
			outputGobbler.start();
		}
		if (in != null) {
			StreamGobbler sender = new StreamGobbler(in, "stdin", p.getOutputStream());
			sender.start();
		}
		return p.waitFor();
	}

	private static final boolean DEBUG = false;

	private class StreamGobbler extends Thread {

		InputStream in;
		String type;
		private OutputStream out;

		private StreamGobbler(InputStream is, String type, OutputStream out) {
			this.in = is;
			this.type = type;
			this.out = out;
		}

		@Override
		public void run() {
			byte[] buf = new byte[1024 * 8];
			try {
				while (true) {
					int len = in.read(buf);
					if (len <= 0)
						break;
					out.write(buf, 0, len);
					out.flush();
					if (DEBUG) {
						if ("stderr".equals(type) || len < 1024) {
							Log.log("[w]" + type + "[len]:" + len + ":" + new String(buf, 0, len));
						} else {
							Log.log("[w]" + type + "[len]:" + len);
						}
					}
				}
//				out.close();
				in.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
