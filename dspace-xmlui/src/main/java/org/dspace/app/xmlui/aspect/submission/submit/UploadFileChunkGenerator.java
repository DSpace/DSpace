package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class UploadFileChunkGenerator  extends AbstractGenerator{
    private static final Logger log = Logger.getLogger(UploadFileChunkGenerator.class);
    private String bitstreamId;
    
    //AttributesImpl emptyAttr = ;
    public void setup(SourceResolver resolver, Map objectModel, 
            String src, Parameters par)  
                    throws ProcessingException, SAXException, IOException 
    {
        super.setup(resolver, objectModel, src, par);
        /*request = ObjectModelHelper.getRequest(objectModel);
        paramNames = request.getParameterNames();
        uri = request.getRequestURI();*/
        try{
            log.info("************** " + par.getParameter("bitstream_id"));
            this.bitstreamId = par.getParameter("bitstream_id");
        }
        catch(ParameterException ex){
            log.info("jings");
        }
    } 
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        log.info("**************************************");
        contentHandler.startDocument();
        contentHandler.startElement("", "bitstreamId", "bitstreamId", new AttributesImpl());
        if(this.bitstreamId != null){
            contentHandler.characters(this.bitstreamId.toCharArray(), 0, this.bitstreamId.length());
        }
        contentHandler.endElement("","bitstreamId", "bitstreamId");
        contentHandler.endDocument();
    }

}
