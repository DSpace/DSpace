package org.dspace.app.importer.crossref;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.importer.AReflectorImporter;
import org.dspace.core.ConfigurationManager;

public class CrossrefImporter extends AReflectorImporter<CrossrefItem>
{
    protected String getImportIdentifier(String record) throws Exception
    {
        return record;
    }

    protected CrossrefItem getInternalImportItem(String record) throws Exception
    {
        if (!ConfigurationManager.getBooleanProperty("importer.remoteservice.demo"))
        {
            GetMethod method = null;
            try
            {
                String apiKey = ConfigurationManager.getProperty("importer.crossref.api-key");
                
                HttpClient client = new HttpClient();
                method = new GetMethod("http://www.crossref.org/openurl/");
        
                NameValuePair pid = new NameValuePair("pid", apiKey);
                NameValuePair noredirect = new NameValuePair("noredirect", "true");
                NameValuePair id = new NameValuePair("id", record);
                method.setQueryString(new NameValuePair[] { pid, noredirect, id });
                // Execute the method.
                int statusCode = client.executeMethod(method);
        
                if (statusCode != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("HTTP error: "
                            + method.getStatusLine());
                }
        
                CrossrefItem crossitem;
                try
                {
                    crossitem = new CrossrefItem(method
                            .getResponseBodyAsStream());
                }
                catch (Exception e)
                {
                    throw new RuntimeException("DOI invalid or not found");
                }
                return crossitem;
            }
            finally
            {
                if (method != null)
                {
                    method.releaseConnection();
                }
            }
        }
        else
        {
            InputStream stream = null; 
            try {
                File file = new File(ConfigurationManager.getProperty("dspace.dir")+"/config/crosswalks/demo/doi.xml");
                stream = new FileInputStream(file);
                return new CrossrefItem(stream);
            }
            finally
            {
                if (stream != null)
                {
                    stream.close();
                }
            }
        }
    }

    protected Class<CrossrefItem> getItemImportClass()
    {
        return CrossrefItem.class;
    }

    protected String getType(CrossrefItem crossitem)
    {
        return crossitem.getItemType();
    }

    protected Set<String> getSingleRecordDataImport(String data)
    {
        Set<String> dois = new HashSet<String>();
        String[] rawDOIs = data.split("\\s+");
        for (String raw : rawDOIs)
        {
            if (StringUtils.isNotBlank(raw))
            {
                if (raw.startsWith("http://dx.doi.org/"))
                {
                    raw = raw.substring("http://dx.doi.org/".length());
                }
                else if (raw.startsWith("doi:"))
                {
                    raw = raw.substring("doi:".length());
                }
                dois.add(raw);
            }
        }
        return dois;
    }
}
