package org.dspace.xoai.util;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dspace.xoai.exceptions.MetadataBindException;

import com.lyncode.xoai.common.dataprovider.exceptions.MarshallingException;
import com.lyncode.xoai.common.dataprovider.util.MarshallingUtils;
import com.lyncode.xoai.common.dataprovider.xml.PrefixMapper;
import com.lyncode.xoai.common.dataprovider.xml.xoai.Metadata;

public class XMLBindUtils
{

    //private static Logger log = LogManager.getLogger(ParsingUtils.class);
    public static Metadata readMetadata (InputStream in) throws MetadataBindException {
        try
        {
            JAXBContext context = JAXBContext.newInstance(Metadata.class
                    .getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Metadata) unmarshaller.unmarshal(in);
        }
        catch (JAXBException e)
        {
            throw new MetadataBindException(e);
        }
    }
    
    public static void writeMetadata (OutputStream out, Metadata meta) throws MetadataBindException {
        try
        {
            MarshallingUtils.marshalWithoutXMLHeader(Metadata.class
                    .getPackage().getName(), meta, new PrefixMapper(),
                    out);
        }
        catch (MarshallingException e)
        {
            throw new MetadataBindException(e);
        }
    }
}
