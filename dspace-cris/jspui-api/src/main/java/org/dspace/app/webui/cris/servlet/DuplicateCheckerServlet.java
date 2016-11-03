/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.cris.deduplication.service.DedupService;
import org.dspace.app.cris.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.cris.deduplication.utils.DedupUtils;
import org.dspace.app.cris.deduplication.utils.DuplicateInfo;
import org.dspace.app.cris.deduplication.utils.DuplicateInfoList;
import org.dspace.app.util.DCInput;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

/**
 * 
 * Servlet to choose and merge duplicates.
 * 
 * @author pascarelli
 * 
 */
public class DuplicateCheckerServlet extends DSpaceServlet
{

    /** Logger */
    private static Logger log = Logger.getLogger(DuplicateCheckerServlet.class);

    private List<String> blockedTypes = new ArrayList<String>();

    private Map<String, Integer> submitOptionMap = new LinkedHashMap<String, Integer>();

    private final static String optionsString[] = { "submit", "submitcheck",
            "submittargetchoice", "submitpreview", "submitunrelatedall",
            "submitmerge" };

    private final static int ALL_DUPLICATES = 0;

    private final static int UNCHECKED_OR_TOFIX_DUPLICATES = 1;

    private final static int TOFIX_DUPLICATES = 2;

    private final static int COUNT_DUPLICATES = 0;

    private final static int SHOW_DUPLICATES = 1;

    private final static int SELECT_TARGET = 2;

    private final static int MANAGE_PREVIEW = 3;

    private final static int REJECT = 4;

    private final static int MERGE = 5;

    private final static int optionsInt[] = { COUNT_DUPLICATES, SHOW_DUPLICATES,
            SELECT_TARGET, MANAGE_PREVIEW, REJECT, MERGE };

    private DedupUtils dedupUtils = new DSpace().getServiceManager()
            .getServiceByName("dedupUtils", DedupUtils.class);

    /**
     * Load blocked metadata configuration.
     */
    public DuplicateCheckerServlet()
    {
        String property = ConfigurationManager.getProperty("misc",
                "tool.duplicatechecker.blocked");
        if (property != null)
        {
            String[] typesConf = property.split(",");
            for (String type : typesConf)
            {
                blockedTypes.add(type.trim());
            }
        }

        int count = 0;
        for (String opt : optionsString)
        {
            submitOptionMap.put(opt, optionsInt[count]);
            count++;
        }
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
                    SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "cleaner", "cleaner servlet"));
        doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
                    SQLException, AuthorizeException
    {

        String option = UIUtil.getSubmitButton(request, "submit");
        String signatureType = request.getParameter("signatureType");
        int scope = UIUtil.getIntParameter(request, "scope");
        boolean mergeByUser = UIUtil.getBoolParameter(request, "mergeByUser");
        if (scope == -1)
            scope = ALL_DUPLICATES;

        int resourceType = UIUtil.getIntParameter(request, "resourceType");
        if (resourceType == -1)
            resourceType = Constants.ITEM;

        int targetDefault = -1;
        int optionInt = submitOptionMap.get(option);
        
        // if suggested duplicates go to SHOW_DUPLICATES
        if (scope == TOFIX_DUPLICATES)
        {
            optionInt = SHOW_DUPLICATES;
        }
        
        // try to get JSP for item choices or preview merge
        switch (optionInt)
        {
        case COUNT_DUPLICATES:
        {
                // count for each group how many duplicates are there
                Map<String, Integer> duplicatesAll = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyWorspace = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyWorkflow = new LinkedHashMap<String, Integer>();
                Map<String, Integer> duplicatesOnlyReported = new LinkedHashMap<String, Integer>();

                try
                {
                    duplicatesAll = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED, resourceType);
                    duplicatesOnlyWorspace = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFY, resourceType);
                    duplicatesOnlyWorkflow = dedupUtils.countSignaturesWithDuplicates(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED_OR_VERIFYWF, resourceType);
                    duplicatesOnlyReported = dedupUtils.countSuggestedDuplicate(SolrDedupServiceImpl.SUBQUERY_NOT_IN_REJECTED, resourceType);
                }
                catch (SearchServiceException e)
                {
                    throw new ServletException(e);
                }

                request.setAttribute("scope", scope);
                request.setAttribute("duplicatessignatureall", duplicatesAll);
                request.setAttribute("duplicatessignatureonlyws", duplicatesOnlyWorspace);
                request.setAttribute("duplicatessignatureonlywf", duplicatesOnlyWorkflow);
                request.setAttribute("duplicatessignatureonlyreported", duplicatesOnlyReported);
                JSPManager.showJSP(request, response,
                        "/deduplication/cleanerview-choose.jsp");
                return;
        }
        case SHOW_DUPLICATES:
        {
            // After loaded grid to display logic here tries discover if target
            // must be choice or not.

            int rule = UIUtil.getIntParameter(request, "submitcheck");
            if (rule == -1)
            {
                rule = UIUtil.getIntParameter(request, "rule");
            }
            if (rule == -1)
            {
                rule = 0;
            }
            int rows = UIUtil.getIntParameter(request, "rows");
            int start = UIUtil.getIntParameter(request, "start");
            if (rows == -1)
            {
                rows = 50;
            }
            if (start == -1)
            {
                start = 0;
            }
            // parent grid is a container where key is the signature description
            // which we want to grouped item duplicates
            Map<String, Map<Integer, String[]>> gridParent = new LinkedHashMap<String, Map<Integer, String[]>>();
            Map<String, List<String>> gridTwiceGroups = new LinkedHashMap<String, List<String>>();

            long count = 0;
            // List<DuplicateInfo> duplicateCouple = null;
            Map<Integer, DSpaceObject> extraInfo = new LinkedHashMap<Integer, DSpaceObject>();
            Map<Integer, String> itemTypeInfo = new LinkedHashMap<Integer, String>();
            String idsListString = request.getParameter("itemid_list");
            List<DSpaceObject> itemList = new ArrayList<DSpaceObject>();

            if (StringUtils.isNotBlank(idsListString))
            {
                String ids[] = idsListString.split(",");
                count = ids.length;
                for (int j = 0; j < ids.length; j++)
                {
                    int currentId = 0;
                    try
                    {
                        currentId = Integer.parseInt(ids[j].trim());
                    }
                    catch (Exception e)
                    {
                        log.error(e.getMessage(), e);
                        count--;
                        continue;
                    }
                    DSpaceObject current = DSpaceObject.find(context, resourceType, currentId);
                    if ((current == null) || (current.isWithdrawn()))
                    {
                        count--;
                        continue;
                    }
                    itemList.add(current);
                }

                Map<Integer, String[]> grid = new LinkedHashMap<Integer, String[]>();
                putItemsOnGrid(context, grid, extraInfo, itemList, request);
                gridParent.put("items", grid);
            }
            else
            {
                try
                {
                    DuplicateInfoList dil = null;
                    if (scope == TOFIX_DUPLICATES) {
                        dil = dedupUtils
                            .findSuggestedDuplicate(context, resourceType, start, rows);
                    }
                    else {
                        dil = dedupUtils
                                .findSignatureWithDuplicate(context, signatureType,
                                        resourceType, start, rows, rule);
                    }
                    for (DuplicateInfo info : dil.getDsi())
                    {
                        boolean found = false;
                        String keyChecked = "";
                        for(String check : info.getOtherSignature()) {
                            if(gridParent.containsKey(check)) {                                
                                found = true;
                                keyChecked = check;
                                break;
                            }
                        }
                        if(!found) {
                            Map<Integer, String[]> grid = new LinkedHashMap<Integer, String[]>();
                            putItemsOnGrid(context, grid, extraInfo,
                                    info.getItems(), request);
                            gridParent.put(info.getSignature(), grid);
                            gridTwiceGroups.put(info.getSignature(), new ArrayList<String>());
                        }
                        else {
                            gridTwiceGroups.get(keyChecked).add(info.getSignature());
                        }
                    }
                    count = dil.getSize();
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), e);
                }
            }

            Iterator<DSpaceObject> it = extraInfo.values().iterator();
            while (it.hasNext())
            {
                DSpaceObject current = it.next();
                if (resourceType == Constants.ITEM)
                {
                    String aliasForm = ItemUtils.getDCInputSet((Item) current)
                            .getFormName();
                    if (StringUtils.isNotBlank(aliasForm))
                    {
                        itemTypeInfo.put(current.getID(), aliasForm);
                    }
                }
            }

            request.setAttribute("extraInfo", extraInfo);
            request.setAttribute("itemTypeInfo", itemTypeInfo);
            request.setAttribute("signatureType", signatureType);
            request.setAttribute("grid", gridParent);
            request.setAttribute("gridTwiceGroups", gridTwiceGroups);            
            request.setAttribute("start", start);
            request.setAttribute("scope", scope);
            request.setAttribute("rows", rows);
            request.setAttribute("count", count);
            request.setAttribute("rule", rule);
            request.setAttribute("mergeByUser", mergeByUser);
            JSPManager.showJSP(request, response,
                    "/deduplication/cleanerview-check.jsp");
            return;
        }
        case SELECT_TARGET:
        {
            // choose target or go to merge
            int[] items = UIUtil.getIntParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items == null || items.length < 2)
            {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submitcheck=" + rule + "&scope="
                        + scope);
                return;
            }

            Map<Integer, String[]> grid = new LinkedHashMap<Integer, String[]>();
            Map<Integer, DSpaceObject> extraInfo = new LinkedHashMap<Integer, DSpaceObject>();

            putItemsOnGrid(context, grid, extraInfo,
                    getDSpaceObjects(context, items, resourceType), request);
            int oldestId = -1;
            for (DSpaceObject item : getDSpaceObjects(context, items,
                    resourceType))
            {
                if (((Item) item).isArchived())
                {
                    if (targetDefault == -1)
                    {
                        targetDefault = item.getID();
                    }
                    else
                    {
                        if (item.getID() < targetDefault)
                        {
                            targetDefault = item.getID();
                        }
                    }
                }
                if (targetDefault == -1)
                {
                    if (oldestId == -1)
                    {
                        oldestId = item.getID();
                    }
                    if (item.getID() < oldestId)
                    {
                        oldestId = item.getID();
                    }
                }
            }
            if (targetDefault == -1)
            {
                targetDefault = oldestId;
            }
            request.setAttribute("rule", rule);
            request.setAttribute("scope", scope);
            optionInt = MANAGE_PREVIEW;
        }
        case MANAGE_PREVIEW:
        {
            // manage preview
            int[] items = UIUtil.getIntParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items != null && items.length == 1)
            {
                String itemstomerge = "";
                for (int i : items)
                {
                    itemstomerge += "&itemstomerge=" + i;
                }
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submittargetchoice&scope=" + scope
                        + "&rule=" + rule + itemstomerge);
                return;
            }

            int target = UIUtil.getIntParameter(request, "target");

            if (target == -1)
            {
                target = targetDefault;
            }

            // Retrieve styles
            String propertyStyles = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.styles");
            String propertyTargetStyle = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.targetStyle");
            String propertyDefaultStyle = ConfigurationManager
                    .getProperty("deduplication", "plugin.bootstrap.defaultStyle");
            String[] styles = { "warning,danger" };
            
            String[] configurationStyles = null;
            if(StringUtils.isNotEmpty(propertyStyles)) {
                configurationStyles = propertyStyles.split(",");
            }

            // Checking configuration
            if (ArrayUtils.isNotEmpty(configurationStyles))
            {
                log.info("INFO: Applying default style " + styles
                        + "  You can overwrite \"plugin.bootstrap.styles\" in dspace.cfg");
                styles = configurationStyles;
            }
            if (StringUtils.isEmpty(propertyTargetStyle))
            {
                log.info(
                        "INFO: Applying taget style \" label label-success\"  You can overwrite \"plugin.bootstrap.targetStyle\" in dspace.cfg");
                propertyTargetStyle = "success";
            }
            if (StringUtils.isEmpty(propertyDefaultStyle))
            {
                log.info(
                        "INFO: Applying taget style \" label label-info\"  You can overwrite \"plugin.bootstrap.defaultStyle\" in dspace.cfg");
                propertyDefaultStyle = "info";
            }

            HashMap<Integer, String> legenda = new HashMap<Integer, String>();
            int k = 0;
            for (int i : items)
            {
                if (i == target)
                    legenda.put(i, propertyTargetStyle);
                else if (k < styles.length)
                    legenda.put(i, styles[k]);
                else
                    legenda.put(i, propertyDefaultStyle);
                k++;
            }

            Map<Integer, String> citations = new HashMap<Integer, String>();
            for (int item : items)
            {
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
                        .getNamedPlugin(StreamDisseminationCrosswalk.class,
                                ConfigurationManager.getProperty("deduplication",
                                        "tool.duplicatechecker.citation"));
                try
                {
                    streamCrosswalkDefault.disseminate(context,
                            Item.find(context, item), output);
                    citations.put(item, output.toString("UTF-8"));
                }
                catch (CrosswalkException e)
                {
                    log.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            }

            // All DC types in the registry
            MetadataField[] types = MetadataField.findAll(context);

            // Get a HashMap of metadata field ids and a field name to display
            HashMap<Integer, String> metadataFields = new HashMap<Integer, String>();

            // Get all existing Schemas
            MetadataSchema[] schemas = MetadataSchema.findAll(context);
            for (int i = 0; i < schemas.length; i++)
            {
                String schemaName = schemas[i].getName();
                // Get all fields for the given schema
                MetadataField[] fields = MetadataField.findAllInSchema(context,
                        schemas[i].getSchemaID());
                for (int j = 0; j < fields.length; j++)
                {
                    Integer fieldID = new Integer(fields[j].getFieldID());
                    String displayName = "";
                    displayName = schemaName + "." + fields[j].getElement()
                            + (fields[j].getQualifier() == null ? ""
                                    : "." + fields[j].getQualifier());
                    metadataFields.put(fieldID, displayName);
                }
            }

            // load target and fill object with other metadata
            Item targetItem = loadTarget(context, request, target, items, types,
                    metadataFields);
            List<Item> otherItems = getItems(context, items);

            Map<Collection, Boolean[]> collections = getCollections(context,
                    targetItem, otherItems, request);

            request.setAttribute("collections", collections);
            request.setAttribute("items", otherItems);
            request.setAttribute("target", targetItem);
            request.setAttribute("blockedMetadata", getBlockedTypes());
            request.setAttribute("dcTypes", types);
            request.setAttribute("metadataFields", metadataFields);
            request.setAttribute("legenda", legenda);
            request.setAttribute("citations", citations);
            request.setAttribute("rule", rule);
            request.setAttribute("scope", scope);
            request.setAttribute("signatureType", signatureType);
            request.setAttribute("mergeByUser", mergeByUser);
            
            JSPManager.showJSP(request, response,
                    "/deduplication/cleanerview-preview.jsp");
            break;
        }
        case REJECT:
        {
            // unrelate item
            int[] items = UIUtil.getIntParameters(request, "itemstomerge");
            int rule = UIUtil.getIntParameter(request, "rule");
            if (items == null)
            {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate?submitcheck&scope=" + scope
                        + "&rule=" + rule);
                break;
            }

            for (int itemId : items)
            {
                for (int itemId2 : items)
                {
                    if (itemId2 == itemId)
                    {
                        continue;
                    }
                    dedupUtils.rejectAdminDups(context, itemId, itemId2, resourceType);
                }
            }
            // Complete transaction
            context.complete();
            dedupUtils.commit();

            if(mergeByUser) {
                response.sendRedirect(request.getContextPath()
                        + "/mydspace");
            }
            else {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate");                
            }
            
            break;
        }
        case MERGE:
        {
            // commit works
            Item item = Item.find(context,
                    UIUtil.getIntParameter(request, "item_id"));

            int collectionOwner = UIUtil.getIntParameter(request,
                    "collectionOwner");
            int[] otherCollections = UIUtil.getIntParameters(request,
                    "collectionOthers");

            int[] toRemove = UIUtil.getIntParameters(request, "itemremove_id");

            int rule = UIUtil.getIntParameter(request, "rule");

            Collection collection = null;
            if (collectionOwner != -1)
            {
                collection = getCollections(context,
                        new int[] { collectionOwner }, -1).get(0);
            }
            List<Collection> collections = new LinkedList<Collection>();
            if (otherCollections != null && otherCollections.length > 0)
            {
                collections = getCollections(context, otherCollections,
                        collectionOwner);
            }
            processUpdateItem(context, request, response, item, collection,
                    collections);

            for (int remove : toRemove)
            {
                if (remove != item.getID())
                {
                    Item itemRemove = Item.find(context, remove);
                    if (itemRemove.isArchived() || itemRemove.isWithdrawn())
                    {
                        // add metadata replaced and go to withdrawn
                        itemRemove.addMetadata(MetadataSchema.DC_SCHEMA,
                                "relation", "isreplacedby", null,
                                "hdl:" + item.getHandle());

                        remove(context, request, itemRemove);

                        dedupUtils.rejectAdminDups(context, item.getID(),
                                remove, resourceType);
                        for (int other : toRemove)
                        {
                            if (other != itemRemove.getID())
                            {
                                dedupUtils.rejectAdminDups(context,
                                        itemRemove.getID(), other, resourceType);
                            }
                        }
                        // reject all other duplicates as fake
                        if ((scope != TOFIX_DUPLICATES) && (rule != -1))
                        {
                            try
                            {
                                dedupUtils.rejectAdminDups(context,
                                        itemRemove.getID(), signatureType,
                                        itemRemove.getType());
                            }
                            catch (SearchServiceException e)
                            {
                                throw new ServletException(e.getMessage(), e);
                            }
                        }
                        itemRemove.update();
                    }
                    else
                    {

                        remove(context, request, itemRemove);

                        DatabaseManager.updateQuery(context,
                                "DELETE FROM dedup_reject WHERE first_item_id = ? OR second_item_id = ?",
                                remove, remove);
                    }
                }
            }
            if ((scope != TOFIX_DUPLICATES) && (rule != -1))
            {
                try
                {
                    dedupUtils.rejectAdminDups(context, item.getID(),
                            signatureType, item.getType());
                }
                catch (SearchServiceException e)
                {
                    throw new ServletException(e.getMessage(), e);
                }
            }
            // Complete transaction
            context.complete();
            dedupUtils.commit();
            
            if(mergeByUser) {
                response.sendRedirect(request.getContextPath()
                        + "/mydspace");
            }
            else {
                response.sendRedirect(request.getContextPath()
                        + "/tools/duplicate");                
            }
        }
        default:
            // none operations
            break;
        }

    }

    private void remove(Context context, HttpServletRequest request,
            Item itemRemove)
                    throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(context, "merge_remove_item",
                "item_id=" + itemRemove.getID()));
        Integer status = ItemUtils.getItemStatus(context, itemRemove);
        switch (status)
        {
        case ItemUtils.ARCHIVE:
            itemRemove.withdraw();
            break;
        case ItemUtils.WORKFLOW:
            WorkflowItem wfi = WorkflowItem.findByItem(context, itemRemove);
            Collection collectionParent = ((Collection) itemRemove.getParentObject());
            if(collectionParent!=null) { 
                collectionParent.removeItem(itemRemove);
            }
            wfi.deleteWrapper(); 
            itemRemove.delete();
            break;
        case ItemUtils.WORKSPACE:
            WorkspaceItem wsi = WorkspaceItem.findByItem(context, itemRemove);
            wsi.deleteAll();
            break;
        default:
            break;
        }
    }

    private Map<Collection, Boolean[]> getCollections(Context context,
            Item targetItem, List<Item> otherItems, HttpServletRequest request)
                    throws SQLException
    {
        Map<Collection, Boolean[]> result = new LinkedHashMap<Collection, Boolean[]>();
        for (Collection coll : targetItem.getCollections())
        {
            result.put(coll, new Boolean[] { false, true });
        }
        if (targetItem.isArchived())
        {
            result.put(targetItem.getOwningCollection(),
                    new Boolean[] { true, false });
        }
        else
        {
            request.setAttribute("noowningcollection", true); // show only
                                                              // radiobutton
                                                              // to choose
                                                              // inprogresssubmission
                                                              // collection

            TableRow trWsi = DatabaseManager.findByUnique(context,
                    "workspaceitem", "item_id", targetItem.getID());
            if (trWsi == null)
            { // if item not in workspace then
              // check if item
              // is
              // in workflow state
                TableRow trWfi = DatabaseManager.findByUnique(context,
                        "workflowitem", "item_id", targetItem.getID());
                if (trWfi != null)
                {
                    Integer idWfi = trWfi.getIntColumn("workflow_id");
                    WorkflowItem wfi = WorkflowItem.find(context, idWfi);
                    if (!result.containsKey(wfi.getCollection()))
                    {
                        result.put(wfi.getCollection(),
                                new Boolean[] { false, true });
                    }

                }
            }
            else
            {

                Integer idWsi = trWsi.getIntColumn("workspace_item_id");
                WorkspaceItem wsi = WorkspaceItem.find(context, idWsi);
                if (!result.containsKey(wsi.getCollection()))
                {
                    result.put(wsi.getCollection(),
                            new Boolean[] { false, true });
                }

            }
        }

        for (Item other : otherItems)
        {
            for (Collection coll : other.getCollections())
            {
                if (!result.containsKey(coll))
                {
                    result.put(coll, new Boolean[] { false, false });
                }
            }
            if (other.isArchived())
            {
                if (!result.containsKey(other.getOwningCollection()))
                {
                    result.put(other.getOwningCollection(),
                            new Boolean[] { false, false });
                }
            }
            else
            {

                TableRow trWsi = DatabaseManager.findByUnique(context,
                        "workspaceitem", "item_id", other.getID());
                if (trWsi == null)
                { // if item not in workspace then
                  // check if item
                  // is
                  // in workflow state
                    TableRow trWfi = DatabaseManager.findByUnique(context,
                            "workflowitem", "item_id", other.getID());
                    if (trWfi != null)
                    {
                        Integer idWfi = trWfi.getIntColumn("workflow_id");
                        WorkflowItem wfi = WorkflowItem.find(context, idWfi);
                        if (!result.containsKey(wfi.getCollection()))
                        {
                            result.put(wfi.getCollection(),
                                    new Boolean[] { false, false });
                        }

                    }
                }
                else
                {

                    Integer idWsi = trWsi.getIntColumn("workspace_item_id");
                    WorkspaceItem wsi = WorkspaceItem.find(context, idWsi);
                    if (!result.containsKey(wsi.getCollection()))
                    {
                        result.put(wsi.getCollection(),
                                new Boolean[] { false, false });
                    }

                }
            }

        }
        return result;
    }

    /**
     * Read from configuration to load blocked metadata
     * 
     * @return
     */
    private List<String> getBlockedTypes()
    {
        return blockedTypes;
    }

    /**
     * Fill target item with new metadata, manages bundles "ORIGINAL" and
     * organize attribute to render on view.
     * 
     * @param context
     * @param request
     * @param target
     * @param items
     * @param fields
     * @param metadataFields
     * @return
     * @throws SQLException
     */
    private Item loadTarget(Context context, HttpServletRequest request,
            int target, int[] items, MetadataField[] fields,
            HashMap<Integer, String> metadataFields) throws SQLException
    {
        // get items
        Item item = Item.find(context, target);

        List<Item> others = getItems(context, items);
        // This map contains items which are owners of metadata (metadata
        // formkey -> owner item ID)
        Map<String, Integer> metadataSourceInfo = new HashMap<String, Integer>();
        // This map contains a field which correspond to the various types of
        // metadata (MetatadaField ID -> List of DTO's DCValues)
        Map<Integer, List<DTODCValue>> metadataExtraSourceInfo = new HashMap<Integer, List<DTODCValue>>();
        // List of all metadata
        List<DTODCValue> dtodcvalues = new LinkedList<DTODCValue>();

        Map<Integer, DCInput> dcinputs = new HashMap<Integer, DCInput>();

        // fill other metadata on target object
        outer: for (int i = 0; i < fields.length; i++)
        {
            Integer fieldID = new Integer(fields[i].getFieldID());
            List<DTODCValue> dtodcvalue = new LinkedList<DTODCValue>();

            String mdString = metadataFields.get(fieldID);
            Metadatum[] value = item.getMetadataValueInDCFormat(mdString);
            if (value != null && value.length > 0)
            {
                if (dcinputs.get(fieldID) == null)
                {
                    try
                    {
                        dcinputs.put(fieldID,
                                ItemUtils.getDCInput(MetadataSchema.DC_SCHEMA,
                                        fields[i].getElement(),
                                        fields[i].getQualifier(),
                                        ItemUtils.getDCInputSet(item)));
                    }
                    catch (Exception e)
                    {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (value == null || value.length == 0)
            {
                // get from the first match
                inner: for (Item other : others)
                {

                    Metadatum[] valueOther = other
                            .getMetadataValueInDCFormat(mdString);
                    if (valueOther == null || valueOther.length == 0)
                    {
                        continue inner;
                    }
                    else
                    {
                        for (Metadatum v : valueOther)
                        {
                            item.addMetadata(v.schema, v.element, v.qualifier,
                                    v.language, v.value);
                            createDTODCValue(fieldID, dtodcvalue, other, v,
                                    false, mdString);
                        }
                        metadataSourceInfo.put(mdString.replaceAll("\\.", "_"),
                                other.getID());
                        if (dcinputs.get(fieldID) == null)
                        {
                            try
                            {
                                dcinputs.put(fieldID, ItemUtils.getDCInput(
                                        MetadataSchema.DC_SCHEMA,
                                        fields[i].getElement(),
                                        fields[i].getQualifier(),
                                        ItemUtils.getDCInputSet(other)));
                            }
                            catch (Exception e)
                            {
                                log.error(e.getMessage(), e);
                            }
                        }
                        break inner;
                    }

                }
            }
            else
            {
                metadataSourceInfo.put(mdString.replaceAll("\\.", "_"),
                        item.getID());
                for (Metadatum v : value)
                {
                    createDTODCValue(fieldID, dtodcvalue, item, v, false,
                            mdString);
                }
                inner: for (Item other : others)
                {
                    if (other.getID() != item.getID())
                    {
                        Metadatum[] valueOther = other
                                .getMetadataByMetadataString(mdString);
                        if (valueOther == null || valueOther.length == 0)
                        {
                            continue inner;
                        }
                        else
                        {
                            for (Metadatum v : valueOther)
                            {

                                boolean removed = checkContentEquality(value,
                                        v);

                                item.addMetadata(v.schema, v.element,
                                        v.qualifier, v.language, v.value,
                                        v.authority, v.confidence);
                                createDTODCValue(fieldID, dtodcvalue, other, v,
                                        true, removed, mdString);

                            }
                            if (dcinputs.get(fieldID) == null)
                            {
                                try
                                {
                                    dcinputs.put(fieldID,
                                            ItemUtils.getDCInput(
                                                    MetadataSchema.DC_SCHEMA,
                                                    fields[i].getElement(),
                                                    fields[i]
                                                            .getQualifier(),
                                            ItemUtils.getDCInputSet(other)));
                                }
                                catch (Exception e)
                                {
                                    log.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
            }

            metadataExtraSourceInfo.put(fieldID, dtodcvalue);
            dtodcvalues.addAll(dtodcvalue);
        }

        List<Bitstream> bitstreams = new LinkedList<Bitstream>();

        for (Item other : others)
        {
            for (Bundle bnd : other.getBundles(Constants.CONTENT_BUNDLE_NAME))
            {
                for (Bitstream b : bnd.getBitstreams())
                {
                    bitstreams.add(b);
                }
            }
        }

        request.setAttribute("bitstreams", bitstreams);
        request.setAttribute("dtodcvalues", dtodcvalues);
        request.setAttribute("metadataSourceInfo", metadataSourceInfo);
        request.setAttribute("metadataExtraSourceInfo",
                metadataExtraSourceInfo);
        request.setAttribute("dcinputs", dcinputs);
        return item;
    }

    /**
     * Utility method to create a DTODCValue and fill it on list to render on
     * view
     * 
     * @param fieldID
     * @param dtodcvalue
     * @param other
     * @param v
     * @param hidden
     * @param mdString
     * @return
     * @throws SQLException
     */
    private DTODCValue createDTODCValue(Integer fieldID,
            List<DTODCValue> dtodcvalue, Item other, Metadatum v,
            boolean hidden, boolean removed, String mdString)
                    throws SQLException
    {

        DTODCValue dto = new DTODCValue();
        dto.setDcValue(v);
        dto.setHidden(hidden);
        dto.setOwner(other.getID());
        dto.setRemoved(removed);
        dto.setMetadataFieldId(fieldID);
        // Collection[] collections = other.getCollections();
        // // owning Collection ID for choice authority calls
        // int collectionID = -1;
        // if (collections.length > 0)
        // collectionID = collections[0].getID();
        // dto.setOwnerCollectionID(collectionID);

        if (getBlockedTypes().contains(mdString))
        {
            dto.setBlocked(true);
        }
        dtodcvalue.add(dto);
        return dto;

    }

    /**
     * Wrapped method to utility method to create a DTODCValue and fill it on
     * list to render on view
     * 
     * @param fieldID
     * @param dtodcvalue
     * @param other
     * @param v
     * @param hidden
     * @param mdString
     * @return
     * @throws SQLException
     */
    private DTODCValue createDTODCValue(Integer fieldID,
            List<DTODCValue> dtodcvalue, Item other, Metadatum v,
            boolean hidden, String mdString) throws SQLException
    {
        return createDTODCValue(fieldID, dtodcvalue, other, v, hidden, false,
                mdString);
    }

    private boolean checkContentEquality(Metadatum[] value, Metadatum v)
    {
        DTODCValue dtoValue = new DTODCValue();
        dtoValue.setDcValue(v);
        boolean result = false;
        Metadatum adcvalue[];
        int j = (adcvalue = value).length;
        for (int i = 0; i < j; i++)
        {
            Metadatum vv = adcvalue[i];
            DTODCValue dtoValueToCompare = new DTODCValue();
            dtoValueToCompare.setDcValue(vv);
            if ((dtoValueToCompare.getValue() == null)
                    || (dtoValue.getValue() == null))
            {
                continue;
            }
            if (!dtoValueToCompare.getValue().equals(dtoValue.getValue())
                    || (!dtoValueToCompare.getAuthority()
                            .equals(dtoValue.getAuthority())))
            {
                continue;
            }
            result = true;
            break;
        }

        return result;
    }

    private void putItemsOnGrid(Context context, Map<Integer, String[]> grid,
            Map<Integer, DSpaceObject> extraInfo, List<DSpaceObject> items,
            HttpServletRequest hrq) throws SQLException, IOException
    {
        final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
                .getNamedPlugin(StreamDisseminationCrosswalk.class,
                        ConfigurationManager.getProperty("deduplication",
                                "tool.duplicatechecker.citation"));

        for (DSpaceObject item : items)
        {

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try
            {
                streamCrosswalkDefault.disseminate(context, item, output);
            }
            catch (CrosswalkException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }
            catch (AuthorizeException e)
            {
                log.error(e.getMessage(), e);
                throw new IOException(e);
            }

            grid.put(item.getID(), new String[] { output.toString("UTF-8") });
            extraInfo.put(item.getID(), item);
        }
    }

    /**
     * Get item from int id.
     * 
     * @param context
     * @param items
     * @return
     * @throws SQLException
     */
    private List<DSpaceObject> getDSpaceObjects(Context context, int[] items,
            int resourceTypeId) throws SQLException
    {
        List<DSpaceObject> result = new ArrayList<DSpaceObject>();
        for (int i = 0; i < items.length; i++)
        {
            DSpaceObject item = DSpaceObject.find(context, resourceTypeId, items[i]);
            result.add(item);
        }
        return result;
    }

    private List<Item> getItems(Context context, int[] items)
            throws SQLException
    {
        List<Item> result = new ArrayList<Item>();
        for (int i = 0; i < items.length; i++)
        {
            Item item = Item.find(context, items[i]);
            result.add(item);
        }
        return result;
    }

    /**
     * Get collections from int id.
     * 
     * @param context
     * @param items
     * @return
     * @throws SQLException
     */
    private List<Collection> getCollections(Context context, int[] items,
            int target) throws SQLException
    {
        List<Collection> result = new ArrayList<Collection>();
        for (int i = 0; i < items.length; i++)
        {
            if (items[i] != target)
            {
                Collection item = Collection.find(context, items[i]);
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Process input from the cleaner duplicate form
     * 
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void processUpdateItem(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item, Collection ownerCollection,
            List<Collection> otherCollections) throws ServletException,
                    IOException, SQLException, AuthorizeException
    {

        /* First, we remove it all, then build it back up again. */
        item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        // process collections
        if (ownerCollection != null)
        { // if exist collection owner check where
          // to insert item (on a
          // inprogresssubmission object direct
          // insert on db)
            TableRow trWsi = DatabaseManager.findByUnique(context,
                    "workspaceitem", "item_id", item.getID());
            if (trWsi == null)
            { // if item not in workspace then
              // check if item
              // is
              // in workflow state
                TableRow trWfi = DatabaseManager.findByUnique(context,
                        "workflowitem", "item_id", item.getID());
                if (trWfi == null)
                {
                    if (item.getOwningCollection() != null
                            && item.getOwningCollection()
                                    .getID() != ownerCollection.getID())
                    {
                        item.move(item.getOwningCollection(), ownerCollection); // on
                                                                                // archived
                                                                                // item
                                                                                // move
                                                                                // collection
                                                                                // from
                                                                                // old
                                                                                // to
                                                                                // new
                    }
                }
                else
                {
                    trWfi.setColumn("collection_id", ownerCollection.getID());
                    DatabaseManager.update(context, trWfi);
                }
            }
            else
            {

                trWsi.setColumn("collection_id", ownerCollection.getID());
                DatabaseManager.update(context, trWsi);

            }
        }

        // if item is archived then insert other collections
        if (item.isArchived())
        {
            Map<Collection, Boolean> toRemoveItem = new LinkedHashMap<Collection, Boolean>(); // this
                                                                                              // map
                                                                                              // contains
                                                                                              // collection
                                                                                              // (with
                                                                                              // value
                                                                                              // a
                                                                                              // true)
                                                                                              // where
                                                                                              // we
                                                                                              // goes
                                                                                              // to
                                                                                              // remove
                                                                                              // item
            if (otherCollections != null && !otherCollections.isEmpty())
            {
                for (Collection c : otherCollections)
                {
                    boolean founded = false;
                    for (Collection cc : item.getCollections())
                    {
                        if (c.getID() == cc.getID())
                        {
                            founded = true;
                            toRemoveItem.put(cc, false);
                            break;
                        }
                        else
                        {
                            if (!toRemoveItem.containsKey(cc))
                            {
                                toRemoveItem.put(cc, true);
                            }
                        }
                    }
                    if (!founded)
                    {
                        c.addItem(item);
                        c.update();
                    }
                }
            }

            for (Collection cc : toRemoveItem.keySet())
            {
                if (cc.getID() != ownerCollection.getID())
                {
                    if (toRemoveItem.get(cc))
                    { // if collection is old remove
                      // item
                        cc.removeItem(item);
                        cc.update();
                    }
                }
            }
        }
        // read sorted row contains on table_values parameter
        String[] tablerows = request.getParameterValues("table_values");
        String result = "";
        String[] splitresult = null;
        for (String row : tablerows)
        {
            // warning 'table-selected' is table id on the view
            result = row.replaceAll("table\\-selected\\[\\]=", "");
            splitresult = result.split("&");
        }

        // for each row searching for "value_" parameters (with "hidden_value_"
        // prefix there are discarded metadata)
        for (String row : splitresult)
        {
            if (!row.isEmpty())
            {
                int index = row.indexOf("_");
                String p = "value" + row.substring(index);
                String parameter = request.getParameter(p);
                if (parameter != null && !parameter.isEmpty())
                {

                    /*
                     * It's a metadata value - it will be of the form
                     * value_element_1 OR value_element_qualifier_2 (the number
                     * being the sequence number) We use a StringTokenizer to
                     * extract these values
                     */
                    StringTokenizer st = new StringTokenizer(p, "_");

                    st.nextToken(); // Skip "value"

                    String schema = st.nextToken();

                    String element = st.nextToken();

                    String qualifier = null;

                    if (st.countTokens() == 2)
                    {
                        qualifier = st.nextToken();
                    }

                    String sequenceNumber = st.nextToken();

                    // Get a string with "element" for unqualified or
                    // "element_qualifier"
                    String key = MetadataField.formKey(schema, element,
                            qualifier);

                    // Get the language
                    String language = request.getParameter(
                            "language_" + key + "_" + sequenceNumber);

                    // Empty string language = null
                    if ((language != null) && language.equals(""))
                    {
                        language = null;
                    }

                    // Get the authority key if any
                    String authority = request.getParameter(
                            "choice_" + key + "_authority_" + sequenceNumber);

                    // Empty string authority = null
                    if ((authority != null) && authority.equals(""))
                    {
                        authority = null;
                    }

                    // Get the authority confidence value, passed as symbolic
                    // name
                    String sconfidence = request.getParameter(
                            "choice_" + key + "_confidence_" + sequenceNumber);
                    int confidence = (sconfidence == null
                            || sconfidence.equals("")) ? Choices.CF_NOVALUE
                                    : Choices.getConfidenceValue(sconfidence);

                    // Get the value
                    String value = parameter.trim();

                    // If remove button pressed for this value, we don't add it
                    // back to the item. We also don't add empty values
                    // (if no authority is specified).
                    if (!((value.equals("") && authority == null)))
                    {
                        // Value is empty, or remove button for this wasn't
                        // pressed
                        item.addMetadata(schema, element, qualifier, language,
                                value, authority, confidence);
                    }
                }
            }
        }

        // logic to discovery bitstream
        int[] bitIDs = UIUtil.getIntParameters(request, "bitstream_id");
        if (bitIDs == null)
        {
            bitIDs = new int[0];
        }
        Arrays.sort(bitIDs);

        Bundle[] originals = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        for (Bundle orig : originals)
        {
            Bitstream[] bits = orig.getBitstreams();
            for (Bitstream b : bits)
            {
                // bitstream in the target item has been unselect
                if (Arrays.binarySearch(bitIDs, b.getID()) < 0)
                {
                    Bundle bundle = b.getBundles()[0];
                    bundle.removeBitstream(b);
                    bundle.update();
                }
            }
        }

        Bundle orig = null;
        if (bitIDs.length > 0)
        {
            if (originals.length == 0)
            {
                orig = item.createBundle(Constants.CONTENT_BUNDLE_NAME);
                orig.update();
                // add read policy to the anonymous group
                AuthorizeManager.addPolicy(context, orig, Constants.READ,
                        Group.find(context, 0));
            }
            else
            {
                orig = originals[0];
            }
            for (int bid : bitIDs)
            {
                Bitstream b = Bitstream.find(context, bid);
                // we need to add only bitstream that are not yet attached to
                // the target item
                if (b.getBundles()[0].getItems()[0].getID() != item.getID())
                {
                    InputStream is = b.retrieve();
                    Bitstream newBits = orig.createBitstream(is);
                    newBits.setName(b.getName());
                    newBits.setDescription(b.getDescription());
                    newBits.setFormat(b.getFormat());
                    newBits.setUserFormatDescription(
                            b.getUserFormatDescription());
                    newBits.setSource(b.getSource());
                    is.close();
                    List<ResourcePolicy> rps = AuthorizeManager
                            .getPolicies(context, b);
                    for (ResourcePolicy rp : rps)
                    {
                        ResourcePolicy newrp = ResourcePolicy.create(context);
                        newrp.setAction(rp.getAction());
                        newrp.setEndDate(rp.getEndDate());
                        newrp.setStartDate(rp.getStartDate());
                        newrp.setEPerson(rp.getEPerson());
                        newrp.setGroup(rp.getGroup());
                        newrp.setResource(newBits);
                        newrp.update();
                    }
                }
            }
        }

        if (orig != null && orig.getBitstreams().length == 0)
        {
            item.removeBundle(orig);
        }
        item.update();
    }

}
