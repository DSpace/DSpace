/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.eperson.Group;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Navigation Class that support Navigation Options for Shopping Cart User Interface
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Message T_administrative_head 				= message("xmlui.administrative.Navigation.administrative_head");

    private static final Message T_administrative_access_shoppingcart 	= message("xmlui.administrative.Navigation.administrative_access_shoppingcart");

    private static final Message T_administrative_shoppingcart 			= message("xmlui.administrative.Navigation.administrative_shoppingcart");
    private static final Message T_administrative_voucher 			= message("xmlui.administrative.Navigation.administrative_voucher");

    private static final Message T_my_account                       = message("xmlui.EPerson.Navigation.my_account");

    private static final Message T_administrative_edit_paymentsystem  =   message("xmlui.EPerson.Navigation.paymentsystem");
    /** Cached validity object */
    private SourceValidity validity;

    /** exports available for download */
    java.util.List<String> availableExports = null;

    /**
     * Generate the unique cache key.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey()
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        // Special case, don't cache anything if the user is logging
        // in. The problem occures because of timming, this cache key
        // is generated before we know whether the operation has
        // succeeded or failed. So we don't know whether to cache this
        // under the user's specific cache or under the anonymous user.
        if (request.getParameter("login_email")    != null ||
                request.getParameter("login_password") != null ||
                request.getParameter("login_realm")    != null )
        {
            return "0";
        }

        if (context.getCurrentUser() == null)
        {
            return HashUtil.hash("anonymous");
        }

        if (availableExports != null && availableExports.size()>0) {
            StringBuilder key = new StringBuilder(context.getCurrentUser().getEmail());
            for(String fileName : availableExports){
                key.append(":").append(fileName);
            }

            return HashUtil.hash(key.toString());
        }

        return HashUtil.hash(context.getCurrentUser().getEmail());
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
        {
            // Only use the DSpaceValidity object is someone is logged in.
            if (context.getCurrentUser() != null)
            {
                try {
                    DSpaceValidity validity = new DSpaceValidity();

                    validity.add(eperson);

                    Group[] groups = Group.allMemberGroups(context, eperson);
                    for (Group group : groups)
                    {
                        validity.add(group);
                    }

                    this.validity = validity.complete();
                }
                catch (SQLException sqle)
                {
                    // Just ignore it and return invalid.
                }
            }
            else
            {
                this.validity = NOPValidity.SHARED_INSTANCE;
            }
        }
        return this.validity;
    }


    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        try{
            availableExports = ItemExport.getExportsAvailable(context.getCurrentUser());
        }
        catch (Exception e) {
            // nothing to do
        }
    }




    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        /* Create skeleton menu structure to ensure consistent order between aspects,
           * even if they are never used
           */
        options.addList("browse");
        List account = options.addList("account");
        List context = options.addList("context");
        List admin = options.addList("administrative");
        account.setHead(T_my_account);


        //Check if a system administrator
        boolean isSystemAdmin = AuthorizeManager.isCuratorOrAdmin(this.context);


        if(isSystemAdmin){
            addContextualOptions(context);
        }
        // System Administrator options!
        if (isSystemAdmin)
        {
            admin.setHead(T_administrative_head);
            List property = admin.addList("properties");
            property.setHead(T_administrative_access_shoppingcart);
            property.addItemXref(contextPath+"/admin/shoppingcart", T_administrative_shoppingcart);
            property.addItemXref(contextPath+"/admin/voucher", T_administrative_voucher);
        }
    }


    public int addContextualOptions(List context) throws SQLException, WingException
    {
        // How many options were added.
        int options = 0;

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        if(dso instanceof Item)
        {
            Item site = (Item) dso;
            boolean isSystemAdmin = AuthorizeManager.isAdmin(this.context);
        }

        return options;
    }


    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }
}
