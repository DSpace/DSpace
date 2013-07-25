/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.Voucher;
import org.dspace.utils.DSpace;

import javax.swing.*;

/**
 * The manage shoppingcart page is the starting point page for managing
 * shoppingcart. From here the user is able to browse or search for shoppingcart,
 * once identified the user can selected them for deletion by selecting
 * the checkboxes and clicking delete or click their name to edit the
 * shoppingcart.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ManageShoppingcartMain extends AbstractDSpaceTransformer {

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.property.ManageShoppingcartMain.title");

    private static final Message T_shoppingcart_trail =
            message("xmlui.property.general.shoppingcart_trail");

    private static final Message T_main_head =
            message("xmlui.property.ManageShoppingcartin.main_head");

    private static final Message T_actions_head =
            message("xmlui.property.ManageShoppingcartin.actions_head");

    private static final Message T_actions_create =
            message("xmlui.property.ManageShoppingcartMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.property.ManageShoppingcartMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.property.ManageShoppingcartMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.property.ManageShoppingcartMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.property.ManageShoppingcartin.actions_search");

    private static final Message T_search_help =
            message("xmlui.property.ManageShoppingcartMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.property.ManageShoppingcart.search_head");

    private static final Message T_search_column1 =
            message("xmlui.property.ManageShoppingcartMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.property.ManageShoppingcartMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.property.ManageShoppingcartMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.property.ManageShoppingcartMain.search_column4");


    private static final Message T_search_column5 =
            message("xmlui.property.ManageShoppingcartMain.search_column5");


    private static final Message T_search_column6 =
            message("xmlui.property.ManageShoppingcartMain.search_column6");

    private static final Message T_search_column7 =
            message("xmlui.property.ManageShoppingcartMain.search_column7");
    private static final Message T_search_column8 =
            message("xmlui.property.ManageShoppingcartMain.search_column8");
    private static final Message T_search_column9 =
            message("xmlui.property.ManageShoppingcartMain.search_column9");
    private static final Message T_search_column10 =
            message("xmlui.property.ManageShoppingcartMain.search_column10");
    private static final Message T_search_column11 =
            message("xmlui.property.ManageShoppingcartMain.search_column11");



    private static final Message T_no_results =
            message("xmlui.property.ManageShoppingcartMain.no_results");

    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 20;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_shoppingcart_trail);
    }


    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters */
        int page          = new Float(parameters.getParameterAsFloat("page",0)).intValue();
        int highlightID   = new Float(parameters.getParameterAsFloat("highlightID",-1)).intValue();
        int dsoID   = parameters.getParameterAsInteger("dsoID",-1);
        int typeID   = new Float(parameters.getParameterAsFloat("typeID",-1)).intValue();




        DSpaceObject dso = DSpaceObject.find(context,typeID,dsoID);

        String query      = decodeFromURL(parameters.getParameter("query",null));
        String baseURL    = null;


        Division main = null;

        baseURL=  contextPath+"/shoppingcart?administrative-continue="+knot.getId();
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

        //shoppingCarts = paymentSystemService.findAllShoppingCart(context,dsoID);
        ShoppingCart[] shoppingCarts = ShoppingCart.search(context, query, page*PAGE_SIZE, PAGE_SIZE);

        ShoppingCart[] totalCount = ShoppingCart.findAll(context);

        // DIVISION: Shoppingcart-main
        main = body.addInteractiveDivision("shoppingcart-main", contextPath
                + "/shoppingcart", Division.METHOD_POST,
                "primary administrative shoppingcart");

        int resultCount   = totalCount.length;



        main.setHead(T_main_head);

        // DIVISION: shoppingcart-actions
        Division actions = main.addDivision("shoppingcart-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");

        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: shoppingcart-search
        Division search = main.addDivision("shoppingcart-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + shoppingCarts.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        Table table = search.addTable("shoppingcart-search-table", shoppingCarts.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);
//        header.addCell().addContent(T_search_column6);
        header.addCell().addContent(T_search_column7);
        header.addCell().addContent(T_search_column8);
        header.addCell().addContent(T_search_column9);
        header.addCell().addContent(T_search_column10);
        header.addCell().addContent(T_search_column11);


        CheckBox selectShoppingcartService;
        for (ShoppingCart shoppingCart : shoppingCarts)
        {
            String shoppingcartID = String.valueOf(shoppingCart.getID());

            Integer voucherId = shoppingCart.getVoucher();
            String country = shoppingCart.getCountry();
            String currency = shoppingCart.getCurrency();
            Integer resourceId = shoppingCart.getItem();
            String status = shoppingCart.getStatus();
            Double total = shoppingCart.getTotal();
            String transactionId = shoppingCart.getTransactionId();
            String secureToken = shoppingCart.getSecureToken();
            Integer depositorId = shoppingCart.getDepositor();
            Item resource = Item.find(context,resourceId);
            EPerson depositor = EPerson.find(context,depositorId);
            String url = baseURL+"&submit_edit&shoppingcart_id="+shoppingcartID;

            Row row;
            if (shoppingCart.getID() == highlightID)
            {
                // This is a highlighted shoppingcart
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }
            row = table.addRow();
            selectShoppingcartService = row.addCell().addCheckBox("select_shoppingcart");
            selectShoppingcartService.setLabel(shoppingcartID);
            selectShoppingcartService.addOption(shoppingcartID);

            row.addCellContent(shoppingcartID);
             try{
            DCValue[] name = resource.getDC("title", null, Item.ANY);
            if(name==null||name.length==0)
            {
                row.addCell().addXref(url,"Untitled");
            }
            else
            if(resource==null)
            {
                row.addCell().addXref(url, "Item not found");
            }
            else
            {
                row.addCell().addXref(url, name[0].value);
            }

             }catch (Exception e)
             {
                 row.addCell().addXref(url, "UnKnown");
                 System.out.println(e.getMessage());
             }
            row.addCellContent(depositor.getFullName());
            if(voucherId!=null)
            {
                Voucher voucher = Voucher.findById(context,voucherId);

                if(voucher!=null)
                {
                    row.addCellContent(voucher.getCode());
                }
                else
                {
                    row.addCellContent("");
                }
            }else{
                row.addCellContent("");
            }



//            row.addCellContent(secureToken);
            row.addCellContent(transactionId);
            row.addCellContent(status);
            row.addCellContent(currency);
            row.addCellContent(Double.toString(total));
            row.addCellContent(country);

        }

        if (shoppingCarts.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 5);
            cell.addHighlight("italic").addContent(T_no_results);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

    }
}
