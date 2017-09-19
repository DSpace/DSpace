package uk.ac.edina.datashare.app.xmlui.aspect.datashare;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Display result of an attempt to agree to the terms of the Depositor Agreement,
 */
public class DepositAgree extends AbstractDSpaceTransformer{
    private static Logger LOG = Logger.getLogger(DepositAgree.class);
    
    private static final Message TITLE =
            message("datashare.depositagree.title");
    private static final Message SUCCESS =
            message("datashare.depositagree.success");
    private static final Message FAIL =
            message("datashare.depositagree.fail");
    private static final Message HOME =
            message("xmlui.general.dspace_home");
    
    private String errorMessage;
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings("rawtypes")
    public void setup(
            SourceResolver resolver,
            Map objectModel,
            String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver,objectModel,src,parameters);
        
        try{
            String result = parameters.getParameter("result");
            if(result.length() > 0){
                this.errorMessage = result;
            }
            else{
                this.errorMessage = null;
            }
        }
        catch(ParameterException ex){
            LOG.warn(ex);
            errorMessage = "Invalid state";
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#addBody(org.dspace.app.xmlui.wing.element.Body)
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    {
        Division div = body.addDivision("datashare-deposit-agree");
        div.setHead(TITLE);
        
        if(this.errorMessage == null){
            div.addPara(SUCCESS);
        }
        else{
            div.addPara(FAIL.parameterize(this.errorMessage,
                    ConfigurationManager.getProperty("mail.admin")));
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#addPageMeta(org.dspace.app.xmlui.wing.element.PageMeta)
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(TITLE);
        pageMeta.addTrailLink(contextPath + "/", HOME);
        pageMeta.addTrail().addContent(TITLE);
    }
}
