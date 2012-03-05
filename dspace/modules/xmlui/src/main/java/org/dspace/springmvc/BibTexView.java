package org.dspace.springmvc;


import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.doi.DOI;
import org.dspace.doi.DryadCitationHelper;
import org.dspace.identifier.DOIIdentifierProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/20/11
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */

public class BibTexView implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(BibTexView.class);
    private static final String EOL = "\r\n";
    private static final String DOI_URL = "http://dx.doi.org/";


    public String getContentType() {

        return "text/plain;charset=utf-8";
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        DSpaceObject item = (DSpaceObject)request.getAttribute(ResourceIdentifierController.DSPACE_OBJECT);
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        String doi = DOIIdentifierProvider.getDoiValue((Item) item);
        String fileName = doi.substring(doi.lastIndexOf('/') +1).replace('.', '_') + ".bib";
        write(response, getBibTex((Item) item, doi), fileName);

        OutputStream aOutputStream = response.getOutputStream();
        aOutputStream.close();

    }

    private void write(HttpServletResponse aResponse, String aContent, String aFileName) throws IOException {
		aResponse.setContentType("text/plain;charset=utf-8");
		aResponse.setContentLength(aContent.length());
		aResponse.setHeader("Content-Disposition", "attachment; filename=\""
				+ aFileName + "\"");

		// It's all over but the writing...
		PrintWriter writer = aResponse.getWriter();
		writer.print(aContent);
		writer.close();
	}



    private String getBibTex(Item aItem, String doi) {
		// No standardized format for data so using 'misc' for now
		StringBuilder builder = new StringBuilder("@misc{");

		String key = new DOI(doi, aItem).getSuffix().replace('.', '_');
		String[] authors = DryadCitationHelper.getAuthors(aItem);
		String year = DryadCitationHelper.getYear(aItem);
		String journalName = DryadCitationHelper.getJournalName(aItem);
		String title = DryadCitationHelper.getTitle(aItem);

		builder.append(key).append(',').append(EOL);

		if (title != null) {
			builder.append("  title = {").append(title).append("},");
			builder.append(EOL);
		}

		if (authors.length > 0) {
			builder.append("  author = {");

			// Bibtex needs the comma... do we want full names here?
			for (int index = 0; index < authors.length; index++) {
				if (index + 1 >= authors.length) { // last one
					builder.append(authors[index].replace(" ", ", "));
				}
				else if (index + 1 < authors.length) { // not last one
					builder.append(authors[index].replace(" ", ", "));
					builder.append(" and ");
				}
			}

			builder.append("},").append(EOL);
		}

		if (year != null) {
			builder.append("  year = {").append(year).append("},").append(EOL);
		}

		if (journalName != null) {
			builder.append("  journal = {").append(journalName).append("},");
			builder.append(EOL);
		}

		if (doi != null) {
			builder.append("  URL = {");
			builder.append(DOI_URL + doi.substring(4, doi.length()));
			builder.append("},");
			builder.append(EOL);

			builder.append("  doi = {");
			builder.append(doi);
			builder.append("},").append(EOL);
		}

		// this should always be last so we don't have to worry about a comma
		builder.append("  publisher = {Dryad Data Repository}").append(EOL);
		return builder.append("}").append(EOL).toString();
	}

}
