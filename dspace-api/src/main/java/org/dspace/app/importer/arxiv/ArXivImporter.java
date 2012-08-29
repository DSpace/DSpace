package org.dspace.app.importer.arxiv;


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

public class ArXivImporter extends AReflectorImporter<ArXivItem>
{
    protected String getImportIdentifier(String record) throws Exception
    {
        return record;
    }

    protected ArXivItem getInternalImportItem(String record) throws Exception
    {
        if (!ConfigurationManager.getBooleanProperty("importer.remoteservice.demo"))
        {
            GetMethod method = null;
            try
            {
                HttpClient client = new HttpClient();
                method = new GetMethod("http://export.arxiv.org/api/query");
        
                NameValuePair id = new NameValuePair("id_list", record);
                method.setQueryString(new NameValuePair[] { id });
                // Execute the method.
                int statusCode = client.executeMethod(method);
        
                if (statusCode != HttpStatus.SC_OK)
                {
                    if (statusCode == HttpStatus.SC_BAD_REQUEST)
                        throw new RuntimeException("ID arXiv not valid");
                    else
                        throw new RuntimeException("HTTP error: "
                                + method.getStatusLine());
                }
        
                ArXivItem crossitem;
                try
                {
                    crossitem = new ArXivItem(method.getResponseBodyAsStream());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(
                            "ID arXiv invalid or not found");
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
                File file = new File(ConfigurationManager.getProperty("dspace.dir")+"/config/crosswalks/demo/arxiv.xml");
                stream = new FileInputStream(file);
                return new ArXivItem(stream);
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

    protected Class<ArXivItem> getItemImportClass()
    {
        return ArXivItem.class;
    }

    protected String getType(ArXivItem crossitem)
    {
        return "default";
    }

    protected Set<String> getSingleRecordDataImport(String data)
    {
        Set<String> dois = new HashSet<String>();
        String[] rawDOIs = data.split("\\s+");
        for (String raw : rawDOIs)
        {
            if (StringUtils.isNotBlank(raw))
            {
                if (raw.startsWith("http://arxiv.org/abs/"))
                {
                    raw = raw.substring("http://arxiv.org/abs/".length());
                }
                else if (raw.startsWith("arXiv:"))
                {
                    raw = raw.substring("arXiv:".length());
                }
                else if (raw.startsWith("arxiv:"))
                {
                    raw = raw.substring("arxiv:".length());
                }
                dois.add(raw);
            }
        }
        return dois;
    }
}
