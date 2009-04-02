package neoe.eve;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.VolatileImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Eve2DMap {

	public static class FocusAnine implements Widge {
		private MapPanel panel;

		private float gx;

		private float gy;

		int r;

		public FocusAnine(final MapPanel panel, float gx, float gy) {
			this.panel = panel;
			this.gx = gx;
			this.gy = gy;
			this.r = 51;
			panel.widges.add(this);
			new Thread() {
				public void run() {
					while (r > 2) {
						panel.repaint();
						r -= 1;
						Eve2DMap.sleep(30);
					}
					dead = true;
					panel.repaint();
				}
			}.start();
		}

		boolean dead = false;

		public void draw(Graphics2D g2) {
			g2.setStroke(new BasicStroke(5));
			g2.setColor(Color.RED);
			int width = panel.getWidth();
			int height = panel.getHeight();
			float dx = panel.maxx - panel.minx;
			float dy = panel.maxy - panel.miny;
			int x = panel.transx(gx, width, dx);
			int y = panel.transy(gy, height, dy);
			Ellipse2D.Double circle = new Ellipse2D.Double(x - r, y - r, 2 * r,
					2 * r);
			g2.draw(circle);
		}

		public Rectangle getBounds() {
			return null;
		}

		public boolean isDead() {
			return dead;
		}

	}

	public static class Button implements Widge, MouseListener {
		private Color bgColor;

		private Callback callback;

		private Color color;

		public boolean dead;

		private int h;

		private String label;

		private MapPanel panel;

		private boolean raised;

		Rectangle rect = new Rectangle();

		private int w;

		private int x;

		private int y;

		public Button(MapPanel panel, String label, int w, int h,
				Callback callback) {
			this.label = label;
			this.w = w;
			this.h = h;
			this.callback = callback;
			this.panel = panel;
			x = 0;
			y = 0;
			bgColor = Color.GRAY;
			color = Color.WHITE;
			raised = true;
			panel.widges.add(this);
			panel.mouseListener = this;
			panel.mouseListener_mouseEntered = false;
		}

		public void dispose() {
			dead = true;
			if (panel.mouseListener == this) {
				panel.mouseListener = null;
			}
		}

		public void draw(Graphics2D g2) {
			g2.setColor(bgColor);
			g2.fill3DRect(x, y, w, h, raised);
			g2.setColor(color);
			int ws = (int) g2.getFont().getStringBounds(label,
					g2.getFontRenderContext()).getWidth();
			g2.drawString(label, x + (w - ws) / 2, y + 20);
		}

		public Rectangle getBounds() {
			rect.x = x;
			rect.y = y;
			rect.width = w;
			rect.height = h;
			return rect;
		}

		public boolean isDead() {
			return dead;
		}

		public void mouseClicked(MouseEvent e) {

		}

		public void mouseEntered(MouseEvent e) {
			bgColor = Color.BLUE;
			panel.repaint();
		}

		public void mouseExited(MouseEvent e) {
			bgColor = Color.GRAY;
			raised = true;
			panel.repaint();
		}

		public void mousePressed(MouseEvent e) {
			raised = false;
			e.consume();
			panel.repaint();
		}

		public void mouseReleased(MouseEvent e) {
			if (!raised) {
				callback.run(new Object[] { this, e });
			}
			e.consume();
			panel.repaint();
		}
	}

	public interface Callback {
		void run(Object o);
	}

	public static class HelpWindow implements Widge {

		private boolean dead;

		private MapPanel panel;

		private long startt;

		public HelpWindow(final MapPanel panel) {
			this.panel = panel;
			panel.widges.add(this);
			startt = System.currentTimeMillis();
			new Thread() {
				public void run() {
					// close thread
					Eve2DMap.sleep(5000);
					dead = true;
					panel.repaint();
					panel.isHelpShown = false;
				}
			}.start();
			new Thread() {
				public void run() {
					// repaint thread

					while (!dead) {
						Eve2DMap.sleep(1000);
						panel.repaint();
					}

				}
			}.start();
		}

		private static String[] helpMsg = { "Mouse Drag: Move",
				"Mouse Scroll: Zoom", "Shift + Mouse Scroll: rotate X-axis",
				"F2:Show/Hide Name", "F3/Enter: Find", "F4: Find Last" };

		public void draw(Graphics2D g2) {
			int w = 200;
			int h = 130;
			g2.setColor(new Color(0.8f, 0.1f, 0.1f, 0.5f));
			int x, y;
			g2.fillRect(x = (panel.getWidth() - w) / 2,
					y = (panel.getHeight() - h) / 2, w, h);
			g2.setColor(Color.WHITE);

			y += 20;
			for (String line : helpMsg) {
				g2.drawString(line, x, y);
				y += 20;
			}
			y += 20;
			int sec = (int) Math.max(0,
					5 - (System.currentTimeMillis() - startt) / 1000);
			g2.setColor(Color.GREEN);
			g2.drawString("Closing in " + sec + " sec", x, y);

		}

		public Rectangle getBounds() {
			return null;
		}

		public boolean isDead() {
			return dead;
		}

	}

	public static class MapPanel extends JComponent implements MouseListener,
			MouseMotionListener, MouseWheelListener, KeyListener {
		private static final int MAXL = 10000;

		private static final int MAXP = 10000;

		private int _dragx;

		private int _dragy;

		private int _x;

		private int _y;

		private float bx;

		private float by;

		private float bz;

		private String centerSolarName;

		private int dragx;

		private int dragy;

		// private int[] drawx;
		//
		// private int[] drawy;
		private int[] cachex;

		private int[] cachey;

		KeyListener focusedObj;

		private boolean isHelpShown = false;

		private boolean isShowName = true;

		private int linecnt;

		private int[] linefrom;

		private int[] lineto;

		private float maxx = Float.NEGATIVE_INFINITY;

		private float maxy = Float.NEGATIVE_INFINITY;

		private float maxz = Float.NEGATIVE_INFINITY;

		private float minx = Float.POSITIVE_INFINITY;

		private float miny = Float.POSITIVE_INFINITY;

		private float minz = Float.POSITIVE_INFINITY;

		private MouseListener mouseListener;

		private boolean mouseListener_mouseEntered;

		private int pcnt;

		private int[] pid;

		private String[] pointn;

		private float[] pointx;

		private float[] pointy;

		private float[] pointz;

		private List<Widge> widges = new ArrayList<Widge>();

		private float zoomx = 1;

		private float zx;

		private float zy;

		private float zz;

		// private float zoomy = 1;
		// y value after rotate in x-axis, (1,-1)
		private float r = 1.0f;

		public MapPanel() {
			pointx = new float[MAXP];
			pointy = new float[MAXP];
			pointz = new float[MAXP];
			// drawx = new int[MAXP];
			// drawy = new int[MAXP];
			cachex = new int[MAXP];
			cachey = new int[MAXP];
			pointn = new String[MAXP];
			pid = new int[MAXP];
			linefrom = new int[MAXL];
			lineto = new int[MAXL];
			addMouseWheelListener(this);
			addMouseMotionListener(this);
			addMouseListener(this);
			addKeyListener(this);
			setFocusable(true);
		}

		private void addLine() {
			System.out.println("adding lines");
			System.out.println("build index");
			Map<Integer, Integer> index = new HashMap<Integer, Integer>();
			for (int i = 0; i < pid.length; i++) {
				index.put(pid[i], i);
			}
			System.out.println("build index ok");
			pid = null;
			System.gc();
			System.out.println("point id released");
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						Eve2DMap.class.getClassLoader().getResourceAsStream(
								"mapSolarSystemJumps.txt"), "utf8"));
				in.readLine();
				String l;
				while ((l = in.readLine()) != null) {
					String[] w = l.split("\\,");
					if (w[2].compareTo(w[3]) > 0) {
						int i1 = index.get(Integer.parseInt(w[2]));
						int i2 = index.get(Integer.parseInt(w[3]));
						this.addLine(i1, i2);
					}
				}
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("adding lines ok");

		}

		public void addLine(int from, int to) {
			linefrom[linecnt] = from;
			lineto[linecnt] = to;
			linecnt++;
		}

		public void addPoint(Object[] p) {
			pointn[pcnt] = (String) p[0];
			pid[pcnt] = ((Number) p[4]).intValue();
			float x = ((Number) p[1]).floatValue();
			float y = ((Number) p[2]).floatValue();
			float z = ((Number) p[3]).floatValue();
			pointx[pcnt] = x;
			pointy[pcnt] = y;
			pointz[pcnt] = z;
			maxx = Math.max(maxx, x);
			maxy = Math.max(maxy, y);
			maxz = Math.max(maxz, z);
			minx = Math.min(minx, x);
			miny = Math.min(miny, y);
			minz = Math.min(minz, z);
			pcnt++;

		}

		private void addPoints() {
			System.out.println("adding points");
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						Eve2DMap.class.getClassLoader().getResourceAsStream(
								"mapSolarSystems.txt"), "utf8"));
				in.readLine();
				String l;
				Object[] buf = new Object[5];
				while ((l = in.readLine()) != null) {
					String[] w = l.split("\\,");
					if (w[0].compareTo("11000001") >= 0) {
						continue;// skip J999999 systems
					}
					buf[0] = w[3];
					buf[1] = Float.valueOf(w[4]);
					buf[2] = Float.valueOf(w[5]);
					buf[3] = Float.valueOf(w[6]);
					buf[4] = Integer.valueOf(w[2]);
					this.addPoint(buf);

				}
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("adding points ok");
			System.out.println("sorting points");
			sortPoints();
			System.out.println("sorting points ok");
		}

		// private int[] adjustxy(int x, int y, int maxl, int i) {
		// // make text not collapse, but due to performce reason, not usable
		// boolean changed = true;
		// int absx = 0;
		// int absy = 0;
		// int rx = maxl * 8;
		// while (changed) {
		// changed = false;
		// for (int j = 0; j < i; j++) {
		// int ax = drawx[j] - x;
		// int ay = drawy[j] - y;
		//
		// if (Math.abs(ax) < rx && Math.abs(ay) < 10) {
		// changed = true;
		// if (Math.abs(ay) < 10) {
		// if (absy == 0) {
		// if (ay < 0) {
		// y += 10;
		// absy = 10;
		// } else {
		// y -= 10;
		// absy = -10;
		// }
		// } else {
		// y += absy;
		// }
		// } else {
		// if (Math.abs(ax) < rx) {
		// if (absx == 0) {
		// if (ax < 0) {
		// x += rx;
		// absx = rx;
		// } else {
		// x -= rx;
		// absx = -rx;
		// }
		// } else {
		// x += absx;
		// }
		// }
		// }
		// // System.out.println(String.format("x=%s,y=%s",
		// // new Object[] { x, y }));
		// }
		// }
		// }
		// return new int[] { x, y };
		// }

		private int binsearch(Object name) {
			int low = 0;
			int high = pcnt - 1;

			while (low <= high) {
				int mid = (low + high) >> 1;
				Comparable midVal = (Comparable) pointn[mid];
				int cmp = midVal.compareTo(name);
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid; // key found
			}
			return -(low + 1); // key not found.
		}

		private void consumeDrag() {
			bx -= (dragx + _dragx) * zx / zoomx;
			by -= (dragy + _dragy) * zy / zoomy();
			dragx = 0;
			dragy = 0;
			_dragx = 0;
			_dragy = 0;
		}

		private float zoomy() {
			return r * zoomx;
		}

		private void drawLines(Graphics2D g2, int[] ps) {
			if (ps == null)
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();
			g2.setColor(Color.GRAY);
			float dx = maxx - minx;
			float dy = maxy - miny;
			int dc = 0;
			
			for (int i = 0; i < linecnt; i++) {
				int f = linefrom[i];
				int t = lineto[i];
				int p1 = -1, p2 = -1;
				if (ps != null) {
					if ((p1 = Arrays.binarySearch(ps, f)) < 0
							&& (p2 = Arrays.binarySearch(ps, t)) < 0) {
						continue;// quick filter
					}
				}

				// int x2 = transx(pointx[t], width, dx);
				// int y2 = transy(pointy[t], height, dy);
				int x1, y1, x2, y2;
				if (p1 < 0) {
					x1 = transx(pointx[f], width, dx);
					y1 = transy(pointy[f], height, dy) + transz(pointz[f]);
				} else {
					x1 = cachex[f];
					y1 = cachey[f];
				}
				if (p2 < 0) {
					x2 = transx(pointx[t], width, dx);
					y2 = transy(pointy[t], height, dy) + transz(pointz[t]);
				} else {
					x2 = cachex[t];
					y2 = cachey[t];
				}
				if ((x1 >= 0 && x1 <= width && y1 >= 0 && y1 <= height)
						|| (x2 >= 0 && x2 <= width && y2 >= 0 && y2 <= height)) {
					dc++;
					g2.drawLine(x1, y1, x2, y2);
				}
				// System.out.println(String.format("line (%s,%s)-(%s,%s)",
				// new Object[] { x1, y1, x2, y2 }));
			}
			// System.out.println(String.format("draw lines %s/%s%s",
			// new Object[] { dc, linecnt,
			// (ps == null) ? "" : " using quick filter" }));
		}

		private int[] drawPoints(Graphics2D g2) {
			int width = getWidth();
			int height = getHeight();
			int pr = 3;
			float dx = maxx - minx;
			float dy = maxy - miny;
			g2.setColor(Color.WHITE);
			// System.out.println(String.format("%s,%s,%s,%s", new Object[] {
			// maxx, minx, maxy, miny }));
			int maxl = 1;
			int dc = 0;
			int[] drawnPoints = new int[pointn.length / 5];
			Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, 2 * pr,
					2 * pr);
			for (int i = 0; i < pcnt; i++) {

				int x = transx(pointx[i], width, dx);
				int y = transy(pointy[i], height, dy) + transz(pointz[i]);
				if (x < 0 || x > width || y < 0 || y > height)
					continue;
				// g.setColor(Color.WHITE);
				int px = x;
				int py = y;
				// int[] ret = adjustxy(x, y, maxl, i);
				// x = ret[0];
				// y = ret[1];
				// drawx[i] = x;
				// drawy[i] = y;
				
				circle.x=px-pr;
				circle.y=py-pr;
				g2.fill(circle);
				
				if (isShowName) {
					g2.drawString(pointn[i], x - 10, y - 10);
				}
				maxl = Math.max(maxl, pointn[i].length());
				if (dc < drawnPoints.length) {
					drawnPoints[dc] = i;
					cachex[i] = px;
					cachey[i] = py;
				}
				dc++;
				// System.out.println(x + "," + y);
			}
			// System.out.println(String.format("draw points %s/%s", new
			// Object[] {
			// dc, pcnt }));
			if (dc <= drawnPoints.length) {
				Arrays.sort(drawnPoints);
				return drawnPoints;
			} else {
				return null;
			}

		}

		private int transz(float z) {
			return Math.round(zoomx * (z - bz) * ((float) Math.sqrt(1 - r * r))
					/ zz) / 2;
		}

		public void fit(int width, int height) {
			bx = minx;
			by = miny;
			bz = minz;
			zx = (maxx - minx) / width;
			zy = (maxy - miny) / height;
			zz = (maxz - minz) / height;
		}

		@Override
		public int getHeight() {
			Container p = getParent();
			if (p == null) {
				return super.getHeight();
			} else {
				return p.getHeight();
			}
		}

		@Override
		public int getWidth() {
			Container p = getParent();
			if (p == null) {
				return super.getWidth();
			} else {
				return p.getWidth();
			}
		}

		private void gotoSolar(String name) {
			if (name == null)
				return;
			int i = binsearch(name);
			System.out.println(String.format("GOTO %s index %s", new Object[] {
					name, i }));
			if (i < 0) {
				return;
			}
			float gx = pointx[i];
			float gy = pointy[i];
			bz= pointz[i];
			consumeDrag();
			bx = gx - getWidth() * zx / zoomx / 2;
			by = gy - getHeight() * zy / zoomy() / 2;
			new FocusAnine(this, gx, gy);
			repaint();
		}

		public void keyPressed(KeyEvent e) {
			if (focusedObj != null) {
				focusedObj.keyPressed(e);
				if (e.isConsumed()) {
					return;
				}
			}
			// System.out.println(e.getKeyCode());
			if (e.getKeyCode() == KeyEvent.VK_F1) {
				showHelp();
			} else if (e.getKeyCode() == KeyEvent.VK_F2) {
				isShowName = !isShowName;
				repaint();
			} else if (e.getKeyCode() == KeyEvent.VK_F3) {
				inputSolarName();
			} else if (e.getKeyCode() == KeyEvent.VK_F4) {
				gotoSolar(centerSolarName);
			}
		}

		private void inputSolarName() {
			new UserInput(this, "Solar Name to Center:", centerSolarName,
					new Callback() {
						public void run(Object o) {
							centerSolarName = (String) o;
							gotoSolar(centerSolarName);
						}
					});
			repaint();
		}

		public void keyReleased(KeyEvent e) {
			if (focusedObj != null) {
				focusedObj.keyReleased(e);
				if (e.isConsumed()) {
					return;
				}
			}
		}

		public void keyTyped(KeyEvent e) {
			if (focusedObj != null) {
				focusedObj.keyTyped(e);
				if (e.isConsumed()) {
					return;
				}
			}
			if (e.getKeyChar() == 10) {// VK_Enter
				inputSolarName();
			}
		}

		public void mouseClicked(MouseEvent e) {
			if (mouseListener != null) {
				mouseListener.mouseClicked(e);
				if (e.isConsumed()) {
					return;
				}
			}
		}

		public void mouseDragged(MouseEvent e) {
			_dragx = e.getX() - _x;
			_dragy = e.getY() - _y;
			repaint();
		}

		public void mouseEntered(MouseEvent e) {
			if (mouseListener != null) {
				mouseListener.mouseEntered(e);
				if (e.isConsumed()) {
					return;
				}
			}
		}

		public void mouseExited(MouseEvent e) {
			if (mouseListener != null) {
				mouseListener.mouseExited(e);
				if (e.isConsumed()) {
					return;
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			if (mouseListener != null) {
				Widge w = (Widge) mouseListener;
				if (w.getBounds().contains(e.getPoint())) {
					if (!mouseListener_mouseEntered) {
						mouseListener.mouseEntered(e);
						mouseListener_mouseEntered = true;
					}
				} else {
					if (mouseListener_mouseEntered) {
						mouseListener.mouseExited(e);
						mouseListener_mouseEntered = false;
					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {

			if (mouseListener != null) {
				Widge w = (Widge) mouseListener;
				if (w.getBounds().contains(e.getPoint())) {
					mouseListener.mousePressed(e);
					if (e.isConsumed()) {
						return;
					}
				}
			}

			_x = e.getX();
			_y = e.getY();

		}

		public void mouseReleased(MouseEvent e) {
			if (mouseListener != null) {
				Widge w = (Widge) mouseListener;
				if (w.getBounds().contains(e.getPoint())) {
					mouseListener.mouseReleased(e);
					if (e.isConsumed()) {
						return;
					}
				}
			}
			dragx += _dragx;
			dragy += _dragy;
			_dragx = 0;
			_dragy = 0;
			repaint();
		}

		public void mouseWheelMoved(final MouseWheelEvent e) {
			final float a;
			final float oldr = r;
			if (e.isShiftDown()) {
				a = 1;
				if (e.getWheelRotation() > 0) {
					r = Math.min(1, r + 0.1f);
					if (r == 0)
						r = 0.1f;
				} else {
					r = Math.max(-1, r - 0.1f);
					if (r == 0)
						r = -0.1f;
				}
			} else {
				if (e.getWheelRotation() > 0) {
					a = 1.1f;
				} else {
					a = 0.9f;
				}
			}

			// zoom
			new Thread(new Runnable() {
				public void run() {
					// normalize drag
					consumeDrag();

					if (e.isShiftDown()) {

						float newr = r;

						if (oldr - newr < 0.01) {
							r = oldr;
							while (r - newr < 0.01) {
								r += 0.01f;
								float zoomy = zoomy();
								by += e.getY() * zy
										* (1 / zoomy - 1 / (a * zoomy));
								repaint();
								System.out.println("r=" + r);
								sleep(50);
							}
						} else {
							r = oldr;
							while (r - newr > 0.01) {
								r -= 0.01f;
								float zoomy = zoomy();
								by += e.getY() * zy
										* (1 / zoomy - 1 / (a * zoomy));
								repaint();
								System.out.println("r=" + r);
								sleep(20);
							}
						}

						r = newr;
					} else {
						float zoomy = zoomy();
						bx += e.getX() * zx * (1 / zoomx - 1 / (a * zoomx));
						by += e.getY() * zy * (1 / zoomy - 1 / (a * zoomy));
						zoomx *= a;
					}
					repaint();
				}
			}).start();

		}

		private String layer1key = "";

		private VolatileImage layer1img;

		@Override
		protected void paintComponent(Graphics g) {
			long startt = System.currentTimeMillis();
			Graphics2D g2 = (Graphics2D) g;
			// super.paintComponent(g);
			int width = getWidth();
			int height = getHeight();

			String newlayer1key = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
					new Object[] { dragx + _dragx, zoomx, bx, zx,
							dragy + _dragy, r, by, zy, isShowName });
			if (newlayer1key.equals(layer1key)) {
				g2.drawImage(layer1img, 0, 0, null);
				// System.out.println("hit image cache, key=" + newlayer1key);
			} else {
				if (layer1img == null || layer1img.getWidth() != width
						|| layer1img.getHeight() != height) {
					System.out.println(String.format("create image %sx%s",
							new Object[] { width, height }));
					layer1img = getGraphicsConfiguration()
							.createCompatibleVolatileImage(width, height);// avg
					// 0 ms
					// layer1img = new BufferedImage(width, height,
					// BufferedImage.TYPE_INT_ARGB); // avg 16 ms
				}
				Graphics2D gi = layer1img.createGraphics();
				gi.setColor(Color.BLACK);
				gi.fillRect(0, 0, width, height);
				gi.setColor(Color.WHITE);
				gi.drawString(String.format("%s*%s", new Object[] { width,
						height }), 10, 30);
				// draw points
				int[] ps = drawPoints(gi);
				// draw lines
				drawLines(gi, ps);
				g2.drawImage(layer1img, 0, 0, null);
				layer1key = newlayer1key;
			}
			// draw Widge
			List<Widge> dead = new ArrayList<Widge>();
			int cnt = 0;
			for (Widge w : widges) {
				if (w.isDead()) {
					dead.add(w);
				} else {
					cnt++;
					w.draw(g2);
				}
			}
			// System.out.println("draw widges " + cnt);

			if (dead.size() > 0) {
				widges.removeAll(dead);
				System.out.println("remove widges " + dead.size() + " remains "
						+ widges.size());
			}
			System.out.println("paint in ms "
					+ (System.currentTimeMillis() - startt));
		}

		private void showHelp() {
			if (isHelpShown) {
				return;
			}
			isHelpShown = true;
			new HelpWindow(this);
			repaint();
		}

		private void sortPoints() {
			// sort by name
			for (int i = 0; i < pcnt - 1; i++) {
				for (int j = i + 1; j < pcnt; j++) {
					if (pointn[i].compareTo(pointn[j]) > 0) {
						String o;
						float f;
						int b;
						o = pointn[i];
						pointn[i] = pointn[j];
						pointn[j] = o;
						f = pointx[i];
						pointx[i] = pointx[j];
						pointx[j] = f;
						f = pointy[i];
						pointy[i] = pointy[j];
						pointy[j] = f;
						b = pid[i];
						pid[i] = pid[j];
						pid[j] = b;
					}
				}
			}

		}

		private int transx(float x, int width, float dx) {
			return dragx + _dragx + Math.round(zoomx * (x - bx) / zx);

		}

		private int transy(float y, int height, float dy) {
			return dragy + _dragy + Math.round(zoomy() * (y - by) / zy);

		}
	}

	public static class UserInput implements Widge, KeyListener, Callback {
		private Button button;

		private Callback callback;

		public static boolean dead = true;

		private String msg;

		private MapPanel panel;

		private StringBuffer text;

		public UserInput(final MapPanel panel, String msg, String value,
				final Callback callback) {
			if (!dead) {
				System.out.println("singleton");
				return;// singleton
			}
			dead = false;
			text = new StringBuffer();
			if (value != null) {
				text.append(value);
			}
			this.msg = msg;
			this.callback = callback;
			this.panel = panel;
			panel.widges.add(this);
			panel.focusedObj = this;
			button = new Button(panel, "OK", 50, 30, this);
		}

		public void dispose() {
			dead = true;
			if (panel.focusedObj == this) {
				panel.focusedObj = null;
			}
		}

		public void draw(Graphics2D g2) {
			int w = 200;
			int h = 130;
			g2.setColor(new Color(0.1f, 0.1f, 0.8f, 0.5f));
			int x, y;
			g2.fillRect(x = (panel.getWidth() - w) / 2,
					y = (panel.getHeight() - h) / 2, w, h);
			g2.setColor(Color.WHITE);
			String[] msg = { this.msg };
			y += 20;
			for (String line : msg) {
				g2.drawString(line, x, y);
				y += 20;
			}
			g2.drawRect(x, y, w - 40, 20);
			g2.drawString(text.toString(), x + 3, y + 19);
			y += 20;
			button.x = x;
			button.y = y + 10;

		}

		public Rectangle getBounds() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isDead() {
			return dead;
		}

		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub

		}

		public void keyTyped(KeyEvent e) {
			// System.out.println(e.getKeyChar());
			// System.out.println((int)e.getKeyChar());
			char ch = e.getKeyChar();
			if (ch == 8) {// backspace
				text.setLength(Math.max(0, text.length() - 1));
			} else if (ch == 10) {// enter
				run(null);
			} else {
				text.append(ch);
			}
			e.consume();
			panel.repaint();
		}

		public void run(Object o) {
			callback.run(text.toString());
			button.dispose();
			dispose();
		}
	}

	public interface Widge {

		void draw(Graphics2D g2);

		Rectangle getBounds();

		boolean isDead();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MapPanel map = new MapPanel();
		map.addPoints();
		map.addLine();
		map.fit(800, 600);

		f.add(map);
		f.setSize(800, 600);
		f.setVisible(true);

	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
