package neoe.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NeoeScriptRunner {

	public static class DataList extends ArrayList {
		public DataList() {
			super();
		}

		public DataList(List a) {
			super(a);
		}
	}

	public static class Env extends HashMap {

	}

	Env env = new Env();
	Object client;
	Class clientClass;
	private Method[] clientMethods;

	public void runOn(List cmds, Object  client, Class class1) throws Exception {
		this.client = client;
		clientClass = class1;
		clientMethods = clientClass.getDeclaredMethods();
		for (Object o : cmds) {
			env.put("_", eval((List) o));
		}
	}

	private Object eval(Object stat0) throws Exception {
		if (stat0 instanceof DataList)
			return stat0;
		if (!(stat0 instanceof List))
			return stat0;
		List stat = (List) stat0;
		Object o = eval(stat.get(0));
		if (!(o instanceof String)) {
			Etc.panic(String.format("stat_0 should be string, but get %s(class:%s)", o, o.getClass()));
			return null;
		}
		String cmd = (String) o;

		List data = new DataList(stat.subList(1, stat.size()));
		if ("list".equals(cmd)) {
			return data;
		} else {
			return invoke(cmd, data);
		}

	}

	private Object invoke(String cmd, List data) throws Exception {
		try {
			Method method = findMethod(cmd);
			if (method == null) {
				Etc.panic("cannot find implement method of " + cmd);
				return null;
			}
			Parameter[] params = method.getParameters();
			int pp = 0, q = 0;
			List values = new ArrayList();
			if (params.length > 0 && params[0].getType().equals(Env.class)) {
				values.add(env);
				pp = 1;
			}

			int sizeDef = params.length - pp;
			int sizeReal = data.size();
			{

				if (sizeReal > sizeDef) {
					Etc.panic(String.format("too many params for %s, expect %s, got %s", cmd, sizeDef, sizeReal));
					return null;
				}
			}
			for (int i = pp; i < params.length; i++) {
				if (q >= sizeReal) {
					values.add(defValue(params[i].getType()));
				} else {
					values.add(mapping(params[i].getType(), eval(data.get(q++))));
				}
			}
			System.out.println("[d]invoke " + cmd + " with " + values + ", type:" + dumpTypes(values));
			return method.invoke(client, values.toArray());
		} catch (Exception e) {
			throw new RuntimeException("err when call " + cmd, e);
		}
	}

	private String dumpTypes(List values) {
		StringBuilder sb = new StringBuilder();
		for (Object o : values) {
			sb.append(o == null ? "NULL" : o.getClass().getSimpleName()).append(",");
		}
		return sb.toString();
	}

	private Object mapping(Class clz, Object v) {
		if (isNumberType(clz)) {
		//	System.out.println("[d2]"+clz);
			Number n = toNumber(v);
			if (clz == int.class) {
				return n.intValue();
			} else if (clz == float.class) {
				return n.floatValue();
			} else {
				return n;
			}
		}
		// more check?
		return v;
	}

	private boolean isNumberType(Class clz) {
		return Number.class.isAssignableFrom(clz) || clz == int.class || clz == float.class;
	}

	private Number toNumber(Object v) {
		if (v instanceof Number) {
			return (Number) v;
		}
		return new BigDecimal(v.toString());
	}

	private static Object defValue(Class clz) {
		if (Number.class.isAssignableFrom(clz)) {
			return 0;
		}
		return null;
	}

	private Method findMethod(String cmd) {
		for (Method m : clientMethods) {
			if (m.getName().equals(cmd))
				return m;
		}
		return null;
	}

}
