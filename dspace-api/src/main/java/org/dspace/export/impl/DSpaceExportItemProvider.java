/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import org.dspace.content.Item;
import org.dspace.export.api.ExportItemException;
import org.dspace.export.api.ExportItemProvider;
import org.dspace.util.ExportItemUtils;

import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.util.MarshallingUtils;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * Generic export provider based on XSL transformation. The needed information 
 * is provided in form of XOAI metadata. For further information, see the documentation
 * at https://wiki.duraspace.org/display/~joaomelo/Sharing+and+Export+Bar 
 *
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public class DSpaceExportItemProvider implements ExportItemProvider 
{
    // Attributes a DSpaceExportItemProvider should have
    private File xslt = null;
    private String id = null;
    private String image = null;
    private String contentType = null;
    private String fileExtension = null;
    
    private static final String DEFAULT_IMAGE_EXTENSION = ".png";
    private static Map<String, DSpaceExportItemProvider> providers = null;
    private static final TransformerFactory TFACTORY = TransformerFactory.newInstance();
    
    private static final ConfigurationService CONFIGURATIONSERVICE = 
                            DSpaceServicesFactory.getInstance().getConfigurationService();
    
    
    public DSpaceExportItemProvider(File xslt, String id, String contentType, 
            String fileExtension, String image) 
    {
        super();
        this.xslt = xslt;
        this.id = id;
        this.image = image;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }
    
    /**
     * Returns a specific provider, that can be used to export item's citation 
     * in common formats like RIS and BibTeX.
     * 
     * @param id
     *          ID of the provider
     * @return 
     *        Returns a provider corresponding to the given ID or null if it does not exit
     */
    public static DSpaceExportItemProvider getInstance(String id) 
    {
        boolean isExportbarEnabled = CONFIGURATIONSERVICE.getBooleanProperty("export.bar.isEnable", false);

        if (isExportbarEnabled)
        {
            if (providers == null) 
            {
                providers = new TreeMap<>();
            }
            
            if (!providers.containsKey(id)) 
            {
                String xslt = CONFIGURATIONSERVICE.getProperty("export." + id + ".xslt");

                if (xslt != null) 
                {
                    String image = CONFIGURATIONSERVICE.getProperty("export." + id + ".image");

                    if (image == null) 
                    {
                        image = id + DEFAULT_IMAGE_EXTENSION;
                    }

                    String contentType = CONFIGURATIONSERVICE.getProperty("export." + id + ".mimeType");
                    String fileExtension = CONFIGURATIONSERVICE.getProperty("export." + id + ".extension");
                    String dir = CONFIGURATIONSERVICE.getProperty("dspace.dir");
                    providers.put(id, new DSpaceExportItemProvider(new File(dir, xslt),
                            id, contentType, fileExtension, image));
                }
            }
            return providers.get(id);
        }
        return null;
    }

    @Override
    public void export(Item item, OutputStream output) 
            throws ExportItemException 
    {
        Metadata m = ExportItemUtils.retrieveMetadata(item);
        
        try {
            
            if (this.getXSLT() == null || !this.getXSLT().exists()) 
            {
                throw new ExportItemException("Invalid stylesheet for Export Provider " 
                                             + this.getId());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MarshallingUtils.writeMetadata(out, m);
            Transformer transformer = TFACTORY
                    .newTransformer(new StreamSource(this.getXSLT()));

            transformer.transform(new StreamSource(new ByteArrayInputStream(out.toByteArray())),
                    new StreamResult(output));

        }  
        catch (MetadataBindException | TransformerException e) 
        {
            throw new ExportItemException(e);
        } 
    }

    @Override
    public String getContentType() 
    {
        return contentType;
    }

    @Override
    public String getFileExtension() 
    {
        return fileExtension;
    }
    
    @Override
    public File getXSLT() 
    {
        return xslt;
    }

    @Override
    public String getId() 
    {
        return id;
    }

    @Override
    public String getImage() 
    {
        return image;
    }
}
