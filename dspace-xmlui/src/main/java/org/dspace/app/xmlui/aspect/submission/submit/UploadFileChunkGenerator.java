package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class UploadFileChunkGenerator extends AbstractGenerator{
    private static final Logger log = Logger.getLogger(UploadFileChunkGenerator.class);
    private Bitstream bitstream;
    private static final AttributesImpl emptyAttr = new AttributesImpl();
    
    public void setup(SourceResolver resolver, Map objectModel, 
            String src, Parameters par)  
                    throws ProcessingException, SAXException, IOException 
    {
        super.setup(resolver, objectModel, src, par);
        try{
            String bId = par.getParameter("bitstream_id");
            
            if(bId != null && bId.length() > 0){
                Context context = ContextUtil.obtainContext(objectModel);
                this.bitstream = Bitstream.find(context, Integer.parseInt(bId));
            }
        }
        catch(ParameterException ex){
            log.warn(ex);
        }
        catch(SQLException ex){
            log.error(ex);
        }
    } 
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        contentHandler.startDocument();
        
        contentHandler.startElement("", "upload", "upload", new AttributesImpl());
        
        if(this.bitstream != null){
            contentHandler.startElement("", "bitstreamId", "bitstreamId", emptyAttr);
            String id = String.valueOf(this.bitstream.getID());
            contentHandler.characters(id.toCharArray(), 0, id.length());
            contentHandler.endElement("","bitstreamId", "bitstreamId");

            contentHandler.startElement("", "size", "size", emptyAttr);
            String size = Long.toString(this.bitstream.getSize());
            contentHandler.characters(size.toCharArray(), 0, size.length());
            contentHandler.endElement("","size", "size");

            contentHandler.startElement("", "format", "format", emptyAttr);
            String format = this.bitstream.getFormat().getShortDescription();
            contentHandler.characters(format.toCharArray(), 0, format.length());
            contentHandler.endElement("","format", "format");

            contentHandler.startElement("", "checksum", "checksum", emptyAttr);
            String checksum = this.bitstream.getChecksumAlgorithm() + ":" + this.bitstream.getChecksum(); 
            contentHandler.characters(checksum.toCharArray(), 0, checksum.length());
            contentHandler.endElement("","checksum", "checksum");
            
            contentHandler.startElement("", "url", "url", emptyAttr);
            String url = this.bitstream.getHandle(); 
            contentHandler.characters(url.toCharArray(), 0, url.length());
            contentHandler.endElement("","url", "url");
            
        }

        contentHandler.endElement("", "upload", "upload");
        contentHandler.endDocument();
    }

}
