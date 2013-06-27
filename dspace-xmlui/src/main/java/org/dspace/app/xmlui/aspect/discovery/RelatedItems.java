/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.xml.sax.SAXException;

/**
 * Displays related items to the currently viewable item
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class RelatedItems extends AbstractDSpaceTransformer
{

    private static final Message T_head = message("xmlui.Discovery.RelatedItems.head");
    private static final Message T_related_help = message("xmlui.Discovery.RelatedItems.help");

    /**
     * Display items related to the given item
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException
    {

        DSpaceObject dspaceObject = HandleUtil.obtainHandle(objectModel);
        if (!(dspaceObject instanceof Item))
        {
            return;
        }
        Item item = (Item) dspaceObject;
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(item);

        if(discoveryConfiguration != null && discoveryConfiguration.getMoreLikeThisConfiguration() != null)
        {
            java.util.List<Item> relatedItems = SearchUtils.getSearchService().getRelatedItems(context, item, discoveryConfiguration.getMoreLikeThisConfiguration());
            if(CollectionUtils.isNotEmpty(relatedItems))
            {
                Division mltDiv = body.addDivision("item-related-container").addDivision("item-related", "secondary related");
                mltDiv.setHead(T_head);

                mltDiv.addPara(T_related_help);

                ReferenceSet set = mltDiv.addReferenceSet(
                        "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                        null, "related-items");

                for (DSpaceObject dso : relatedItems)
                {
                    set.addReference(dso);
                }
            }
        }
    }

    /**
     * Recycle
     */
    public void recycle() {
    	super.recycle();
    }
}
