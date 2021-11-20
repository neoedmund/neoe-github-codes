//neoe(c)
package neoe.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * features: (1) fail okay (2) performance (3) Configurable
 *
 * @author neoe
 */
public class Log {

	static public String DEFAULT = "neoe";

	final static Map<String, Log> cache = new HashMap<String, Log>();

	public boolean stdout = false;

	public boolean debug = true;

	public synchronized static Log getLog(String name) {
		Log log = cache.get(name);
		if (log == null) {
			log = new Log(name, "log-" + name + ".log");
			cache.put(name, log);
		}
		return log;
	}

	private Date now = new Date();
	private File f;
	private SideQueue sq;

	public void close() {
		if (sq != null) {
			sq.close();
			sq = null;
		}
	}

	public void criticalMode() {
		sq.intervalMs = 1;
	}

	public static String defaultLogDir = "/tmp";

	private Log(String name, String fn) {
		try {
			f = new File(defaultLogDir, fn);
			System.out.println("Log " + name + ":" + f.getAbsolutePath());

			final SimpleDateFormat time = new SimpleDateFormat("yyMMdd H:m:s:S");
			sq = new SideQueue(1000, new SideQueue.Batch() {
				@Override
				public Object run(List data) throws Exception {

					StringBuilder sb = new StringBuilder();
					for (Object o : data) {
						Object[] row = (Object[]) o;
						Throwable t = (Throwable) row[1];
						String s0 = row[0].toString();
						long ts = (Long) row[2];
						now.setTime(ts);
						if (!debug && s0.startsWith("[D]"))
							continue;
						sb.append(time.format(now)).append(" ").append(s0);
						while (true) {
							if (sb.length() <= 0)
								break;
							char c = sb.charAt(sb.length() - 1);
							if (c == '\r' || c == '\n')
								sb.setLength(sb.length() - 1);
							else
								break;
						}
						if (t == null)
							sb.append("\r\n");
						else
							sb.append(", Error:\r\n");

						if (t != null) {
							ByteArrayOutputStream baos;
							PrintStream tos = new PrintStream(baos = new ByteArrayOutputStream());
							t.printStackTrace(tos);
							tos.close();
							sb.append(baos.toString("utf8"));
						}
					}
					PrintWriter out = getPrintWriter(f);
					out.write(sb.toString());
					out.close();
					if (stdout)
						System.out.print(sb.toString());
					return null;
				}
			}, false);
		} catch (Exception ex) {
//			f = null;
//			sq = null;
			System.out.println("cannot init log file:" + ex);
		}
	}

	private static synchronized PrintWriter getPrintWriter(File f) throws IOException {
		return new PrintWriter(new OutputStreamWriter(new FileOutputStream(f, true), "utf8"), true);
	}

	public static void logTo(String name, Object msg) {
		Log.getLog(name).log0(msg);
	}

	public static void logTo(String name, Object msg, Throwable t) {
		Log.getLog(name).log0(msg, t);
	}

	public static void log(Object msg) {
		Log.getLog(DEFAULT).log0(msg);
	}

	public static void log(Object msg, Throwable t) {
		Log.getLog(DEFAULT).log0(msg, t);
	}

	public static Log getLog() {
		return Log.getLog(DEFAULT);
	}

	public synchronized void log0(Object o, Throwable t) {
		if (sq == null || o == null) {
			return;
		}
		sq.add(new Object[] { o, t, System.currentTimeMillis() });
	}

	public synchronized void log0(Object o) {
		log0(o, null);
	}

	public synchronized void logs0(String fmt, Object... args) {
		log0(String.format(fmt, args));
	}

	public static void logs(String fmt, Object... args) {
		log(String.format(fmt, args));
	}

	/** only call it when application exit outside JVM */
	public static void flush() {
		SideQueue sq = Log.getLog(DEFAULT).sq;
		if (sq != null)
			sq.oneTurn();
	}
//	public static PrintWriter getWriter() {
//		Log a = Log.getLog(DEFAULT);
//		return new PrintWriter(a.out) {
//			public void write(String str) {
//				write(str, 0, str.length());
//				if (stdout)
//					System.out.print(str);
//			}
//
//			public void println() {
//				write("\n");
//			}
//		};
//	}
}
