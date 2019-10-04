package ar.edu.unlp.sedici.dspace.curation;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class PDFVersionChecker extends AbstractCurationTask {

	private final float MIN_VERSION_ALLOWED = (float) 1.4;

	private final String PDF_MIME_YPE = "application/pdf";

	private int status;

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		StringBuilder reporter = new StringBuilder();
		status = Curator.CURATE_SKIP;

		if (dso instanceof Item) {
			status = Curator.CURATE_SUCCESS;
			Item item = (Item) dso;
			try {
				for (Bundle bundle : item.getBundles(Constants.DEFAULT_BUNDLE_NAME)) {
					for (Bitstream bitstream : bundle.getBitstreams()) {
						if (isPDF(bitstream)) {
							checkValidPDF(bitstream, item.getHandle(), reporter);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			}
			String report = reporter.toString();
			if (!report.isEmpty()) {
				report(report);
			}
		}
		setResult(reporter.toString());
		return status;
	}

	private void checkValidPDF(Bitstream bitstream, String itemHandle, StringBuilder reporter)
			throws IOException, SQLException, AuthorizeException {
		PDDocument pdfDoc = null;
		InputStream file = bitstream.retrieve();
		try {
			pdfDoc = PDDocument.load(file);
		} catch (IOException e) {
			reportPDFException(e, bitstream, itemHandle, reporter);
		} finally {
			if (pdfDoc != null) {
				pdfDoc.close();
			}
		}
		float pdfVersion = pdfDoc.getVersion();
		if (MIN_VERSION_ALLOWED > pdfVersion) {
			reportOldPDFVersion(pdfVersion, bitstream, itemHandle, reporter);
		}
		if (pdfDoc.isEncrypted()) {
			reportEncryptedPDF(bitstream, itemHandle, reporter);
		}

	}

	private boolean isPDF(Bitstream bitstream) {
		return bitstream.getFormat().getMIMEType().equalsIgnoreCase(PDF_MIME_YPE);
	}

	private void reportOldPDFVersion(float pdfVersion, Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		String errorMsge = "PDF version not allowed " + pdfVersion;
		reportError(errorMsge, bitstream, itemHandle, reporter);
	}

	private void reportEncryptedPDF(Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		String errorMsge = "PDF encrypted";
		reportError(errorMsge, bitstream, itemHandle, reporter);
	}

	private void reportPDFException(Exception e, Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		String errorMsge = "There was a problem while processing bitstream";
		reportError(errorMsge, bitstream, itemHandle, reporter);
		reporter.append(e.getMessage());
		e.printStackTrace();
		status = Curator.CURATE_ERROR;
	}

	private void reportError(String errorMessage, Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		reporter.append("- ERROR: ");
		reporter.append(errorMessage);
		reporter.append(", Item:\"");
		reporter.append(itemHandle);
		reporter.append("\", Bitstream id:\"");
		reporter.append(bitstream.getID());
		reporter.append("\", Bitstream:\"");
		reporter.append(bitstream.getDescription());
		reporter.append("\"\n");
	}
}
