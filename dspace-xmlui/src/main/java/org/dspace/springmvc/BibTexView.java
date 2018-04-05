/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.springmvc;


import org.dspace.content.MetadataValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */

public class BibTexView implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(BibTexView.class);
    private static final String EOL = "\r\n";

    private String resourceIdentifier=null;
    
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    

    public String getContentType() {

        return "text/plain;charset=utf-8";
    }

    public BibTexView(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        DSpaceObject item = (DSpaceObject)request.getAttribute(ResourceIdentifierController.DSPACE_OBJECT);

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        String fileName = getFileName(item);

        write(response, getBibTex((Item) item, resourceIdentifier), fileName);

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


    private String getFileName(DSpaceObject item)
    {
        String fileName = resourceIdentifier;
        if(resourceIdentifier.lastIndexOf("/") !=-1)
        {
            fileName = resourceIdentifier.replaceAll("/", "_") + ".bib";
        }

        return fileName;
    }


    private String getBibTex(Item item, String resourceIdentifier) {
		// No standardized format for data so using 'misc' for now
		StringBuilder builder = new StringBuilder("@misc{");

		String[] authors = getAuthors(item);
		String year = getYear(item);
        String title = getMetadataValue(item, "dc.title");

		builder.append(resourceIdentifier).append(',').append(EOL);

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

		return builder.append("}").append(EOL).toString();
	}

    private String getMetadataValue(Item item, String metadatafield)
    {
        for (MetadataValue value : itemService.getMetadataByMetadataString(item, metadatafield))
        {
            return value.getValue();
        }
        return null;
    }


    private String[] getAuthors(Item aItem)
    {
        ArrayList<String> authors = new ArrayList<String>();

        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.contributor.author")));
        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.creator")));
        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.contributor")));

        return authors.toArray(new String[authors.size()]);
    }

    private String getYear(Item aItem)
    {
        for (MetadataValue date : itemService.getMetadataByMetadataString(aItem, "dc.date.issued"))
        {
            return date.getValue().substring(0, 4);
        }

        return null;
    }

    private List<String> getAuthors(List<MetadataValue> aMetadata)
    {
        ArrayList<String> authors = new ArrayList<String>();
        StringTokenizer tokenizer;

        for (MetadataValue metadata : aMetadata)
        {
            StringBuilder builder = new StringBuilder();

            if (metadata.getValue().indexOf(",") != -1)
            {
                String[] parts = metadata.getValue().split(",");

                if (parts.length > 1)
                {
                    tokenizer = new StringTokenizer(parts[1], ". ");
                    builder.append(parts[0]).append(" ");

                    while (tokenizer.hasMoreTokens())
                    {
                        builder.append(tokenizer.nextToken().charAt(0));
                    }
                }
                else
                {
                    builder.append(metadata.getValue());
                }

                authors.add(builder.toString());
            }
            // Now the minority case (as we've cleaned up data and input method)
            else
            {
                String[] parts = metadata.getValue().split("\\s+|\\.");
                String name = parts[parts.length - 1].replace("\\s+|\\.", "");

                builder.append(name).append(" ");

                for (int index = 0; index < parts.length - 1; index++)
                {
                    if (parts[index].length() > 0)
                    {
                        name = parts[index].replace("\\s+|\\.", "");
                        builder.append(name.charAt(0));
                    }
                }
            }
        }
        return authors;
    }

}
