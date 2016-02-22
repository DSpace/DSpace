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
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class UploadFileChunkGenerator extends AbstractGenerator
{
    private static final Logger log = Logger.getLogger(UploadFileChunkGenerator.class);
    private Bitstream bitstream;
    private String error;
    private static final AttributesImpl emptyAttr = new AttributesImpl();
    
    public void setup(SourceResolver resolver, @SuppressWarnings("rawtypes") Map objectModel, 
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
            
            this.error = par.getParameter("error");
        }
        catch(ParameterException ex){}
        catch(SQLException ex){
            log.error(ex);
        }
    } 
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException
    {
        contentHandler.startDocument();
        contentHandler.startElement("", "upload", "upload", new AttributesImpl());
        
        if(this.bitstream != null)
        {
            this.addElement(contentHandler, "bitstreamId", String.valueOf(this.bitstream.getID()));
            this.addElement(contentHandler, "size", Long.toString(this.bitstream.getSize()));
            this.addElement(contentHandler, "format", this.bitstream.getFormat().getShortDescription());
            this.addElement(contentHandler, "checksum", this.bitstream.getChecksumAlgorithm() + ":" + this.bitstream.getChecksum());
            this.addElement(contentHandler, "sequenceId", String.valueOf(this.bitstream.getSequenceID()));
        }
        
        if(this.error != null && this.error.length() > 0){
            this.addElement(contentHandler, "errorkey", this.error);
        }

        contentHandler.endElement("", "upload", "upload");
        contentHandler.endDocument();
    }
    
    private void addElement(ContentHandler contentHandler, String name, String value) throws SAXException{
        contentHandler.startElement("", name, name, emptyAttr);
        contentHandler.characters(value.toCharArray(), 0, value.length());
        contentHandler.endElement("", name, name);
    }
}
