/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Display a single collection. This includes a full text search, browse by
 * list, community display and a list of recent submissions.
 * 
 * @author Scott Phillips
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class AddItemCollections extends AbstractDSpaceTransformer implements CacheableProcessingComponent 
{
	/** Cached validity object */
	private SourceValidity validity = null;
	
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

            return HashUtil.hash(dso.getHandle() + "full:" + showFullItem(objectModel)+"withCommName");
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
    
    /*
     * Agrega las colecciones con sus comunidades padres
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        // Build the item viewer division.
        Division division = body.addDivision("item-view-sedici","primary");

        List colecciones=division.addList("item-view-sedici-colecciones");
        
        // Reference all collections the item appears in.
        for (Collection collection : item.getCollections()) 
        {
        	List coleccion=colecciones.addList("item-view-sedici-coleccion");
        	Community[] comunidades=collection.getCommunities();
        	if (comunidades.length>0){
        		Community comunidad=comunidades[0];
        		coleccion.addItemXref(comunidad.getHandle(), comunidad.getName());        		
        	}
        	coleccion.addItemXref(collection.getHandle(), collection.getName()); 
        	

        }


    }
    
    /**
     * Determine if the full item should be referenced or just a summary.
     */
    public static boolean showFullItem(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("show");

        if (show != null && show.length() > 0)
        {
            return true;
        }

        return false;
    }

    /**
     * Obtain the item's title.
     */
    public static String getItemTitle(Item item)
    {
        DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

        String title;
        if (titles != null && titles.length > 0)
        {
            title = titles[0].value;
        }
        else
        {
            title = null;
        }
        return title;
    }

}
