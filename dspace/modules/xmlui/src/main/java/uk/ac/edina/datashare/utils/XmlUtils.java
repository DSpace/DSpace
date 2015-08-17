package uk.ac.edina.datashare.utils;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.environment.http.HttpEnvironment;

public class XmlUtils {
    /**
     * Fetch the HTTP request object from a coccon object model.
     * @param objectModel The cocoon object model.
     * @return HTTP request object.
     */
    @SuppressWarnings("rawtypes")
    public static HttpServletRequest getRequest(Map objectModel)
    {
        return (HttpServletRequest)objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT); 
    }
    
    /**
     * Fetch the HTTP response object from a coccon object model.
     * @param objectModel The cocoon object model.
     * @return HTTP response object.
     */
    @SuppressWarnings("rawtypes")
    public static HttpServletResponse getResponse(Map objectModel)
    {
        return (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT); 
    }

    /**
     * Fetch the HTTP session object from a cocoon object model.
     * @param objectModel The cocoon object model.
     * @return HTTP session object.
     */
/*    @SuppressWarnings("rawtypes")
    public static HttpSession getSession(Map objectModel)
    {
        return getRequest(objectModel).getSession();
    }
*/    
    /**
     * Create an item checker object.
     * @param item The DSpace item.
     * @return An item checker object.
     */
/*    public static AbstractItemChecker createItemChecker(DSpaceObject item)
    {
        AbstractItemChecker checker = getFactory().getChecker();
        checker.setItemId(item.getID());
        
        return checker;
    }
*/
}
