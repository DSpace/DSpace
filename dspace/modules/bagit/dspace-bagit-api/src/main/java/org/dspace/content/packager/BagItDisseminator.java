package org.dspace.content.packager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.SQLException;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.content.packager.utils.PackageManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
   BagItDisseminator manages the creation of a BagIt package from a Dryad data package.

   @author Kevin Clarke

**/
@SuppressWarnings("deprecation")
public class BagItDisseminator implements PackageDisseminator {

	private static Logger LOGGER = Logger.getLogger(BagItDisseminator.class);

	private static final String DATA_FILE_NAME = "dryadfile-";

	private static final String PUB_FILE_NAME = "dryadpub.xml";

	private static final String PKG_FILE_NAME = "dryadpkg.xml";

	// TODO: make hard-coded things above configurable, more generalizable

	private static final String WORK_DIR = "bagit.work.dir";
	private static final String DOWNLOAD_DIR = "bagit.download.dir";

	public void disseminate(Context aContext, DSpaceObject aDSO,
			PackageParameters aPkgParams, OutputStream aOutStream)
			throws PackageException, CrosswalkException, AuthorizeException,
			SQLException, IOException {
		try {
			disseminateBagIt(aContext, aDSO, aPkgParams);
			aOutStream.close();
		}
		catch (RuntimeException details) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Catching RuntimeException: "
						+ details.getMessage(), details);
			}

			emailException(aContext, details, aDSO);
		}
		catch (BagItDisseminatorException details) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Catching BagItDisseminatorException: "
						+ details.getMessage(), details);
			}

			emailException(aContext, details, aDSO);
		}
	}

    /**
       Emails a notice of an exception to the repository admin.
    **/
	private void emailException(Context aContext, Throwable aThrowable,
			DSpaceObject aDSO) throws IOException, PackageException {
		EPerson person = aContext.getCurrentUser();
		String admin = ConfigurationManager.getProperty("mail.admin");
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer, true);
		Locale locale = I18nUtil.getDefaultLocale();
		String emailFile = I18nUtil.getEmailFilename(locale, "bagit_error");
		Email email = ConfigurationManager.getEmail(emailFile);

		// Write our stack trace to a string for output
		aThrowable.printStackTrace(printWriter);
		email.addRecipient(admin);

		// Add details to display in the email message
		email.addArgument(writer.toString());
		email.addArgument(person.getEmail());
		email.addArgument(aDSO.getHandle());

		if (aThrowable instanceof BagItDisseminatorException) {
			email.addArgument(((BagItDisseminatorException) aThrowable)
					.getWorkplace());
		}

		try {
			email.send();
		}
		catch (MessagingException emailExceptionDetails) {
			throw new IOException(emailExceptionDetails);
		}

		throw new PackageException(aThrowable);
	}

	private void disseminateBagIt(Context aContext, DSpaceObject aDSO,
			PackageParameters aPkgParams) throws BagItDisseminatorException {
		String downloadDirName = ConfigurationManager.getProperty(DOWNLOAD_DIR);
		String workDirName = ConfigurationManager.getProperty(WORK_DIR);
		File downloadDir = new File(downloadDirName);
		File workDir = new File(workDirName);

		// Work dir is where we put things together to be bagged
		if (!workDir.exists() && !workDir.mkdirs()) {
			throw new BagItDisseminatorException("Configuration error: "
					+ workDirName + " doesn't exist and can't be created");
		}

		// Download dir is where the bagit goes when it's ready for access
		if (!downloadDir.exists() && !downloadDir.mkdirs()) {
			throw new BagItDisseminatorException("Configuration error: "
					+ downloadDirName + " doesn't exist and can't be created");
		}

		if (aDSO == null) {
			throw new BagItDisseminatorException("The DSO passed in is null");
		}

		if (aDSO.getType() == Constants.ITEM) {
			Item item = (Item) aDSO; // this should be a package...
			String handle = aDSO.getHandle();

			aContext.setIgnoreAuthorization(true);

			// ...but let's specifically get package to make sure
			Item pkg = getPackage(aContext, handle);

			// Find our Dryad id shorthand to use as directory name
			int startIndex = handle.indexOf("dryad.");
			int slashIndex = handle.indexOf('/', startIndex);
			int endIndex = slashIndex == -1 ? handle.length() : slashIndex;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Start/end index: " + startIndex + "/" + endIndex);
			}

			// Change the dot in name to underscore for creating a dir name
			String id = handle.substring(startIndex, endIndex)
					.replace('.', '_');

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("ID: " + id);
			}

			String timestamp = Long.toString(new Date().getTime());
			File timestampedDir = new File(workDir, id + "-" + timestamp);
			File dir = new File(timestampedDir, id);
			EPerson person = aContext.getCurrentUser();
			String xWalkName = aPkgParams.getProperty("xwalk");
			String files = aPkgParams.getProperty("files");

			if (!dir.mkdirs()) {
				throw new BagItDisseminatorException("Configuration error: "
						+ dir.getAbsolutePath()
						+ " doesn't exist and can't be created");
			}

			try {
				String email = person.getEmail();
				BagInfoTxtGenerator.writeBagInfoTxt(timestampedDir, email);
			}
			catch (IOException details) {
				throw new BagItDisseminatorException(details);
			}

			// The bulk of the work is done in this try/catch
			try {
				if (item.getHandle().equals(pkg.getHandle())) {
					exportPackage(dir, pkg, person, xWalkName, files, aContext);
				}
				else {
					throw new BagItDisseminatorException(
							"The supplied item is not a package");
				}

				File bagIt = BagItBuilder.buildIt(timestampedDir);
				String remoteRepo = aPkgParams.getProperty("repo");
				BagItRepoResolver.getRepo(remoteRepo).send(bagIt, person);
			}
			catch (BagItDisseminatorException details) {
				throw details.setWorkplace(timestampedDir);
			}
		}
		else {
			throw new BagItDisseminatorException(
					new PackageValidationException("Can only disseminate items"));
		}
	}

    @Override
    public void disseminate(Context context, DSpaceObject object, PackageParameters params, File pkgFile) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<File> disseminateAll(Context context, DSpaceObject dso, PackageParameters params, File pkgFile) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getMIMEType(PackageParameters aPkgParams) {
		return "text/plain"; // if we wanted to return something to STDOUT
	}

    @Override
    public String getParameterHelp() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /* Just wrapping the exception */
	private Item getPackage(Context aContext, String aHandle)
			throws BagItDisseminatorException {
		try {
			return PackageManager.resolveToDataPackage(aContext, aHandle);
		}
		catch (SQLException details) {
			throw new BagItDisseminatorException(details);
		}
	}

    /**
       Exports a single data file to bagit representation on disk.
    **/
	private void exportFile(File aWorkDir, Item aDataFile, String aXWalkName,
			int aPosition) throws BagItDisseminatorException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Exporting a data file in bagit format using the "
					+ aXWalkName + " metadata crosswalk");
		}

		try {
			Element dataFile = crosswalk(aXWalkName, aDataFile);

			// a little debugging to see what we're getting...
			if (LOGGER.isDebugEnabled()) {
				String logDir = ConfigurationManager.getProperty("log.dir");
				String fileName = "bagit-f-" + aPosition + ".xml";
				File bagItFile = new File(logDir, fileName);
				FileWriter fileOut = new FileWriter(bagItFile);
				Format format = Format.getPrettyFormat();
				XMLOutputter outputter = new XMLOutputter(format);
				Element xmlOut = (Element) dataFile.getChildren().get(0);

				outputter.output(xmlOut, fileOut);
				fileOut.close();
			}

			File fileDir = new File(aWorkDir, "datafile-" + aPosition);

			if (!fileDir.mkdirs()) {
				throw new BagItDisseminatorException("Configuration error: "
						+ fileDir + " doesn't exist and can't be created");
			}

			File file = new File(fileDir, DATA_FILE_NAME + aPosition + ".xml");
			Namespace ns = dataFile.getNamespace();
			writeXML(dataFile.getChild("DryadDataFile", ns), file);

			for (Bundle bundle : aDataFile.getBundles()) {
				for (Bitstream bitstream : bundle.getBitstreams()) {
					String bitstreamName = bitstream.getName();

					File bitsFile = new File(fileDir, bitstreamName);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.info("Writing " + bitstreamName
								+ " to the bagit working directory");
					}

					writeBitstream(bitstream.retrieve(), bitsFile);
				}
			}
		}
		catch (Exception details) {
			throw new BagItDisseminatorException(details);
		}
	}

	private void writeBitstream(InputStream aInputStream, File aBitsFile)
			throws IOException {
		BufferedInputStream inStream = new BufferedInputStream(aInputStream);
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		FileOutputStream fileOutStream = new FileOutputStream(aBitsFile);
		BufferedOutputStream output = new BufferedOutputStream(fileOutStream);
		byte[] buffer = new byte[1024];
		int bytesRead = 0;

		while (true) {
			bytesRead = inStream.read(buffer);
			if (bytesRead == -1) break;
			byteStream.write(buffer, 0, bytesRead);
		};

		output.write(byteStream.toByteArray());
		output.close();
		inStream.close();
	}

	private void writeXML(Element aElement, File aFile) throws IOException {
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		FileWriter writer = new FileWriter(aFile);

		outputter.output(aElement, writer);
		writer.close();
	}

    /**
       Writes a data package and all of its associated files to disk, in bagit format.
    **/
	private void exportPackage(File aWorkDir, Item aDataPkg, EPerson aPerson,
			String aXWalkName, String aFilesParam, Context aContext)
			throws BagItDisseminatorException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Exporting a data package in bagit format using the "
					+ aXWalkName + " metadata crosswalk");
		}

		Element dataPkg = crosswalk(aXWalkName, aDataPkg);
		Namespace ns = dataPkg.getNamespace();
		
		try {
			if (LOGGER.isDebugEnabled()) {
				String logDir = ConfigurationManager.getProperty("log.dir");
				File bagItPackage = new File(logDir, "bagit-p.xml");
				FileWriter pkgOut = new FileWriter(bagItPackage);
				Format format = Format.getPrettyFormat();
				XMLOutputter outputter = new XMLOutputter(format);
				Element xmlOut = dataPkg.getChild("DryadDataPackage", ns);
				
				outputter.output(xmlOut, pkgOut);
				pkgOut.close();
			}

			Element pkg = dataPkg.getChild("DryadDataPackage", ns);
			Element pub = dataPkg.getChild("DryadPublication", ns);
			
			if (pkg != null) writeXML(pkg, new File(aWorkDir, PKG_FILE_NAME));
			if (pub != null) writeXML(pub, new File(aWorkDir, PUB_FILE_NAME));

			if (aFilesParam == null) {
				throw new BagItDisseminatorException(
						"No data files requested; this isn't implemented yet");
			}
			else {
				String[] files = aFilesParam.split(";");

				for (int index = 1; index < files.length + 1; index++) {
					exportFile(aWorkDir, (Item) HandleManager.resolveToObject(
							aContext, files[index - 1]), aXWalkName, index);
				}
			}

		}
		catch (Exception details) {
			throw new BagItDisseminatorException(details.getMessage(), details);
		}
	}

    /**
       Transforms metadata from DSpace-native format to the bagit format (as XML)
    **/
    private Element crosswalk(String aXWalkName, Item aItem)
			throws BagItDisseminatorException {
		DisseminationCrosswalk xWalk = (DisseminationCrosswalk) PluginManager
				.getNamedPlugin(DisseminationCrosswalk.class, aXWalkName);
		String baseURL = ConfigurationManager.getProperty("dspace.url") + "/";
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		if (xWalk == null) {
			throw new BagItDisseminatorException(
					new PackageValidationException("Cannot find " + aXWalkName
							+ " crosswalk plugin"));
		}

		try {
			for (Bundle bundle : aItem.getBundles()) {
				for (Bitstream bitstream : bundle.getBitstreams()) {
					String checksum = bitstream.getChecksum();
					String checksumAlgo = bitstream.getChecksumAlgorithm();

					String format = bitstream.getFormatDescription();
					String size = bitstream.getSize() + " bytes";
					String bitstreamName = bitstream.getName();
					int seqId = bitstream.getSequenceID();

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(checksum + " " + checksumAlgo);
						LOGGER.debug(seqId);
						LOGGER.debug(bitstreamName);
						LOGGER.debug(size);
						LOGGER.debug(format);
					}

					aItem.addMetadata("dc", "format", null, "en", format);
					aItem.addMetadata("dc", "extent", null, null, size);

					StringBuilder builder = new StringBuilder(urlBuilder);

					if (builder.charAt(builder.length() - 1) != '/') {
						builder.append('/');
					}

					// Build the bitstream URL
					builder.append("bitstream/handle/");
					builder.append(aItem.getHandle());
					builder.append('/').append(bitstreamName);
					builder.append("?sequence=");

					String url = builder.append(seqId).toString();
					aItem.addMetadata("dryad", "bitstreamId", null, null, url);
				}
			}
		}
		catch (SQLException details) {
			throw new BagItDisseminatorException(details);
		}

		try {
			if (xWalk.canDisseminate(aItem)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.info("Crosswalking metadata from: " + aItem.getHandle());
				}
				
				return xWalk.disseminateElement(aItem);
			}
			else {
				throw new BagItDisseminatorException("Can't disseminate "
						+ aItem.getHandle());
			}
		}
		catch (Exception details) {
			throw new BagItDisseminatorException(details);
		}
	}
}
