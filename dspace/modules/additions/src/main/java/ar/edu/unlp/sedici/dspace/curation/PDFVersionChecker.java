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
								parseBitstream(bitstream, item.getHandle(), reporter);
							}
						}
					}
				}
			} catch (SQLException sqle) {
				// Something went wrong
				reportError(sqle, reporter, item.getHandle());
				status = Curator.CURATE_ERROR;
			} catch (AuthorizeException e) {
				// Something went wrong
				reportError(e, reporter, item.getHandle());
				status = Curator.CURATE_ERROR;
			}
			report(reporter.toString());
		}
		setResult(reporter.toString());
		return status;
	}

	private void parseBitstream(Bitstream bitstream, String itemHandle, StringBuilder reporter)
			throws IOException, SQLException, AuthorizeException {
		PDDocument pdfDoc = null;
		try {
			pdfDoc = PDDocument.load(bitstream.retrieve());
			float pdfVersion = pdfDoc.getVersion();
			if (MIN_VERSION_ALLOWED > pdfVersion) {
				reportVersionError(pdfVersion, bitstream, itemHandle, reporter);
			}
		} catch (IOException ioe) {
			reportError(ioe, reporter, itemHandle);
		} finally {
			if (pdfDoc != null) {
				pdfDoc.close();
			}
		}
	}

	private boolean isPDF(Bitstream bitstream) {
		return true;
	}

	private void reportVersionError(float pdfVersion, Bitstream bitstream, String itemHandle, StringBuilder reporter) {
		reporter.append("- ERROR: Item ");
		reporter.append(itemHandle);
		reporter.append(" Bitstream id: ");
		reporter.append(bitstream.getID());
		reporter.append(". PDF version not allowed: ");
		reporter.append(pdfVersion);
		reporter.append("\n");
	}

	private void reportError(Exception e, StringBuilder reporter, String itemHandle) {
		reporter.append("There was a problem while processing bitstream of item ");
		reporter.append(itemHandle);
		reporter.append(":\n");
		reporter.append(e.getMessage());
		e.printStackTrace();
	}

}
