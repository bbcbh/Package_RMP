package test.nonsim;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util_Population_Remote_MetaPopulation_Recursive_Copy_CSV {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println(
					"Usgage: java Util_Population_Remote_MetaPopulation_Recursive_Copy_CSV SRC_DIR TARGET_DIR");
		} else {
			File srcDir = new File(args[0]);
			File tarDir = new File(args[1]);
			recusiveCopyCSV(srcDir, tarDir);

			
		}

	}

	/**
	 * @param srcSub
	 * @param tarSub
	 * @throws IOException
	 */
	public static void recusiveCopyCSV(File srcSub, File tarSub) throws IOException {
		if (!tarSub.exists()) {
			tarSub.mkdirs();
		}		
		
		int counter = 0;

		for (File f : srcSub.listFiles()) {
			File tarFile = new File(tarSub, f.getName());
			if (f.isDirectory()) {
				recusiveCopyCSV(f, tarFile);
			} else {
				if (COPY_FILE_FILTER.accept(f)) {
					//System.out.println(String.format("Copying from %s to %s", f.toPath(), tarFile.toPath()));
					Files.copy(f.toPath(), tarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					counter++;
				}
			}

		}
		System.out.println(String.format("%d file(s) copied from %s\n   to %s", counter,
				srcSub.getAbsolutePath(),  tarSub.getAbsolutePath()));
		
		
	}

	private static final FileFilter COPY_FILE_FILTER = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			boolean res = false;

			if (pathname.isFile()) {
				String fName = pathname.getName();
				Pattern hasSimPattern = Pattern.compile("_S\\d+");
				if (fName.toLowerCase().endsWith(".csv")) {
					Matcher m = hasSimPattern.matcher(fName);
					return !m.find();
				}

			}

			return res;
		}

	};

}
