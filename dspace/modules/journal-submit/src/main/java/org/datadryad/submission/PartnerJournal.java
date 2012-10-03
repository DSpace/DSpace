package org.datadryad.submission;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartnerJournal {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartnerJournal.class);
	
	private String myName;

	private File myMetadataDir;

	private String myParsingScheme;

	public PartnerJournal(String aName) {
		if (aName == null) {
			throw new NullPointerException(
					"Journal code not allowed to be null");
		}

		myName = aName;
	}

	public void setFullName(String aName) {
		myName = aName;
	}

	public void setMetadataDir(String aMetadataDir) {
		myMetadataDir = new File(aMetadataDir);

		if (!myMetadataDir.exists() && !myMetadataDir.mkdirs()) {
			throw new SubmissionRuntimeException(
					"Unable to create metadata directory "
							+ myMetadataDir.getAbsolutePath());
		}
	}

	public void setParsingScheme(String aParsingScheme) {
		myParsingScheme = aParsingScheme;
	}

	public EmailParser getParser() {
		String className = PartnerJournal.class.getPackage().getName()
				+ ".EmailParserFor" + StringUtils.capitalize(myParsingScheme);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting parser: " + className);
		}
		
		try {
			return (EmailParser) Class.forName(className).newInstance();
		}
		catch (ClassNotFoundException details) {
			throw new SubmissionRuntimeException(details);
		}
		catch (IllegalAccessException details) {
			throw new SubmissionRuntimeException(details);
		}
		catch (InstantiationException details) {
			throw new SubmissionRuntimeException(details);
		}
	}

	public String getName() {
		return myName;
	}

	public String getMetadataDirName() {
		return myMetadataDir.getAbsolutePath();
	}

	public File getMetadataDir() {
		return myMetadataDir;
	}

	public String getParsingScheme() {
		return myParsingScheme;
	}

	public boolean isComplete() {
		return myParsingScheme != null && myMetadataDir != null
				&& myName != null;
	}

	public String toString() {
		StringBuilder string = new StringBuilder("<PartnerJournal");
		string.append(" fullname=\"" + myName + "\"");
		string.append(" metadataDir=\"" + myMetadataDir.getAbsolutePath()
				+ "\"");
		string.append(" parsingScheme=\"" + myParsingScheme + "\"");
		return string.append(">").toString();
	}
}
