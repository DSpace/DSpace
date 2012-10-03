package org.dspace.content.packager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.content.packager.utils.DirFileFilter;
import org.dspace.core.ConfigurationManager;

public class BagItBuilder {

	private static final Logger LOGGER = Logger.getLogger(BagItBuilder.class);

	private static final String JAVA_OPTS = "-Djava.awt.headless=true -Xmx64M -XX:+UseConcMarkSweepGC -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager";

	private static final String BAGIT_ACTION = "create";
	private static final String BAG_INFO = "--baginfotxt";
	private static final String WRITER = "--writer";
	private static final String[] TAR_GZIP = new String[] { "tar.gz", "tar_gz" };

	public static final File buildIt(File aWorkingDir)
			throws BagItDisseminatorException {
		String destDir = ConfigurationManager.getProperty("bagit.download.dir");
		String bagitExec = ConfigurationManager.getProperty("bagit.executable");
		File dataDir = aWorkingDir.listFiles(new DirFileFilter())[0];
		File bagItDir = new File(destDir, aWorkingDir.getName());

		if (!bagItDir.mkdir()) {
			throw new BagItDisseminatorException("Configuration error: "
					+ bagItDir.getAbsolutePath()
					+ " doesn't exist and can't be created");
		}

		File dest = new File(bagItDir, dataDir.getName() + "." + TAR_GZIP[0]);
		File bagInfoFile = new File(aWorkingDir, "bag-info.txt");

		// This is hard-coded for Linux/Solaris (TODO: move to maven profile)
		String[] command = new String[] {
				"/bin/bash",
				"-c",
				bagitExec + " " + BAGIT_ACTION + " " + BAG_INFO + " "
						+ bagInfoFile.getAbsolutePath() + " " + WRITER + " "
						+ TAR_GZIP[1] + " " + dest.getAbsolutePath() + " "
						+ dataDir.getAbsolutePath() + "/*" };

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("RUNNING: " + Arrays.toString(command));
		}

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			Map<String, String> env = processBuilder.environment();
			String javaOptsValue = env.remove("JAVA_OPTS");

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Removing script JAVA_OPTS=" + javaOptsValue);
			}

			// Setting a new java_opts with a lower memory amount
			env.put("JAVA_OPTS", JAVA_OPTS);

			Process process = processBuilder.start();
			byte[] bytes = new byte[0];
			int result = process.waitFor();
			int available = 0;

			switch (result) {
			case 0:
				LOGGER.info("Successfully created bagit file");
				break;
			case 1:
				InputStream iStream = process.getErrorStream();
				BufferedInputStream bStream = new BufferedInputStream(iStream);
				available = bStream.available();
				bytes = new byte[available];

				bStream.read(bytes);
			default:
				throw new BagItDisseminatorException(
						"BagItBuilder couldn't build bagit file ("
								+ result
								+ ")"
								+ (available > 0 ? ": " + new String(bytes)
										: ""));
			}
		}
		catch (InterruptedException details) {
			throw new BagItDisseminatorException(details);
		}
		catch (IOException details) {
			throw new BagItDisseminatorException(details);
		}

		return dest;
	}
}
