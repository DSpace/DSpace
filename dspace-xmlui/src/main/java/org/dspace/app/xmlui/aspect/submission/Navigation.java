/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * Simple navigation class to add the top level link to 
 * the main submissions page.
 * 
 * @author Scott Phillips
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	 /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        return 1;
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }

    @Override
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        EPerson eperson = context.getCurrentUser();

        if (eperson != null) {
            userMeta.addMetadata("identifier","submissionsURL")
                    .addContent(contextPath + "/submissions");
        }
    }
	
   
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
		// Basic navigation skeleton
        options.addList("browse");
        options.addList("context");
        options.addList("administrative");
    }
}
