package org.dspace.content.packager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dspace.core.ConfigurationManager;

public class BagInfoTxtGenerator {

	private static final String SOURCE_ORG = "Source-Organization: ";
	private static final String CONTACT_EMAIL = "Contact-Email: ";
	private static final String RECIPIENT_EMAIL = "Dryad-User: ";
	private static final String EOL = "\r\n";

	public static final void writeBagInfoTxt(File aDir, String aEmail)
			throws IOException {
		String adminEmail = ConfigurationManager.getProperty("mail.admin");
		String dspaceName = ConfigurationManager.getProperty("dspace.name");

		FileWriter writer = new FileWriter(new File(aDir, "bag-info.txt"));
		writer.write(SOURCE_ORG);
		writer.write(dspaceName);
		writer.write(EOL);
		writer.write(CONTACT_EMAIL);
		writer.write(adminEmail);
		writer.write(EOL);
		writer.write(RECIPIENT_EMAIL);
		writer.write(aEmail);
		writer.write(EOL);
		writer.close();
	}
}
