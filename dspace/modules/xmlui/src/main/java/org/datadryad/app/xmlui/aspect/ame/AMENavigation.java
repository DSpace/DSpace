
package org.datadryad.app.xmlui.aspect.ame;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * Adds "Automatic metadata extraction" navigation context menu
 * 
 * @author craig.willis@unc.edu
 */
public class AMENavigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");
    private static final Message T_context_ame= message("xmlui.aspect.ame.AMENavigation.context");

    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        options.addList("browse");
        options.addList("account");

        List context = options.addList("context");

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	if (dso instanceof Item)
    	{
    		Item item = (Item) dso;
    		if (item.canEdit())
    		{
                if(AuthorizeConfiguration.authorizeManage(this.context,"extract-meta",item))
                {
                    try
                    {
                        AuthorizeManager.authorizeAction(this.context, item.getCollections()[0].getCommunities()[0], Constants.ADMIN);

                        context.setHead(T_context_head);
                        context.addItem().addXref(contextPath+"/item/ame?itemID="+item.getID(), T_context_ame);
                    }catch (AuthorizeException e)
                    {

                    }
                }
            }
    	}
    }

	public Serializable getKey() {
		return null;
	}


	public SourceValidity getValidity() {
		return null;
	}
}
