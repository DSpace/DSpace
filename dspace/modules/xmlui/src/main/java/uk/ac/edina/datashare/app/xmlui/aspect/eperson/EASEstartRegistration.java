package uk.ac.edina.datashare.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.xml.sax.SAXException;

import uk.ac.edina.datashare.utils.Consts;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.XmlUtils;

public class EASEstartRegistration extends AbstractDSpaceTransformer
{
    private String uun = null;
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings({ "rawtypes" })
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        
        this.uun = parameters.getParameter("uun", "");
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#addBody(org.dspace.app.xmlui.wing.element.Body)
     */
    public void addBody(Body body) throws WingException
    {
    	// for the timebeing don't any thing on DEV or BETA
    	if(DSpaceUtils.isLive()){
    		if(this.uun == null || this.uun.length() == 0)
    		{
    			HttpServletResponse response = XmlUtils.getResponse(objectModel);
    			String url = response.encodeRedirectURL(
    			        XmlUtils.getRequest(objectModel).getContextPath() + "/" +
    							Consts.LOGIN_PAGE);

    			try
    			{
    				response.sendRedirect(url);
    			}
    			catch(IOException ex)
    			{
    				throw new RuntimeException(ex);
    			}
    		}
    		else
    		{
    			//  add uun to as hidden value
    			Division register = body.addInteractiveDivision("register",
    					contextPath+"/register",Division.METHOD_POST,"primary");
    			register.addHidden("uun").setValue(this.uun);
    		}
    	}
    }
}