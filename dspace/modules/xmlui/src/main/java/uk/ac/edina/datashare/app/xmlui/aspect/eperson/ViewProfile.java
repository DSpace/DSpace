package uk.ac.edina.datashare.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import uk.ac.edina.datashare.db.DbQuery;

/**
 * Add SWORD key to DataShare user profile page. 
 */
public class ViewProfile extends AbstractDSpaceTransformer{
    private String swordkey = null;
    
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
            Context context = ContextUtil.obtainContext(objectModel);
            this.swordkey = DbQuery.fetchSwordKey(context);
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
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
        Division div = body.addDivision("datashare-profile");
        div.setHead(message("datashare.profile.head"));
        div.addSimpleHTMLFragment(true, "<p>" + this.swordkey + "</p>");
    }
}
