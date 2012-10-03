package org.dspace.springmvc;


import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
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

public class RisView implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(RisView.class);
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
        String fileName = doi.substring(doi.lastIndexOf('/') +1).replace('.', '_') + ".ris";
        write(response, getRIS((Item) item, doi), fileName);

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


    private String getRIS(Item aItem, String doi) {
		StringBuilder builder = new StringBuilder("TY  - DATA").append(EOL);
		String[] dateParts = DryadCitationHelper.getDate(aItem);
		String title = DryadCitationHelper.getTitle(aItem);
		String abstrakt = DryadCitationHelper.getAbstract(aItem);
		String[] keywords = DryadCitationHelper.getKeywords(aItem);
		String journalName = DryadCitationHelper.getJournalName(aItem);

		if (doi != null) {
			builder.append("ID  - ").append(doi).append(EOL);
		}

		// Title for data package
		if (title != null) {
			builder.append("T1  - ").append(title).append(EOL);
		}

		// Authors for data package
		for (String author : DryadCitationHelper.getAuthors(aItem)) {
			builder.append("AU  - ").append(author).append(EOL);
		}

		// Date for data package
		if (dateParts.length > 0) {
			int count = 3;

			builder.append("Y1  - ");

			if (dateParts.length < 3) {
				count = dateParts.length;
			}

			for (int index = 0; index < count; index++) {
				builder.append(dateParts[index]).append("/");
			}

			for (; count < 3; count++) {
				builder.append('/');
			}

			builder.append(EOL);
		}

		if (abstrakt != null) {
			builder.append("N2  - ").append(abstrakt).append(EOL);
		}

		for (String keyword : keywords) {
			builder.append("KW  - ").append(keyword).append(EOL);
		}

		if (journalName != null) {
			builder.append("JF  - ").append(journalName).append(EOL);
		}

		builder.append("PB  - ").append("Dryad Data Repository").append(EOL);

		if (doi != null) {
			builder.append("UR  - ");
			builder.append(DOI_URL + doi.substring(4, doi.length()));
			builder.append(EOL);

			builder.append("DO  - ").append(doi).append(EOL);
		}

		return builder.append("ER  - ").append(EOL).toString();
	}
}
