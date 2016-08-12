package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.DoiFactoryUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.integration.batch.ScriptCrossrefSender;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class DoiFixUtilityCheckerServlet extends DSpaceServlet
{

    private static final String QUERY_PGSQL = "SELECT i2.item_id FROM item i2 INNER JOIN "
            + ScriptCrossrefSender.TABLE_NAME_DOI2ITEM
            + " d2i ON d2i.item_id = i2.item_id LEFT JOIN metadatavalue mv2 ON i2.item_id = mv2.resource_id and mv2.resource_type_id = 2 LEFT JOIN metadatafieldregistry mfr2 ON mv2.metadata_field_id = mfr2.metadata_field_id WHERE mfr2.element = 'identifier' AND mfr2.qualifier = 'doi' AND mv2.text_value != d2i.identifier_doi AND d2i.last_modified is not null AND d2i.criteria = ?";

    private static final String QUERY_ORACLE = "SELECT i2.item_id FROM item i2 INNER JOIN "
        + ScriptCrossrefSender.TABLE_NAME_DOI2ITEM
        + " d2i ON d2i.item_id = i2.item_id LEFT JOIN metadatavalue mv2 ON i2.item_id = mv2.resource_id and mv2.resource_type_id = 2 LEFT JOIN metadatafieldregistry mfr2 ON mv2.metadata_field_id = mfr2.metadata_field_id WHERE mfr2.element = 'identifier' AND mfr2.qualifier = 'doi' AND DBMS_LOB.SUBSTR(mv2.text_value,3000) != d2i.identifier_doi AND d2i.last_modified is not null AND d2i.criteria = ?";
    
    /** log4j category */
    private static Logger log = Logger
            .getLogger(DoiFixUtilityCheckerServlet.class);

    public static int DOI_ALL = 1;

    public static int DOI_ANY = 0;

    private static String dbName = ConfigurationManager.getProperty("db.name");
    
    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        // After loaded grid to display logic here tries discover if target
        // must be choice or not.

        int submit = UIUtil.getIntParameter(request, "submit");
        if (submit != -1)
        {
            // perform action
            String pathString = request.getPathInfo();
            String[] pathInfo = pathString.split("/", 2);
            String criteria = pathInfo[1];
            if (submit == DOI_ALL)
            {

                TableRowIterator tri = DatabaseManager.query(context, getQuery(),
                        criteria);

                while (tri.hasNext())
                {
                    TableRow tr = tri.next();
                    int id = tr.getIntColumn("item_id");
                    fixItem(context, id);
                }
                context.commit();
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doi");
            }
            else if (submit == DOI_ANY)
            {

                int[] items = UIUtil.getIntParameters(request, "builddoi");

                for (Integer id : items)
                {
                    fixItem(context, id);
                }
                context.commit();
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doifix");
            }

        }
        else
        {

            String[] criteria = ConfigurationManager.getProperty("doi.list")
                    .split(",");

            Map<String, Item[]> results = new HashMap<String, Item[]>();
            Map<Integer, String> doi2items = new HashMap<Integer, String>();
            for (String type : criteria)
            {
                try
                {
                    TableRowIterator tri = DatabaseManager.query(context,
                            getQuery(), type);

                    List<Item> tempItems = new LinkedList<Item>();
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();
                        int intColumn = tr.getIntColumn("item_id");
                        Item find = Item.find(context, intColumn);
                        tempItems.add(find);
                    }
                    Item[] realresult = null;
                    if (tempItems != null && !tempItems.isEmpty())
                    {
                        realresult = tempItems.toArray(new Item[tempItems
                                .size()]);
                    }
                    results.put(type, realresult);
                    context.commit();

                    if (realresult != null)
                    {

                        for (Item real : realresult)
                        {
                            doi2items.put(real.getID(), DoiFactoryUtils
                                    .getDoiFromDoi2Item(context, real.getID()));
                        }
                    }

                }
                catch (SQLException e)
                {
                    log.error(e.getMessage(), e);
                }

            }

            request.setAttribute("doi2items", doi2items);

            // Pass the result
            request.setAttribute("mapitems", results);
            JSPManager.showJSP(request, response, "/doi/fixDoiResults.jsp");
        }
    }

    public static String getQuery()
    {
        if ("oracle".equals(dbName)) {
            return QUERY_ORACLE;
        }
        
        return QUERY_PGSQL;
    }

    private void fixItem(Context context, Integer id) throws SQLException,
            AuthorizeException
    {
        Item item = Item.find(context, id);
        item.clearMetadata("dc", "identifier", "doi", Item.ANY);
        item.addMetadata("dc", "identifier", "doi", null,
                DoiFactoryUtils.getDoiFromDoi2Item(context, id));
        item.update();
    }

}
