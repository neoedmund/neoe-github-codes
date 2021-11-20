package neoe.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FileIterator implements Iterable<File> {

	// private String root;
	List<File> buf;
	private boolean sortByName;

	public FileIterator(String dir) {
		buf = new ArrayList<File>();
		File f = new File(dir);
		buf.add(f);
	}

	public FileIterator(String dir, boolean sortByName) {
		this(dir);
		this.sortByName = sortByName;
	}

	

	@Override
	public Iterator<File> iterator() {
		return new Iterator<File>() {

			@Override
			public boolean hasNext() {
				return buf.size() > 0;
			}

			@Override
			public File next() {
				File f = buf.remove(0);
				if (f.isDirectory()) {
					File[] sub = f.listFiles();
					if (sub!=null){
						if (sortByName) {
							sortFiles(sub);
						}
						buf.addAll(Arrays.asList(sub));
					}
				}
				return f;
			}

			@Override
			public void remove() {
			}
		};
	}

	public static void sortFiles(File[] sub) {
		Arrays.sort(sub, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
	}

}
