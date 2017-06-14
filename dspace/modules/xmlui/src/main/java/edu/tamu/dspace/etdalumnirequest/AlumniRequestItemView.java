package edu.tamu.dspace.etdalumnirequest;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
 * A widget placed on the item view page of a restricted legacy thesis. The "xmlui.alumni.request.collections" parameter determines whether the widget will be displayed. 
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 */

public class AlumniRequestItemView extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    
    /** Cached validity object */
    private SourceValidity validity = null;

    
        
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
      	throws ProcessingException, SAXException, IOException
	{
		super.setup(resolver, objectModel, src, parameters);
	}
    
    
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0"; // no item, something is wrong.
            }

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object will include the item being viewed,
     * along with all bundles & bitstreams.
     */
    public SourceValidity getValidity()
    {
        DSpaceObject dso = null;

        if (this.validity == null)
    	{
	        try {
	            dso = HandleUtil.obtainHandle(objectModel);

	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(dso);
	            this.validity =  validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Ignore all errors and just invalidate the cache.
	        }

    	}
    	return this.validity;
    }
    
    
    

    /** What to add at the end of the body */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	    	
        if (!AlumniRequest.isRequestable(context, dso))
            return;
            
        Division current = body.addDivision("alumni-request-item","primary");
        
        Division inner = current.addDivision("alumni-request-item-form");
        
        inner.setHead("Request Open Access");
        inner.addPara("This item and its contents are restricted. If this is your thesis or dissertation, you can make it open-access. This will allow all visitors " +
        		"to view the contents of the thesis.");
        inner.addPara().addXref("http://asktamulib.altarama.com/reft100.aspx?key=DSSC_OA&cllcid=DSSC", "Request Open Access");
                
        /*
        Division buttons = current.addInteractiveDivision("alumni-request-item-form", contextPath + "/handle/"+dso.getHandle()+"/alumni-request", Division.METHOD_POST);
        buttons.setHead("Request Open Access");
        buttons.addPara("This item and its contents are restricted. If this is your thesis or dissertation, you can make it open-access. This will allow all visitors " +
        		"to view the contents of the thesis.");
        buttons.addPara(null, "button-list").addButton("submit").setValue("Request Open Access");
        */
    }
    
    
    /**
     * Recycle
     */
    public void recycle() {
    	this.validity = null;
    	super.recycle();
    }
   
}
