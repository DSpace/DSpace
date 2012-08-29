package org.dspace.app.importer.pubmed;


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

public class PubmedImporter extends AReflectorImporter<PubmedItem>
{
    protected String getImportIdentifier(String record) throws Exception
    {
        return record;
    }

    protected Class<PubmedItem> getItemImportClass()
    {
        return PubmedItem.class;
    }

    protected String getType(PubmedItem crossitem)
    {
        return "default";
    }

    @Override
    protected Set<String> getSingleRecordDataImport(String data)
    {
        Set<String> pubmedid = new HashSet<String>();
        String[] rawDOIs = data.split("\\s+");
        for (String raw : rawDOIs)
        {
            if (StringUtils.isNotBlank(raw))
            {
                pubmedid.add(raw);
            }
        }
        return pubmedid;
    }

    protected PubmedItem getInternalImportItem(String record) throws Exception
    {
        if (!ConfigurationManager.getBooleanProperty("importer.remoteservice.demo"))
        {
            GetMethod method = null;
            try
            {
                HttpClient client = new HttpClient();
                method = new GetMethod(
                        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");
        
                NameValuePair db = new NameValuePair("db", "pubmed");
                NameValuePair retmode = new NameValuePair("retmode", "xml");
                NameValuePair rettype = new NameValuePair("rettype", "full");
                NameValuePair id = new NameValuePair("id", record);
                method.setQueryString(new NameValuePair[] { db, retmode, rettype, id });
                // Execute the method.
                int statusCode = client.executeMethod(method);
        
                if (statusCode != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("Webservice call failure: "
                            + method.getStatusLine());
                }
        
                PubmedItem crossitem = null;
                try {
                    crossitem = new PubmedItem(method.getResponseBodyAsStream());
                }
                catch (Exception e) {
                    throw new RuntimeException("PubmedID invalid or not found");
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
                File file = new File(ConfigurationManager.getProperty("dspace.dir")+"/config/crosswalks/demo/pubmed.xml");
                stream = new FileInputStream(file);
                return new PubmedItem(stream);
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
    
}
