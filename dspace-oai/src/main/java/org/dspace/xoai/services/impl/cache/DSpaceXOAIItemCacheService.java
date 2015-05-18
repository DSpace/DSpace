/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.cache;

import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.dataprovider.xml.xoai.XOAIParser;
import org.apache.commons.io.FileUtils;
import org.dspace.content.Item;
import org.dspace.xoai.services.api.cache.XOAIItemCacheService;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.apache.commons.io.FileUtils.deleteDirectory;


public class DSpaceXOAIItemCacheService implements XOAIItemCacheService {
    private static final String ITEMDIR = File.separator + "items";

    @Autowired
    ConfigurationService configurationService;

    private String baseDir;

    private String getBaseDir()
    {
        if (baseDir == null)
            baseDir = configurationService.getProperty("oai", "cache.dir") + ITEMDIR;
        return baseDir;
    }


    private File getMetadataCache(Item item)
    {
        File dir = new File(getBaseDir());
        if (!dir.exists())
            dir.mkdirs();

        String name = File.separator + item.getHandle().replace('/', '_');
        return new File(getBaseDir() + name);
    }




    @Override
    public boolean hasCache(Item item) {
        return getMetadataCache(item).exists();
    }





    @Override
    public Metadata get(Item item) throws IOException {
        System.out.println(FileUtils.readFileToString(getMetadataCache(item)));
        Metadata metadata;
        FileInputStream input = new FileInputStream(getMetadataCache(item));
        try {
            metadata = XOAIParser.parse(input);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        input.close();
        
        return metadata;
    }





    @Override
    public void put(Item item, Metadata metadata) throws IOException {
        FileOutputStream output = new FileOutputStream(getMetadataCache(item));
        try {
            XmlOutputContext context = XmlOutputContext.emptyContext(output, Second);
            metadata.write(context);
            context.getWriter().flush();
            context.getWriter().close();
            
            output.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } catch (WritingXmlException e) {
            throw new IOException(e);
        }
    }


    @Override
    public void delete(Item item) {
        this.getMetadataCache(item).delete();
    }


    @Override
    public void deleteAll() throws IOException {
        deleteDirectory(new File(getBaseDir()));
    }

}
