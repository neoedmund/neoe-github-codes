package neoe.eve;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class Eve2DMap {

	public static class MapPanel extends JComponent implements MouseListener,
			MouseMotionListener, MouseWheelListener {
		private static final int MAXP = 10000;

		private static final int MAXL = 10000;

		private float[] pointx;

		private float[] pointy;

		private String[] pointn;

		private int pcnt;

		private float maxx = Float.NEGATIVE_INFINITY;

		private float maxy = Float.NEGATIVE_INFINITY;

		private float minx = Float.POSITIVE_INFINITY;

		private float miny = Float.POSITIVE_INFINITY;

		private float zoomx = 1;

		private float zoomy = 1;

		private float bx;

		private float zx;

		private float by;

		private float zy;

		private int _y;

		private int _x;

		private int dragx;

		private int dragy;

		private int _dragx;

		private int _dragy;

		private int[] drawy;

		private int[] drawx;

		private int[] linefrom;

		private int[] lineto;

		private int linecnt;

		public MapPanel() {
			pointx = new float[MAXP];
			pointy = new float[MAXP];
			drawx = new int[MAXP];
			drawy = new int[MAXP];
			pointn = new String[MAXP];
			linefrom = new int[MAXL];
			lineto = new int[MAXL];
			addMouseWheelListener(this);
			addMouseMotionListener(this);
			addMouseListener(this);
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

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			// super.paintComponent(g);
			int width = getWidth();
			int height = getHeight();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.WHITE);
			g.drawString(
					String.format("%s*%s", new Object[] { width, height }), 10,
					30);
			// draw points
			drawPoints(g2);
			// draw lines
			drawLines(g2);

		}

		private void drawLines(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();
			g2.setColor(Color.WHITE);
			float dx = maxx - minx;
			float dy = maxy - miny;
			int dc = 0;
			for (int i = 0; i < linecnt; i++) {
				int f = linefrom[i];
				int t = lineto[i];
				int x1 = transx(f, width, dx);
				int y1 = transy(f, height, dy);
				int x2 = transx(t, width, dx);
				int y2 = transy(t, height, dy);
				if ((x1 >= 0 && x1 <= width && y1 >= 0 && y1 <= height)
						|| (x2 >= 0 && x2 <= width && y2 >= 0 && y2 <= height)) {
					dc++;
					g2.drawLine(x1, y1, x2, y2);
				}
				// System.out.println(String.format("line (%s,%s)-(%s,%s)",
				// new Object[] { x1, y1, x2, y2 }));
			}
			System.out.println(String.format("draw lines %s/%s", new Object[] {
					dc, linecnt }));
		}

		private void drawPoints(Graphics2D g2) {
			int width = getWidth();
			int height = getHeight();
			int pw = 5, ph = 5;
			float dx = maxx - minx;
			float dy = maxy - miny;
			g2.setColor(Color.WHITE);
			// System.out.println(String.format("%s,%s,%s,%s", new Object[] {
			// maxx, minx, maxy, miny }));
			int maxl = 1;
			int dc = 0;
			for (int i = 0; i < pcnt; i++) {
				int x = transx(i, width, dx);
				int y = transy(i, height, dy);
				if (x < 0 || x > width || y < 0 || y > height)
					continue;
				// g.setColor(Color.WHITE);
				int px = x;
				int py = y;
				// int[] ret = adjustxy(x, y, maxl, i);
				// x = ret[0];
				// y = ret[1];
				drawx[i] = x;
				drawy[i] = y;
				g2.fillRect(px, py, pw, ph);
				g2.drawString(pointn[i], x - 10, y - 10);
				maxl = Math.max(maxl, pointn[i].length());
				dc++;
				// System.out.println(x + "," + y);
			}
			System.out.println(String.format("draw points %s/%s", new Object[] {
					dc, pcnt }));

		}

		private int[] adjustxy(int x, int y, int maxl, int i) {
			boolean changed = true;
			int absx = 0;
			int absy = 0;
			int rx = maxl * 8;
			while (changed) {
				changed = false;
				for (int j = 0; j < i; j++) {
					int ax = drawx[j] - x;
					int ay = drawy[j] - y;

					if (Math.abs(ax) < rx && Math.abs(ay) < 10) {
						changed = true;
						if (Math.abs(ay) < 10) {
							if (absy == 0) {
								if (ay < 0) {
									y += 10;
									absy = 10;
								} else {
									y -= 10;
									absy = -10;
								}
							} else {
								y += absy;
							}
						} else {
							if (Math.abs(ax) < rx) {
								if (absx == 0) {
									if (ax < 0) {
										x += rx;
										absx = rx;
									} else {
										x -= rx;
										absx = -rx;
									}
								} else {
									x += absx;
								}
							}
						}
						// System.out.println(String.format("x=%s,y=%s",
						// new Object[] { x, y }));
					}
				}
			}
			return new int[] { x, y };
		}

		public void addPoint(Object[] p) {
			pointn[pcnt] = (String) p[0];
			float x = ((Number) p[1]).floatValue();
			float y = ((Number) p[2]).floatValue();
			pointx[pcnt] = x;
			pointy[pcnt] = y;
			maxx = Math.max(maxx, x);
			maxy = Math.max(maxy, y);
			minx = Math.min(minx, x);
			miny = Math.min(miny, y);
			pcnt++;
		}

		public void addLine(int from, int to) {
			linefrom[linecnt] = from;
			lineto[linecnt] = to;
			linecnt++;
		}

		public void mouseClicked(MouseEvent e) {

		}

		public void mouseEntered(MouseEvent e) {

		}

		public void mouseExited(MouseEvent e) {

		}

		public void mousePressed(MouseEvent e) {
			_x = e.getX();
			_y = e.getY();

		}

		public void mouseReleased(MouseEvent e) {
			dragx += _dragx;
			dragy += _dragy;
			_dragx = 0;
			_dragy = 0;
			repaint();
		}

		public void mouseDragged(MouseEvent e) {
			_dragx = e.getX() - _x;
			_dragy = e.getY() - _y;
			repaint();
		}

		public void mouseMoved(MouseEvent e) {

		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			float a;
			if (e.getWheelRotation() > 0) {
				a = 1.1f;
			} else {
				a = 0.9f;
			}
			// normalize drag
			bx -= (dragx + _dragx) * zx / zoomx;
			by -= (dragy + _dragy) * zy / zoomy;
			dragx = 0;
			dragy = 0;
			_dragx = 0;
			_dragy = 0;
			// zoom
			if (e.isShiftDown()) {
				by += e.getY() * zy * (1 / zoomy - 1 / (a * zoomy));
				zoomy *= a;
			} else {
				bx += e.getX() * zx * (1 / zoomx - 1 / (a * zoomx));
				by += e.getY() * zy * (1 / zoomy - 1 / (a * zoomy));
				zoomx *= a;
				zoomy *= a;
			}
			repaint();
		}

		private int transy(int f, int height, float dy) {
			return dragy + _dragy + Math.round(zoomy * (pointy[f] - by) / zy);

		}

		private int transx(int f, int width, float dx) {
			return dragx + _dragx + Math.round(zoomx * (pointx[f] - bx) / zx);

		}

		public void fit(int width, int height) {
			bx = minx;
			by = miny;
			zx = (maxx - minx) / width;
			zy = (maxy - miny) / height;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MapPanel map = new MapPanel();

		addLine(map, addPoints(map));
		map.fit(800, 600);

		f.add(map);
		f.setSize(800, 600);
		f.setVisible(true);

	}

	private static List<String> addPoints(MapPanel map) {
		System.out.println("adding points");
		// for (Object point : NoFormat.getPoints()) {
		// map.addPoint((Object[]) point);
		// }
		List<String> idlist = new ArrayList<String>(MapPanel.MAXP);
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					"C:/tmp/eveexp2/mapSolarSystems.txt"));
			in.readLine();
			String l;
			Object[] buf = new Object[3];
			while ((l = in.readLine()) != null) {
				String[] w = l.split("\\,");
				if (w[0].compareTo("11000001") >= 0) {
					continue;// skip J999999 systems
				}
				buf[0] = w[3];
				buf[1] = Float.valueOf(w[4]);
				buf[2] = Float.valueOf(w[5]);
				map.addPoint(buf);
				idlist.add(w[2]);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("adding points ok");
		return idlist;
	}

	private static void addLine(MapPanel map, List<String> idlist) {
		System.out.println("adding lines");
		System.out.println("build index");
		Map<String, Integer> index = new HashMap<String, Integer>();
		for (int i = 0; i < idlist.size(); i++) {
			index.put(idlist.get(i), i);
		}
		System.out.println("build index ok");
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					"C:/tmp/eveexp2/mapSolarSystemJumps.txt"));
			in.readLine();
			String l;
			while ((l = in.readLine()) != null) {
				String[] w = l.split("\\,");
				if (w[2].compareTo(w[3]) > 0) {
					int i1 = index.get(w[2]);
					int i2 = index.get(w[3]);
					map.addLine(i1, i2);
				}
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("adding lines ok");
	}
	/*
	 * zoom: minx, maxx
	 * 
	 */
}
