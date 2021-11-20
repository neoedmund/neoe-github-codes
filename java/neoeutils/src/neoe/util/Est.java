package neoe.util;

public class Est {
	private long t1, t3;
	private long start;

	public Est(long start) {
		t1 = System.currentTimeMillis();
		this.start = start;
	}

	public String getInfo(long i, long cnt) {
		long t2 = System.currentTimeMillis();
		long t = (t2 - t1) / 1000L;
		double h = ((double) t) / 3600L;
		long p = i - start;
		if (p == 0)
			p = 1;
		double h3 = h / p * (cnt - start);
		double h2 = h / p * (cnt - i);
		String unit = "H";
		if (h2 < 1 && h2 >= 0) {
			h *= 60;
			h2 *= 60;
			h3 *= 60;
			unit = "M";
		}
		String s = String.format("\u23F2%.1f%s\u231B%.1f%s=%.1f%s %.1f%%", h, unit, h2, unit, h3, unit,
				((double) i / cnt * 100));
		return s;
	}

	public void update(long i, long cnt, long printMs, String append) {
		update(i, cnt, printMs, append, null);
	}

	public void update(long i, long cnt, long printMs, String append, Runnable f) {
		long t2 = System.currentTimeMillis();
		if (t2 - t3 >= printMs) {
			t3 = t2;
			if (append != null) {
				System.out.println(getInfo(i, cnt) + append);
			} else {
				System.out.println(getInfo(i, cnt));
			}
			if (f != null) {
				f.run();
			}
		}
	}

}
