/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.dspace.core.service.NewsService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Encapsulate access to the news texts.
 *
 * @author mhwood
 */
public class NewsServiceImpl implements NewsService
{
    private final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

	private List<String> acceptableFilenames;

    @Autowired(required = true)
    private ConfigurationService configurationService;
	
	public void setAcceptableFilenames(List<String> acceptableFilenames) {
        this.acceptableFilenames = addLocalesToAcceptableFilenames(acceptableFilenames);
    }

    protected List<String> addLocalesToAcceptableFilenames(List<String> acceptableFilenames){
        String [] locales = configurationService.getArrayProperty("webui.supported.locales");
        List<String> newAcceptableFilenames = new ArrayList<>();
        newAcceptableFilenames.addAll(acceptableFilenames);
        for(String local : locales){
            for(String acceptableFilename : acceptableFilenames){
                int lastPoint = acceptableFilename.lastIndexOf(".");
                newAcceptableFilenames.add(
                        acceptableFilename.substring(0, lastPoint)
                                + "_"
                                + local
                                + acceptableFilename.substring(lastPoint));
            }
        }
        return newAcceptableFilenames;
    }


    /** Not instantiable. */
    protected NewsServiceImpl() {}

    @Override
    public String readNewsFile(String newsFile)
    {
    	if (!validate(newsFile)) {
    		throw new IllegalArgumentException("The file "+ newsFile + " is not a valid news file");
    	}
        String fileName = getNewsFilePath();

        fileName += newsFile;

        StringBuilder text = new StringBuilder();

        try
        {
            // retrieve existing news from file
            FileInputStream fir = new FileInputStream(fileName);
            InputStreamReader ir = new InputStreamReader(fir, "UTF-8");
            BufferedReader br = new BufferedReader(ir);

            String lineIn;

            while ((lineIn = br.readLine()) != null)
            {
                text.append(lineIn);
            }

            br.close();
            ir.close();
            fir.close();
        }
        catch (IOException e)
        {
            log.warn("news_read: " + e.getLocalizedMessage());
        }

        return text.toString();
    }

    @Override
    public String writeNewsFile(String newsFile, String news)
    {
    	if (!validate(newsFile)) {
    		throw new IllegalArgumentException("The file "+ newsFile + " is not a valid news file");
    	}
        String fileName = getNewsFilePath();

        fileName += newsFile;

        try
        {
            // write the news out to the appropriate file
            FileOutputStream fos = new FileOutputStream(fileName);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(news);
            out.close();
        }
        catch (IOException e)
        {
            log.warn("news_write: " + e.getLocalizedMessage());
        }

        return news;
    }

    @Override
    public String getNewsFilePath()
    {
        String filePath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
                + File.separator + "config" + File.separator;
        return filePath;
    }
    
	@Override
	public boolean validate(String newsName) {
		if (acceptableFilenames != null) {
			return acceptableFilenames.contains(newsName);
		}
		return false;
	}
}
