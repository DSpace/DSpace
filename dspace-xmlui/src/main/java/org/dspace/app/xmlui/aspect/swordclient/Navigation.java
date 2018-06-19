/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{

    private static final Message T_context_swordclient_head = message("xmlui.swordclient.Navigation.context_head");
    private static final Message T_swordclient_copy = message("xmlui.swordclient.Navigation.context_copy");

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        return 1;
    }


    /**
     * Generate the cache validity object.
     *
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }


    public void addOptions(Options options) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
    {
        // todo : Some other navigation classes seem to add skeleton lists. I haven't done so here as
        // todo : I don't understand what they do.

        List context = options.addList("context");

        // Context Administrative options
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso instanceof Item)
        {
            Item item = (Item) dso;

            if (authorizeService.isAdmin(this.context, dso))
            {
                context.setHead(T_context_swordclient_head);
                context.addItemXref(contextPath + "/swordclient?itemID="+item.getID(), T_swordclient_copy);
            }
        }

    }

}
