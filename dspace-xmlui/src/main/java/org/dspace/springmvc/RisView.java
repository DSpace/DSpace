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

public class RisView implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(RisView.class);
    private static final String EOL = "\r\n";

    private String resourceIdentifier=null;
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    
    public RisView(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    public String getContentType()
    {

        return "text/plain;charset=utf-8";
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        DSpaceObject item = (DSpaceObject)request.getAttribute(ResourceIdentifierController.DSPACE_OBJECT);

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        String fileName = getFileName(item);

        write(response, getRIS((Item) item, resourceIdentifier), fileName);
        OutputStream aOutputStream = response.getOutputStream();
        aOutputStream.close();
    }

    private String getFileName(DSpaceObject item)
    {
        String fileName = resourceIdentifier;
        if(resourceIdentifier.indexOf("/") !=-1)
        {
            fileName = resourceIdentifier.replaceAll("/", "_") + ".ris";
        }
        return fileName;
    }

    private void write(HttpServletResponse aResponse, String aContent, String aFileName) throws IOException
    {
		aResponse.setContentType("text/plain;charset=utf-8");
		aResponse.setContentLength(aContent.length());
		aResponse.setHeader("Content-Disposition", "attachment; filename=\"" + aFileName + "\"");

		// It's all over but the writing...
		PrintWriter writer = aResponse.getWriter();
		writer.print(aContent);
		writer.close();
	}


    private String getRIS(Item item, String resourceIdentifier)
    {
		StringBuilder builder = new StringBuilder("TY  - DATA").append(EOL);

        String[] dateParts = getDate(item);
		String title = getMetadataValue(item, "dc.title");
		String description = getMetadataValue(item, "dc.description");

        String[] keywords = getKeywords(item);


		if (resourceIdentifier != null)
        {
			builder.append("ID  - ").append(resourceIdentifier).append(EOL);
		}

		if (title != null)
        {
			builder.append("T1  - ").append(title).append(EOL);
		}

		for (String author : getAuthors(item)) {
			builder.append("AU  - ").append(author).append(EOL);
		}

		// Date for data package
		if (dateParts.length > 0)
        {
			int count = 3;

			builder.append("Y1  - ");

			if (dateParts.length < 3)
            {
				count = dateParts.length;
			}

			for (int index = 0; index < count; index++) {
				builder.append(dateParts[index]).append("/");
			}

			for (; count < 3; count++)
            {
				builder.append('/');
			}

			builder.append(EOL);
		}

		if (description != null)
        {
			builder.append("N2  - ").append(description).append(EOL);
		}

		for (String keyword : keywords)
        {
			builder.append("KW  - ").append(keyword).append(EOL);
		}

		return builder.append("ER  - ").append(EOL).toString();
	}


    private String[] getAuthors(Item aItem)
    {
        ArrayList<String> authors = new ArrayList<String>();

        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.contributor.author")));
        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.creator")));
        authors.addAll(getAuthors(itemService.getMetadataByMetadataString(aItem, "dc.contributor")));

        return authors.toArray(new String[authors.size()]);
    }

    private String[] getKeywords(Item aItem)
    {
        ArrayList<String> keywordList = new ArrayList<String>();

        for (MetadataValue keyword : itemService.getMetadataByMetadataString(aItem, "dc.subject"))
        {
            if (keyword.getValue().length() < 255)
            {
                keywordList.add(keyword.getValue());
            }
        }

        for (MetadataValue keyword : itemService.getMetadataByMetadataString(aItem, "dwc.ScientificName"))
        {
            if (keyword.getValue().length() < 255)
            {
                keywordList.add(keyword.getValue());
            }
        }

        return keywordList.toArray(new String[keywordList.size()]);
    }

    private String[] getDate(Item item)
    {
        StringTokenizer tokenizer;

        for (MetadataValue date : itemService.getMetadataByMetadataString(item, "dc.date.issued"))
        {
            tokenizer = new StringTokenizer(date.getValue(), "-/ T");
            String[] dateParts = new String[tokenizer.countTokens()];

            for (int index = 0; index < dateParts.length; index++)
            {
                dateParts[index] = tokenizer.nextToken();
            }

            return dateParts;
        }

        return new String[0];
    }

    private String getMetadataValue(Item item, String metadatafield)
    {
        for (MetadataValue value : itemService.getMetadataByMetadataString(item, metadatafield))
        {
            return value.getValue();
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
