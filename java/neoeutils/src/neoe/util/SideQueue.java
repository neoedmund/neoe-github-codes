package neoe.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * a queue in other thread, put put put, fetch, batch
 * 
 * @see Log for usage of this
 * 
 * @author neoe
 *
 */
public class SideQueue {

	public interface Batch {
		Object run(List data) throws Exception;
	}

	LinkedBlockingQueue buf;
	private boolean stop;
	private Batch batch;
	private boolean fastfail;
	public int intervalMs;

	public SideQueue(final int interval, final Batch batch, boolean fastfail) {
		this.intervalMs = interval;
		buf = new LinkedBlockingQueue();
		this.batch = batch;
		this.fastfail = fastfail;
		Thread t = new Thread() {
			public void run() {
				while (true) {
					oneTurn();
					if (stop) {
						break;
					}
					try {
						Thread.sleep(intervalMs <= 0 ? 1 : intervalMs);
					} catch (InterruptedException e) {
					}
				}
				// System.out.println("SideQueue thread stopped.");
			}
		};
		t.setDaemon(true);
		t.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				close();
				oneTurn();
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// System.out.println("SQ closed");
			}
		});
	}

	public synchronized void oneTurn() {
		List data = new ArrayList();
		buf.drainTo(data);
		try {
			batch.run(data);
		} catch (Throwable e) {
			e.printStackTrace();
			if (fastfail) {
				throw new RuntimeException(e);
			}
		}
//		data.clear();
	}

	public void close() {
		stop = true;
	}

	public void add(Object o) {
		buf.add(o);
	}

}
