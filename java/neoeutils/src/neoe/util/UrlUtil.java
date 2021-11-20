package neoe.util;

import java.util.zip.GZIPInputStream;
import java.util.*;
import java.io.*;
import java.net.*;

public class UrlUtil {
	public void addHeader(Map m) {
		reqHeader.putAll(m);
	}

	public void setProxy(String ip, int port, String userAndPass) {
		if (ip == null) {
			useProxy = false;
			return;
		}
		useProxy = true;
		proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(ip, port));
		if (userAndPass != null) {
			reqHeader.put("Proxy-Authorization", "Basic " + Base64.encodeBytes(userAndPass.getBytes()));
		}
	}

	// Context ct;
	public String enc = "UTF-8";

	public UrlUtil setAgentFirefox() {
		reqHeader.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:58.0) Gecko/20100101 Firefox/70.0");
		return this;
	}

	public UrlUtil download(String url) throws Exception {
		this.url = url;
		run();
		return this;
	}

	public String getPage() throws Exception {
		if (ba == null)
			return "not downloaded";
		return new String(ba, enc);
	}

	public void savePage(String path) throws Exception {
		if (ba == null) {
			Log.log("[W]no content for url[" + url + "] to " + path);
			return;
		}
		FileUtil.save(ba, path);
		Log.log(String.format("[D]save %d bytes for url[%s] to %s", ba.length, url, path));
	}

	String url;
	boolean useProxy;
	public boolean readContent = true;
	public byte[] ba;
	Proxy proxy;
	Map reqHeader = new HashMap();
	public Map respHeader;
	int retry;
	public int MAX_TRY = 10;
	public int code;
	
	public static final byte[] emptyBA = new byte[0];

	public void run() throws Exception {
		ba = emptyBA;
		retry = 0;
		while (true) {
			retry += 1;
			// safeguard
			if (retry > MAX_TRY)
				return;

			URL u = new URL(url);
			HttpURLConnection conn;
			if (useProxy) {
				Log.log(String.format("[D]connect to %s via [%s]", url, proxy));
				conn = (HttpURLConnection) u.openConnection(proxy);

			} else {
				Log.log(String.format("[D]connect to %s", url));
				conn = (HttpURLConnection) u.openConnection();
			}
			if (hasPostData()) {
				conn.setDoOutput(true); // trigger POST
			}
			// set headers
			for (Object o : reqHeader.keySet()) {
				conn.setRequestProperty((String) o, reqHeader.get(o).toString());
			}

			if (hasPostDataFile()) {
				postDataMultipart(conn);
			} else if (hasPostData()) {
				postDataUrlencode(conn.getOutputStream());
			}

			boolean error = false;
			Exception ex1 = null;
			try {
				code = conn.getResponseCode();
				respHeader = conn.getHeaderFields();

				if (readContent) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					FileUtil.copy(conn.getInputStream(), baos);
					ba = baos.toByteArray();
					int len1 = ba.length;
					{ // gzip enc
						Object encoding = respHeader.get("Content-Encoding");
						if (encoding == null) {
							encoding = respHeader.get("content-encoding");
						}
						// Log.log("encoding="+encoding);
						if (encoding != null && encoding.toString().toLowerCase().indexOf("gzip") >= 0) {
							// gzipped
							GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(ba));
							ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
							FileUtil.copy(gzip, baos2);
							ba = baos2.toByteArray();
							int len2 = ba.length;
							Log.log(String.format("[D]extract gzip %d bytes -> %d bytes", len1, len2));
						}
					}

					Log.log(String.format("[D]downloaded %s(%d bytes)", url, ba.length));
				}

			} catch (Exception ex) {
				error = true;
				ex1 = ex;
			}
			if (error) {
				Log.log("found error:" + ex1);
				String errorString = "" + ex1;
				if (errorString.indexOf("java.io.FileNotFoundException") >= 0) {
					Log.log("should " + "be 404, skip");
					return;
				}

				// panic
				throw new RuntimeException("panic", ex1);

			} else {
				return;
			}
		}
	}

	final String ENC = "UTF8";

	private void postDataUrlencode(OutputStream out) throws IOException {
		StringBuilder sb = new StringBuilder();

		params.forEach((k, v) -> {
			if (sb.length() > 0) {
				sb.append("&");
			}
			try {
				sb.append(URLEncoder.encode(k.toString(), enc));
				sb.append("=");
				sb.append(URLEncoder.encode(v.toString(), enc));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		});
		String s = sb.toString();
		Log.log(s);
		out.write(s.getBytes(enc));
	}

	private void postDataMultipart(URLConnection conn) throws IOException {
		String boundary = null;
		for (int i = 0; i < 5; i++) {
			boundary = genBoundary();
			if (foundBoundaryInParams(boundary) || foundBoundaryInFiles(boundary)) {
				boundary = null;
			} else {
				break;
			}
		}
		if (boundary == null) {
			Etc.panic("failed to generate boundary");
		}

		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		OutputStream out = conn.getOutputStream();
		// output
		StringBuilder sb = new StringBuilder();
		final String boundary1 = boundary;
		params.forEach((k, v) -> {
			sb.append("--").append(boundary1).append(RN);
			sb.append("Content-Disposition: form-data; name=\"").append(k).append("\"").append(RN).append(RN);
			sb.append(v).append(RN);
		});
		output(out, sb);
		files.forEach((k, v) -> {
			Object[] row = (Object[]) v;
			sb.append("--").append(boundary1).append(RN);
			sb.append("Content-Disposition: form-data; name=\"").append(k).append("\"; filename=\"").append(row[0])
					.append("\"").append(RN).append(RN);
			try {
				output(out, sb);
				out.write((byte[]) row[1]);
				out.write(RN.getBytes());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		sb.append("--").append(boundary1).append("--").append(RN);
		output(out, sb);
	}

	private void output(OutputStream out, StringBuilder sb) {
		try {
			out.write(sb.toString().getBytes(ENC));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sb.setLength(0);
	}

	String RN = "\r\n";

	private boolean foundBoundaryInFiles(String boundary) throws IOException {
		byte[] b = boundary.getBytes(ENC);
		for (Object o : files.keySet()) {
			Object[] row = (Object[]) files.get(o);
			// String fn = (String) row[0];
			byte[] b2 = (byte[]) row[1];
			if (indexOf(b2, b) >= 0) {
				return true;
			}
		}
		return false;
	}

	private boolean foundBoundaryInParams(String boundary) {
		for (Object o : params.keySet()) {
			String s2 = (String) params.get(o);
			if (s2.indexOf(boundary) >= 0) {
				return true;
			}
		}
		return false;
	}

	final static int M4 = 36 * 36 * 36 * 36;

	private static String genBoundary() {
		return Integer.toString((int) (System.currentTimeMillis() % M4), 36) + "_"
				+ Integer.toString(Etc.rand.nextInt(M4), 36);
	}

	private boolean hasPostDataFile() {
		return !files.isEmpty();
	}

	private boolean hasPostData() {
		return (!params.isEmpty() || !files.isEmpty());
	}

	public void addFile(String key, String filename, byte[] value) {
		files.put(key, new Object[] { filename, value });
	}

	public void addParam(String key, String value) {
		params.put(key, value);
	}

	Map params = new HashMap();
	Map files = new HashMap();

	/**
	 * https://github.com/google/guava/blob/master/guava/src/com/google/common/primitives/Bytes.java#L113
	 */
	public static int indexOf(byte[] array, byte[] target) {
		if (target.length == 0) {
			return 0;
		}
		outer: for (int i = 0; i < array.length - target.length + 1; i++) {
			for (int j = 0; j < target.length; j++) {
				if (array[i + j] != target[j]) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}
}
