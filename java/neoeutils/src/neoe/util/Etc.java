package neoe.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Random;

public class Etc {
	public static String passToHash(String s) throws Exception {
		if (s == null || s.isEmpty()) {
			return "";
		}
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] arr = md.digest(s.getBytes("UTF8"));
		return toHex(arr);
	}

	public static String toHex(byte[] hash) {
		StringBuilder sb = new StringBuilder(hash.length * 2);
		for (int i = 0; i < hash.length; i++) {
			int b = hash[i];
			if (b < 0)
				b += 0x100;
			String v = Integer.toHexString(b);
			if (v.length() < 2) {
				sb.append("0");
			}
			sb.append(v);
		}
		return sb.toString();
	}

	public static Object invoke(Class cls, Object obj, String method, Class[] argTypes, Object[] args)
			throws Exception {
		return cls.getMethod(method, argTypes).invoke(obj, args);
	}

	public static boolean isEmpty(CharSequence s) {
		return s == null || s.length() <= 0;
	}

	public static Random rand = new Random();

	public static void replaceAll(StringBuilder builder, String from, String to) {
		int index = builder.indexOf(from);
		while (index != -1) {
			builder.replace(index, index + from.length(), to);
			index += to.length();
			index = builder.indexOf(from, index);
		}
	}
//	Function<Object,Void> c = String::format;
//	static Function<int[], Boolean> b = a -> {
//		int r = 0, m = 1;
//		for (int i : a)
//			r += (m ^= 2) * i;
//		return r % 10 < 1;
//	};

	public static void panic(String m) {
		throw new RuntimeException(m);
	}

	public static String toString(Throwable t) {
		ByteArrayOutputStream baos;
		PrintStream tos = new PrintStream(baos = new ByteArrayOutputStream());
		t.printStackTrace(tos);
		tos.close();
		try {
			String s = baos.toString("UTF8");
			return s;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return baos.toString();
	}

	public static void asleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void confirm(boolean b, String s) {
		if (!b) {
			throw new RuntimeException(s);
		}
	}

}