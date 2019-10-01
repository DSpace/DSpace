package ar.edu.unlp.sedici.dspace.curation;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class PDFVersionChecker extends AbstractCurationTask {

	private final float MIN_VERSION_ALLOWED = (float) 1.4;

	private final String PDF_MIME_YPE = "application/pdf";

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		StringBuilder reporter = new StringBuilder();
		int status = Curator.CURATE_SKIP;

		if (dso instanceof Item) {
			status = Curator.CURATE_SUCCESS;
			Item item = (Item) dso;
			try {
				for (Bundle bundle : item.getBundles()) {
					if ("ORIGINAL".equals(bundle.getName())) {
						for (Bitstream bitstream : bundle.getBitstreams()) {
							if (isPDF(bitstream)) {
								try {
									checkPDFVersion(bitstream, item.getHandle(), reporter);
								} catch (Exception e) {
									// Something went wrong
									reportError(e, reporter, bitstream, item.getHandle());
								}
							}
						}
					}
				}
			} catch (SQLException sqle) {
				reporter.append(sqle.getMessage());
				sqle.printStackTrace();
				status = Curator.CURATE_ERROR;
			}
			String report = reporter.toString();
			if (!report.isEmpty()) {
				report(report);
			}
		}
		setResult(reporter.toString());
		return status;
	}

	private void checkPDFVersion(Bitstream bitstream, String itemHandle, StringBuilder reporter)
			throws IOException, SQLException, AuthorizeException {
		PDDocument pdfDoc = null;
		try {
			pdfDoc = PDDocument.load(bitstream.retrieve());
			float pdfVersion = pdfDoc.getVersion();
			if (MIN_VERSION_ALLOWED > pdfVersion) {
				reportOldPDFVersion(pdfVersion, bitstream, itemHandle, reporter);
			}
		} finally {
			if (pdfDoc != null) {
				pdfDoc.close();
			}
		}
	}

	private boolean isPDF(Bitstream bitstream) {
		return bitstream.getFormat().getMIMEType().equalsIgnoreCase(PDF_MIME_YPE);
	}

	private void reportOldPDFVersion(float pdfVersion, Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		reporter.append("- ERROR: PDF version not allowed: ");
		reporter.append(pdfVersion);
		reporter.append(". Item ");
		reporter.append(itemHandle);
		reporter.append(". Bitstream '");
		reporter.append(bitstream.getName());
		reporter.append("' with id ");
		reporter.append(bitstream.getID());
		reporter.append("\n");
	}

	private void reportError(Exception e, StringBuilder reporter, Bitstream bitstream, String itemHandle) {
		reporter.append("- ERROR: There was a problem while processing bitstream '");
		reporter.append(bitstream.getName());
		reporter.append("' with id ");
		reporter.append(bitstream.getID());
		reporter.append(" of item ");
		reporter.append(itemHandle);
		reporter.append(":\n");
		reporter.append(e.getMessage());
		e.printStackTrace();
	}

}
