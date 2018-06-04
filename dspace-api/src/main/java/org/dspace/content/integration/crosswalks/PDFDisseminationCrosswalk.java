/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.dspace.app.util.MetadataExposure;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.SelfNamedPlugin;

public class PDFDisseminationCrosswalk extends SelfNamedPlugin
implements StreamGenericDisseminationCrosswalk, FileNameDisseminator {
	private static Logger log = Logger.getLogger(PDFDisseminationCrosswalk.class);

	public static final String FILE_NAME_EXPORT_PDF = "pdf.export.filename";
	private static final String PLACEHOLDER_SPACING = "EMPTY";
	private static final String style = "pdf";
	private static String defaultFields = "dc.title, dc.title.alternative, dc.contributor.*, dc.subject, dc.date.issued, dc.publisher, dc.identifier.citation, dc.relation.ispartofseries, dc.description.abstract, dc.description, dc.identifier.govdoc, dc.identifier.uri, dc.identifier.isbn, dc.identifier.issn, dc.identifier.ismn, dc.identifier";

	@Override
	public boolean canDisseminate(Context context, DSpaceObject dso) {
		return (dso.getType() == Constants.ITEM);
	}

	@Override
	public void disseminate(Context context, DSpaceObject dso, OutputStream out)
			throws CrosswalkException, IOException, SQLException, AuthorizeException {
		// Create a document and add a page to it
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);

		createPage(context, dso, document, page, new HashMap<String, Integer>());

		// Save the results and ensure that the document is properly closed:
		try

		{
			// Finally Let's save the PDF
			document.save(out);
		} catch (COSVisitorException e) {
			log.error(e.getMessage(), e);
		}

		document.close();
	}

	private void createPage(Context context, DSpaceObject dso, PDDocument document, PDPage page,
			Map<String, Integer> alreadyWrote) throws IOException, SQLException {

		// Create a new font object selecting one of the PDF base fonts
		PDFont fontBold = PDType1Font.HELVETICA_BOLD;
		PDFont font = PDType1Font.HELVETICA;

		// Start a new content stream which will "hold" the to be created content
		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		PDRectangle mediabox = page.findMediaBox();

		float margin = 30;
		float yCordinate = mediabox.getUpperRightY() - margin;
		float startX = mediabox.getLowerLeftX() + margin;
		float endX = mediabox.getUpperRightX() - margin;

		float fontSize = 10;
		float fontHeight = fontSize;
		float leading = 1.5f * fontSize;

		float width = mediabox.getWidth() - 2 * margin;
		float height = mediabox.getHeight() - 2 * margin;

		String configLine = ConfigurationManager.getProperty("webui.itemexport.pdf");
		if (StringUtils.isBlank(configLine)) {
			configLine = defaultFields;
		}

		StringTokenizer st = new StringTokenizer(configLine, ",");

		primary: while (st.hasMoreTokens()) {
			String field = st.nextToken().trim();

			if (!alreadyWrote.containsKey(field)) {
				alreadyWrote.put(field, 0);
			}

			// Get the separate schema + element + qualifier
			String[] eq = field.split("\\.");
			String schema = eq[0];
			String element = eq[1];
			String qualifier = null;
			if (eq.length > 2 && eq[2].equals("*")) {
				qualifier = Item.ANY;
			} else if (eq.length > 2) {
				qualifier = eq[2];
			}

			// check for hidden field, even if it's configured..
			if (MetadataExposure.isHidden(context, schema, element, qualifier)) {
				continue;
			}

			// FIXME: Still need to fix for metadata language?
			Metadatum[] values = dso.getMetadata(schema, element, qualifier, Item.ANY);

			if (values.length > 0) {

				String label = null;
				try {
					label = I18nUtil.getMessage("metadata." + ("default".equals(style) ? "" : style + ".") + field,
							context.getCurrentLocale(), true);
				} catch (MissingResourceException e) {
					// if there is not a specific translation for the style we
					// use the default one
					label = I18nUtil.getMessage("metadata." + field, context);
				}

				List<String> lines = new ArrayList<String>();
				lines.addAll(createAndGetLines(label,startX,fontSize,font,width));
				int labelSize = lines.size();
				for (Metadatum mm : values) {
					lines.addAll(createAndGetLines(mm.value,startX,fontSize,font,width));
				}

				internal: for (int index = alreadyWrote.get(field); index < lines.size(); index++) {

					if (index < labelSize)
					{
						contentStream.beginText();
						contentStream.setFont(fontBold, fontSize);
					}else {
						contentStream.beginText();
						contentStream.setFont(font, fontSize);
					}
					contentStream.moveTextPositionByAmount(startX, yCordinate);
					if (height <= 0) {
						PDPage pdPage = new PDPage();
						document.addPage(pdPage);
						createPage(context, dso, document, pdPage, alreadyWrote);
						break primary;
					}
					contentStream.drawString(lines.get(index));

					yCordinate -= leading;
					height -= leading;

					contentStream.endText();
					alreadyWrote.put(field, index+1);

					if (height > 0 && index==lines.size()-1) {
						yCordinate -= fontHeight;
						height -= fontHeight;

						contentStream.moveTo(startX, yCordinate);
						contentStream.lineTo(endX, yCordinate);
						contentStream.stroke();

						yCordinate -= leading;
						height -= leading;
					}
				}

			}

		}
		// Make sure that the content stream is closed:
		contentStream.close();
	}

	private List<String> createAndGetLines(String text, float startX, float fontSize, PDFont font, float width) throws IOException {
		List<String> lines = new ArrayList<String>();

		int lastSpace = -1;
		while (text.length() > 0) {
			int spaceIndex = text.indexOf(' ', lastSpace + 1);
			if (spaceIndex < 0)
				spaceIndex = text.length();
			String subString = text.substring(0, spaceIndex);
			float sizeWord = startX + (fontSize * font.getStringWidth(subString) / 1000);
			if (sizeWord > width) {
				if (lastSpace < 0)
					lastSpace = spaceIndex;
				subString = text.substring(0, lastSpace);
				lines.add(subString);
				text = text.substring(lastSpace).trim();
				lastSpace = -1;
			} else if (spaceIndex == text.length()) {
				lines.add(text);
				text = "";
			} else {
				lastSpace = spaceIndex;
			}
		}
		return lines;
		
	}

	@Override
	public String getMIMEType() {
		return "application/pdf";
	}

	@Override
	public String getFileName() {
		String result = ConfigurationManager.getProperty(FILE_NAME_EXPORT_PDF);
		if (StringUtils.isNotEmpty(result))
			return result;
		return "export";
	}

	@Override
	public void disseminate(Context context, List<DSpaceObject> dso, OutputStream out)
			throws CrosswalkException, IOException, SQLException, AuthorizeException {
		PDDocument document = new PDDocument();
		for (DSpaceObject dd : dso) {

			PDPage page = new PDPage();
			document.addPage(page);

			createPage(context, dd, document, page, new HashMap<String, Integer>());
		}

		// Save the results and ensure that the document is properly closed:
		try {
			// Finally Let's save the PDF
			document.save(out);
		} catch (COSVisitorException e) {
			log.error(e.getMessage(), e);
		}

		document.close();
	}

}
