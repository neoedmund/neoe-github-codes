/* neoe */
package neoe.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neoe.util.Template3naz.Compiled.Part.Type;

/**
 * An template engine that will not be slow. ${xxxx}
 */
public class Template3naz {

	private static Compiled initCache(String name, ByteBuffer bb) throws IOException {

		String s = StandardCharsets.UTF_8.decode(bb).toString();
		Compiled ret = Compiled.compile(s);
		ret.lastModified = -1;
		cache.put(name, ret);
		return ret;

	}

	private static Compiled findInCache(final String name) {
		// add auto update cache if file changed when necessary
		final Compiled ret = (Compiled) cache.get(name);
		return ret;
	}

	static Map<String, Compiled> cache = new HashMap<String, Compiled>();

	public static class Compiled {

		static class Part {

			enum Type {
				TEXT, VALUE;
			}

			Type type;
			String v;
		}

		public static Compiled compile(String s) {
			Compiled ret = new Compiled();
			int p1 = 0;
			int p2 = s.indexOf("${", p1);
			while (true) {
				if (p2 < 0) {
					ret.addText(s.substring(p1));
					p1 = s.length();
					break;
				} else {
					String name = readName(s, p2 + 2);
					ret.addText(s.substring(p1, p2));
					ret.addValue(name);
					p1 = p2 + 3 + name.length();
					p2 = s.indexOf("${", p1);
				}
			}
			return ret;
		}

		private static String readName(String s, int p) {
			int p2 = s.indexOf('}', p);
			if (p2 < 0) {
				throw new RuntimeException("template2 error");
			}
			return s.substring(p, p2);
		}

		public long lastModified;

		public Compiled() {
			list = new ArrayList<Part>();
		}

		List<Part> list;

		public String resolve(Map<Object, Object> values) {
			StringBuilder sb = new StringBuilder();
			for (Part p : list) {
				if (p.type == Type.TEXT) {
					sb.append(p.v);
				} else if (p.type == Type.VALUE) {
					if (values != null) {
						Object v = values.get(p.v);
						if (v != null) {
							sb.append(v.toString());
						}
					}
				}
			}
			return sb.toString();
		}

		private void addText(String text) {
			if (text == null || text.isEmpty()) {
				return;
			}
			Part part = new Part();
			part.type = Type.TEXT;
			part.v = text;
			list.add(part);
		}

		private void addValue(String name) {
			if (name == null || name.isEmpty()) {
				return;
			}
			Part part = new Part();
			part.type = Type.VALUE;
			part.v = name;
			list.add(part);
		}
	}

	public static String render(String name, ByteBuffer bb, Map values) {
		Compiled temp = findInCache(name);
		if (temp == null) {
			try {
				temp = initCache(name, bb);
			} catch (IOException ex) {
				Log.log("Template3.render():" + ex);
			}
		}
		if (temp == null) {
			return null;
		}
		return temp.resolve(values);
	}

}
