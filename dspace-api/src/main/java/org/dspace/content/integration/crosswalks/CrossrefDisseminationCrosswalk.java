/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

public class CrossrefDisseminationCrosswalk extends ReferCrosswalk {
	/** log4j category */
	private static Logger log = Logger
			.getLogger(CrossrefDisseminationCrosswalk.class);

	protected final static String CONFIG_VERSION = "crosswalk.crossref.version";

	protected final static String CONFIG_NAMESPACE = "crosswalk.crossref.namespace";
	
	protected final static String CONFIG_SCHEMA = "crosswalk.crossref.schemaLocation";
	
	@Override
	public void disseminate(Context context, DSpaceObject dso, OutputStream out)
			throws CrosswalkException, IOException, SQLException,
			AuthorizeException {

		// see header template file and footer template file, arrays elements
		// will go to fill on template
		// TODO manage configurable array...
		String[] HEADER_PARAMETERS = {
				ConfigurationManager
				.getProperty(CONFIG_NAMESPACE),
				ConfigurationManager
				.getProperty(CONFIG_SCHEMA),
				ConfigurationManager
				.getProperty(CONFIG_VERSION),
				""+dso.getID(),
				DateFormatUtils.format(new Date(),"yyyyMMddHHMMSS"),
				ConfigurationManager
						.getProperty("crosswalk.crossref.depositor"),
				ConfigurationManager.getProperty("mail.admin"),
				ConfigurationManager
						.getProperty("crosswalk.crossref.registrant") };
		String[] FOOTER_PARAMETERS = {};

		// write header
		String myName = getPluginInstanceName();
		if (myName == null)
			throw new CrosswalkInternalException(
					"Cannot determine plugin name, "
							+ "You must use PluginManager to instantiate ReferCrosswalk so the instance knows its name.");

		String templatePropNameHeader = CONFIG_PREFIX + ".template." + myName
				+ ".header";

		String templateFileNameHeader = ConfigurationManager
				.getProperty(templatePropNameHeader);

		if (templateFileNameHeader == null)
			throw new CrosswalkInternalException(
					"Configuration error: "
							+ "No template header file configured for Refer crosswalk named \""
							+ myName + "\"");

		String parent = ConfigurationManager.getProperty("dspace.dir")
				+ File.separator + "config" + File.separator;
		File templateFile = new File(parent, templateFileNameHeader);
		FileInputStream sourceHeader = new FileInputStream(templateFile);
		InputStream headerFinalStream = null;
		try {
			byte[] buffer = new byte[(int) templateFile.length()];
			BufferedInputStream f = null;
			try {
				f = new BufferedInputStream(sourceHeader);
				f.read(buffer);
			} finally {
				if (f != null)
					try {
						f.close();
					} catch (IOException ignored) {
						throw new CrosswalkInternalException(
								"Error to read header file "
										+ templateFileNameHeader);
					}
			}

			String header = MessageFormat.format(new String(buffer),
					HEADER_PARAMETERS);
			headerFinalStream = new ByteArrayInputStream(
					header.getBytes("UTF-8"));
			Utils.bufferedCopy(headerFinalStream, out);
		} finally {
			sourceHeader.close();
			if (headerFinalStream != null) {
				headerFinalStream.close();
			}
		}

		
		super.disseminate(context, dso, out);
		

		// write footer
		String templatePropNameFooter = CONFIG_PREFIX + ".template." + myName
				+ ".footer";

		String templateFileNameFooter = ConfigurationManager
				.getProperty(templatePropNameFooter);

		if (templateFileNameFooter == null)
			throw new CrosswalkInternalException(
					"Configuration error: "
							+ "No template footer file configured for Refer crosswalk named \""
							+ myName + "\"");

		String parentfooter = ConfigurationManager.getProperty("dspace.dir")
				+ File.separator + "config" + File.separator;
		File templateFileFooter = new File(parentfooter, templateFileNameFooter);
		FileInputStream sourceFooter = new FileInputStream(templateFileFooter);
		InputStream footerFinalStream = null;
		try {
			byte[] buffer = new byte[(int) templateFileFooter.length()];
			BufferedInputStream f = null;
			try {
				f = new BufferedInputStream(sourceFooter);
				f.read(buffer);
			} finally {
				if (f != null)
					try {
						f.close();
					} catch (IOException ignored) {
						throw new CrosswalkInternalException(
								"Error to read header file "
										+ templateFileNameFooter);
					}
			}

			String footer = MessageFormat.format(new String(buffer),
					FOOTER_PARAMETERS);
			footerFinalStream = new ByteArrayInputStream(
					footer.getBytes("UTF-8"));
			Utils.bufferedCopy(footerFinalStream, out);
		} finally {
			sourceFooter.close();
			if (footerFinalStream != null) {
				footerFinalStream.close();
			}
		}
	}
	
}