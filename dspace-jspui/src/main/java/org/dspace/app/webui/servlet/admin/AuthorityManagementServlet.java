/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.StringConfigurationComparator;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.content.authority.AuthorityInfo;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * 
 * @author bollini
 */
public class AuthorityManagementServlet extends DSpaceServlet
{
    public static final int AUTHORITY_KEYS_LIMIT = 20;

    /** log4j category */
    private static Logger log = Logger
            .getLogger(AuthorityManagementServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String issued = request.getParameter("issued");
        String authority = request.getParameter("authority");
        String authkey = request.getParameter("key");

        if (authority != null)
        {
            if (authkey != null)
            {
                doAuthorityKeyDetailsForAuthority(context, authority, authkey, request, response);
            }
            else
            {
                doAuthorityIssuedForAuthority(context, authority, request, response);
            }
        }
        else
        {
            if (authkey != null && issued != null)
            {
                doAuthorityKeyDetails(context, issued, authkey, request, response);
            }
            else if (issued != null)
            {
                doAuthorityIssued(context, issued, request, response);
            }
            else
            {
                doMainPage(context, request, response);
            }
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String key = request.getParameter("key");
        String issuedParam = request.getParameter("issued");
        String authority = request.getParameter("authority");
        
        List<String> metadataList;
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        if (authority != null)
        {
            metadataList = cam.getAuthorityMetadataForAuthority(authority);
        }
        else
        {
            metadataList = new LinkedList<String>();
            metadataList.add(issuedParam);
        }
        
        int[] items_uncertain = UIUtil.getIntParameters(request,
                "items_uncertain");
        int[] items_ambiguos = UIUtil.getIntParameters(request,
                "items_ambiguos");
        int[] items_novalue = UIUtil.getIntParameters(request, "items_novalue");
        int[] items_failed = UIUtil.getIntParameters(request, "items_failed");
        int[] items_unset = UIUtil.getIntParameters(request, "items_unset");
        int[] items_reject = UIUtil.getIntParameters(request, "items_reject");
        int[] items_notfound = UIUtil.getIntParameters(request, "items_notfound");
        
        int[][] items = { items_uncertain, items_ambiguos, items_notfound,
                items_failed, items_unset, items_reject, items_novalue };
        
        final String submitButton = UIUtil.getSubmitButton(request,
                "submit_accept");

        Set<Integer> itemRejectedIDs = new HashSet<Integer>();
        for (int[] items_uanfur : items)
        {
            if (items_uanfur!=null)
            {
                for (int itemID : items_uanfur)
                {
                    Item item = Item.find(context, itemID);
                    
                    for (String issued : metadataList)
                    {
                        String[] metadata = issued.split("\\.");
                        DCValue[] original = item.getMetadata(issued);
                        item.clearMetadata(metadata[0], metadata[1],
                                metadata.length > 2 ? metadata[2] : null, Item.ANY);
                        for (DCValue md : original)
                        {
                            if (key.equals(md.authority))
                            {
                                if ("submit_accept".equalsIgnoreCase(submitButton))
                                {
                                    log.debug(LogManager.getHeader(context,
                                            "confirm_authority_key", "item_id: "
                                                    + itemID + ", authority_key: "
                                                    + key));
                                    md.confidence = Choices.CF_ACCEPTED;
                                }
                                else
                                {
                                    log.debug(LogManager.getHeader(context,
                                            "reject_authority_key", "item_id: "
                                                    + itemID + ", authority_key: "
                                                    + key));
                                    md.confidence = Choices.CF_UNSET;
                                    md.authority = null;
                                    itemRejectedIDs.add(itemID);
                                }
                            }
                            item.addMetadata(md.schema, md.element, md.qualifier,
                                    md.language, md.value, md.authority,
                                    md.confidence);
                        }
                    }
                    item.update();
                }
            }
        }

        context.commit();
        if (itemRejectedIDs.size() > 0)
        {
            // notify reject
            int[] ids = new int[itemRejectedIDs.size()];
            Iterator<Integer> iter = itemRejectedIDs.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                ids[i] = (Integer) iter.next();
                i++;
            }
            
            String[] splitted = metadataList.get(0).split("\\.");
            String schema = splitted[0];
            String element = splitted[1];
            String qualifier = (splitted.length == 3)?splitted[2]:null;
            cam
                    .notifyReject(ids, schema, element, qualifier, key);
        }
        log.info(LogManager.getHeader(context, "validate_authority_key",
                "action: " + submitButton + " #items: " + items.length));
        String message = I18nUtil.getMessage(
                "org.dspace.app.webui.AuthorityManagementServlet."
                        + submitButton, UIUtil.getSessionLocale(request));
        request.getSession().setAttribute("authority.message", message);
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        
        if (authority != null)
        {
            long numIssuedItems = dao.countIssuedItemsByAuthorityValueInAuthority(authority, key);
            if (numIssuedItems > 0)
            {
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/authority?key="
                        + URLEncoder.encode(key, "UTF-8") + "&authority=" + authority);
            }
            else
            {
                long numIssuedKeys = dao.countIssuedAuthorityKeysByAuthority(authority);
                if (numIssuedKeys > 0)
                {
                    // search the next authority key to process...
                    String authkey = dao.findNextIssuedAuthorityKeyInAuthority(authority, key);
                    if (authkey == null)
                    { // there is no next... go back!
                        authkey = dao.findPreviousIssuedAuthorityKeyInAuthority(authority, key);
                    }
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority?" + "&authority=" + authority
                            + "&key=" + URLEncoder.encode(authkey, "UTF-8"));
                }
                else
                {
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority");
                }
            }
        }
        else
        {
            long numIssuedItems = dao.countIssuedItemsByAuthorityValue(metadataList.get(0), key);
            if (numIssuedItems > 0)
            {
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/authority?key="
                        + URLEncoder.encode(key, "UTF-8") + "&issued=" + issuedParam);
            }
            else
            {
                long numIssuedKeys = dao.countIssuedAuthorityKeys(issuedParam);
                if (numIssuedKeys > 0)
                {
                    // search the next authority key to process...
                    String authkey = dao.findNextIssuedAuthorityKey(issuedParam, key);
                    if (authkey == null)
                    { // there is no next... go back!
                        authkey = dao.findPreviousIssuedAuthorityKey(issuedParam, key);
                    }
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority?" + "&issued=" + issuedParam
                            + "&key=" + URLEncoder.encode(authkey, "UTF-8"));
                }
                else
                {
                    response.sendRedirect(request.getContextPath()
                            + "/dspace-admin/authority");
                }
            }
        }
    }

    private void doMainPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SQLException,
            ServletException, IOException
    {
        boolean detail = UIUtil.getBoolParameter(request, "detail");
        
        
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        Map<String, AuthorityInfo> infos = new HashMap<String, AuthorityInfo>();
        Comparator<String> configurationKeyComparator = new StringConfigurationComparator("authority.management.order.");
        
        if (detail)
        {
            MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
            List<String> authorityMetadata = new ArrayList<String>();
            List<String> tmpAuthorityMetadata = mam.getAuthorityMetadata();
            final String authManagementPrefix = "authority.management.";
            for(String tmp : tmpAuthorityMetadata) {
                boolean req = ConfigurationManager.getBooleanProperty(
                        authManagementPrefix + tmp, true);
                if(req) {
                    authorityMetadata.add(tmp);
                }
            }
            Collections.sort(authorityMetadata, configurationKeyComparator);
            
            request.setAttribute("authorities", authorityMetadata);
    
            for (String md : authorityMetadata)
            {
                AuthorityInfo info = dao.getAuthorityInfo(md);
                infos.put(md, info);
            }
            request.setAttribute("infos", infos);
    
            // add RP set # total item in HUB
            long numItems = DatabaseManager.querySingle(context,
                    "select count(*) as count from Item where in_archive = true")
                    .getLongColumn("count");
            request.setAttribute("numItems", numItems);
    
            log.info(LogManager.getHeader(context, "show_main_page",
                    "#authorities: " + authorityMetadata.size()));
            JSPManager.showJSP(request, response, "/dspace-admin/authority.jsp");//XXX
        }
        else
        {
            ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
            Set<String> authorityNames = cam.getAuthorities();
            List<String> listnames = new LinkedList<String>();
            
            for (String authorityName : authorityNames)
            {
                AuthorityInfo info = dao.getAuthorityInfoByAuthority(authorityName);
                infos.put(authorityName, info);
                listnames.add(authorityName);
            }
            
            Collections.sort(listnames, configurationKeyComparator);
            request.setAttribute("authorities", listnames);
            request.setAttribute("infos", infos);
    
            // add RP set # total item in HUB
            long numItems = DatabaseManager.querySingle(context,
                    "select count(*) as count from Item where in_archive = true")
                    .getLongColumn("count");
            request.setAttribute("numItems", numItems);
    
            log.info(LogManager.getHeader(context, "show_main_page",
                    "#authorities file: " + authorityNames.size()));
            JSPManager.showJSP(request, response, "/dspace-admin/authority.jsp");
        }
    }

    private void doAuthorityIssued(Context context, String issued,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException
    {
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        int page = UIUtil.getIntParameter(request, "page");
        if (page < 0)
        {
            page = 0;
        }
        List<String> keys = dao.listAuthorityKeyIssued(issued,
                AUTHORITY_KEYS_LIMIT, page);
        List<String[]> authoritiesIssued = new ArrayList<String[]>();// "authoritiesIssued"
        for (String key : keys)
        {
            authoritiesIssued.add(new String[] {
                    key,
                    cam.getLabel(issued.replaceAll("\\.", "_"), key, UIUtil
                            .getSessionLocale(request).toString()) });
        }
        request.setAttribute("authoritiesIssued", authoritiesIssued);
        request.setAttribute("totAuthoritiesIssued", Long.valueOf(dao
                .countIssuedAuthorityKeys(issued)));
        request.setAttribute("currPage", Integer.valueOf(page));
        log.info(LogManager.getHeader(context, "show_authority_issues",
                "metadata: " + issued + ", #keys: " + keys.size()));

        JSPManager.showJSP(request, response,
                "/dspace-admin/authority-issued.jsp");
    }

    private void doAuthorityKeyDetails(Context context, String issued,
            String authkey, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);

        // ItemIterator itemsIter = dao.findIssuedByAuthorityValue(issued,
        // authkey);
        ItemIterator itemsIter_cf_uncertain = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_UNCERTAIN);
        ItemIterator itemsIter_cf_ambiguos = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_AMBIGUOUS);
        ItemIterator itemsIter_cf_novalue = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_NOVALUE);
        ItemIterator itemsIter_cf_failed = dao
        .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                Choices.CF_FAILED);
        ItemIterator itemsIter_cf_notfound = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_NOTFOUND);
        ItemIterator itemsIter_cf_unset = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_UNSET);
        ItemIterator itemsIter_cf_reject = dao
                .findIssuedByAuthorityValueAndConfidence(issued, authkey,
                        Choices.CF_REJECTED);

        List<Item> items_uncertain = new ArrayList<Item>();
        while (itemsIter_cf_uncertain.hasNext())
        {
            Item item = itemsIter_cf_uncertain.next();
            items_uncertain.add(item);
        }
        Item[] arrItems_uncertain = new Item[items_uncertain.size()];
        arrItems_uncertain = items_uncertain.toArray(arrItems_uncertain);

        List<Item> items_ambiguos = new ArrayList<Item>();
        while (itemsIter_cf_ambiguos.hasNext())
        {
            Item item = itemsIter_cf_ambiguos.next();
            items_ambiguos.add(item);
        }
        Item[] arrItems_ambiguos = new Item[items_ambiguos.size()];
        arrItems_ambiguos = items_ambiguos.toArray(arrItems_ambiguos);

        List<Item> items_novalue = new ArrayList<Item>();
        while (itemsIter_cf_novalue.hasNext())
        {
            Item item = itemsIter_cf_novalue.next();
            items_novalue.add(item);
        }
        Item[] arrItems_novalue = new Item[items_novalue.size()];
        arrItems_novalue = items_novalue.toArray(arrItems_novalue);

        List<Item> items_failed = new ArrayList<Item>();
        while (itemsIter_cf_failed.hasNext())
        {
            Item item = itemsIter_cf_failed.next();
            items_failed.add(item);
        }
        Item[] arrItems_failed = new Item[items_failed.size()];
        arrItems_failed = items_failed.toArray(arrItems_failed);

        List<Item> items_notfound = new ArrayList<Item>();
        while (itemsIter_cf_notfound.hasNext())
        {
            Item item = itemsIter_cf_notfound.next();
            items_notfound.add(item);
        }
        Item[] arrItems_notfound = new Item[items_notfound.size()];
        arrItems_notfound = items_notfound.toArray(arrItems_notfound);
        
        List<Item> items_unset = new ArrayList<Item>();
        while (itemsIter_cf_unset.hasNext())
        {
            Item item = itemsIter_cf_unset.next();
            items_unset.add(item);
        }
        Item[] arrItems_unset = new Item[items_unset.size()];
        arrItems_unset = items_unset.toArray(arrItems_unset);

        List<Item> items_reject = new ArrayList<Item>();
        while (itemsIter_cf_reject.hasNext())
        {
            Item item = itemsIter_cf_reject.next();
            items_reject.add(item);
        }
        Item[] arrItems_reject = new Item[items_reject.size()];
        arrItems_reject = items_reject.toArray(arrItems_reject);

        String label = cam.getLabel(issued.replaceAll("\\.", "_"), authkey,
                UIUtil.getSessionLocale(request).toString());
        String[] md = issued.split("\\.");

        List<String> variants = cam.getVariants(md[0], md[1],
                md.length > 2 ? md[2] : null, authkey, UIUtil.getSessionLocale(
                        request).toString());

        String nextKey = dao.findNextIssuedAuthorityKey(issued, authkey);
        String prevKey = dao.findPreviousIssuedAuthorityKey(issued, authkey);

        request.setAttribute("items_uncertain", arrItems_uncertain);
        request.setAttribute("items_ambiguos", arrItems_ambiguos);
        request.setAttribute("items_novalue", arrItems_novalue);
        request.setAttribute("items_failed", arrItems_failed);
        request.setAttribute("items_notfound", arrItems_notfound);
        request.setAttribute("items_unset", arrItems_unset);
        request.setAttribute("items_reject", arrItems_reject);
        request.setAttribute("authKey", authkey);
        request.setAttribute("label", label);
        request.setAttribute("variants", variants);
        request.setAttribute("next", nextKey);
        request.setAttribute("previous", prevKey);
        request.setAttribute("required", mam.isAuthorityRequired(md[0], md[1],
                md.length > 2 ? md[2] : null));

        log.info(LogManager.getHeader(context, "show_key_issues", "metadata: "
                + issued + ", key: " + authkey + ", #items: "
                + arrItems_uncertain.length + arrItems_ambiguos.length
                + arrItems_novalue.length + arrItems_failed.length
                + arrItems_unset.length));

        JSPManager
                .showJSP(request, response, "/dspace-admin/authority-key.jsp");
    }
    
    private void doAuthorityKeyDetailsForAuthority(Context context,
            String authority, String authkey, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, AuthorizeException, IOException, ServletException
    {
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);

        // ItemIterator itemsIter = dao.findIssuedByAuthorityValue(issued,
        // authkey);
        ItemIterator itemsIter_cf_uncertain = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_UNCERTAIN);
        ItemIterator itemsIter_cf_ambiguos = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_AMBIGUOUS);
        ItemIterator itemsIter_cf_novalue = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_NOVALUE);
        ItemIterator itemsIter_cf_failed = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_FAILED);
        ItemIterator itemsIter_cf_notfound = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority,
                        authkey, Choices.CF_NOTFOUND);

        ItemIterator itemsIter_cf_unset = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_UNSET);
        ItemIterator itemsIter_cf_reject = dao
                .findIssuedByAuthorityValueAndConfidenceInAuthority(authority, authkey,
                        Choices.CF_REJECTED);

        List<Item> items_uncertain = new ArrayList<Item>();
        while (itemsIter_cf_uncertain.hasNext())
        {
            Item item = itemsIter_cf_uncertain.next();
            items_uncertain.add(item);
        }
        Item[] arrItems_uncertain = new Item[items_uncertain.size()];
        arrItems_uncertain = items_uncertain.toArray(arrItems_uncertain);

        List<Item> items_ambiguos = new ArrayList<Item>();
        while (itemsIter_cf_ambiguos.hasNext())
        {
            Item item = itemsIter_cf_ambiguos.next();
            items_ambiguos.add(item);
        }
        Item[] arrItems_ambiguos = new Item[items_ambiguos.size()];
        arrItems_ambiguos = items_ambiguos.toArray(arrItems_ambiguos);

        List<Item> items_novalue = new ArrayList<Item>();
        while (itemsIter_cf_novalue.hasNext())
        {
            Item item = itemsIter_cf_novalue.next();
            items_novalue.add(item);
        }
        Item[] arrItems_novalue = new Item[items_novalue.size()];
        arrItems_novalue = items_novalue.toArray(arrItems_novalue);

        List<Item> items_failed = new ArrayList<Item>();
        while (itemsIter_cf_failed.hasNext())
        {
            Item item = itemsIter_cf_failed.next();
            items_failed.add(item);
        }
        Item[] arrItems_failed = new Item[items_failed.size()];
        arrItems_failed = items_failed.toArray(arrItems_failed);

        List<Item> items_notfound = new ArrayList<Item>();
        while (itemsIter_cf_notfound.hasNext())
        {
            Item item = itemsIter_cf_notfound.next();
            items_notfound.add(item);
        }
        Item[] arrItems_notfound = new Item[items_notfound.size()];
        arrItems_notfound = items_notfound.toArray(arrItems_notfound);
        
        
        List<Item> items_unset = new ArrayList<Item>();
        while (itemsIter_cf_unset.hasNext())
        {
            Item item = itemsIter_cf_unset.next();
            items_unset.add(item);
        }
        Item[] arrItems_unset = new Item[items_unset.size()];
        arrItems_unset = items_unset.toArray(arrItems_unset);

        List<Item> items_reject = new ArrayList<Item>();
        while (itemsIter_cf_reject.hasNext())
        {
            Item item = itemsIter_cf_reject.next();
            items_reject.add(item);
        }
        Item[] arrItems_reject = new Item[items_reject.size()];
        arrItems_reject = items_reject.toArray(arrItems_reject);

        String label = cam.getLabel(cam.getAuthorityMetadataForAuthority(authority).get(0).replaceAll("\\.", "_"), authkey,
                UIUtil.getSessionLocale(request).toString());
        String[] md = cam.getAuthorityMetadataForAuthority(authority).get(0).split("\\.");

        List<String> variants = cam.getVariants(md[0], md[1],
                md.length > 2 ? md[2] : null, authkey, UIUtil.getSessionLocale(
                        request).toString());

        String nextKey = dao.findNextIssuedAuthorityKeyInAuthority(authority, authkey);
        String prevKey = dao.findPreviousIssuedAuthorityKeyInAuthority(authority, authkey);

        request.setAttribute("items_uncertain", arrItems_uncertain);
        request.setAttribute("items_ambiguos", arrItems_ambiguos);
        request.setAttribute("items_novalue", arrItems_novalue);
        request.setAttribute("items_failed", arrItems_failed);
        request.setAttribute("items_notfound", arrItems_notfound);
        request.setAttribute("items_unset", arrItems_unset);
        request.setAttribute("items_reject", arrItems_reject);
        request.setAttribute("authKey", authkey);
        request.setAttribute("label", label);
        request.setAttribute("variants", variants);
        request.setAttribute("next", nextKey);
        request.setAttribute("previous", prevKey);
        request.setAttribute("required", mam.isAuthorityRequired(md[0], md[1],
                md.length > 2 ? md[2] : null));

        log.info(LogManager.getHeader(context, "show_key_issues", "authority: "
                + authority + ", key: " + authkey + ", #items: "
                + arrItems_uncertain.length + arrItems_ambiguos.length
                + arrItems_novalue.length + arrItems_failed.length
                + arrItems_unset.length));

        JSPManager
                .showJSP(request, response, "/dspace-admin/authority-key.jsp");
    }

    private void doAuthorityIssuedForAuthority(Context context,
            String authority, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, ServletException, IOException
    {
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        int page = UIUtil.getIntParameter(request, "page");
        if (page < 0)
        {
            page = 0;
        }
        List<String> keys = dao.listAuthorityKeyIssuedByAuthority(authority,
                AUTHORITY_KEYS_LIMIT, page);
        List<String[]> authoritiesIssued = new ArrayList<String[]>();// "authoritiesIssued"
        for (String key : keys)
        {
            authoritiesIssued.add(new String[] {
                    key,
                    cam.getLabel(cam.getAuthorityMetadataForAuthority(authority).get(0).replaceAll("\\.", "_"), key, UIUtil
                            .getSessionLocale(request).toString()) });
        }
        request.setAttribute("authoritiesIssued", authoritiesIssued);
        request.setAttribute("totAuthoritiesIssued", Long.valueOf(dao
                .countIssuedAuthorityKeysByAuthority(authority)));
        request.setAttribute("currPage", Integer.valueOf(page));
        log.info(LogManager.getHeader(context, "show_authority_issues",
                "authority: " + authority + ", #keys: " + keys.size()));

        JSPManager.showJSP(request, response,
                "/dspace-admin/authority-issued.jsp");
        
    }
}
