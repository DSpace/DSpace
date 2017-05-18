/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import java.sql.SQLException;

import org.datadryad.api.DryadOrganizationConcept;
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
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.paymentsystem.Voucher;

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

    private static final Message T_col_select_box =
            message("xmlui.property.ManageShoppingcartMain.col_select_box");

    private static final Message T_col_item_ID =
            message("xmlui.property.ManageShoppingcartMain.col_item_ID");

    private static final Message T_col_item_title =
            message("xmlui.property.ManageShoppingcartMain.col_item_title");

    private static final Message T_col_voucher =
            message("xmlui.property.ManageShoppingcartMain.col_voucher");
    private static final Message T_col_status =
            message("xmlui.property.ManageShoppingcartMain.col_status");
    private static final Message T_col_country =
            message("xmlui.property.ManageShoppingcartMain.col_country");
    private static final Message T_col_sponsor =
            message("xmlui.property.ManageShoppingcartMain.col_sponsor");
    private static final Message T_col_total =
            message("xmlui.property.ManageShoppingcartMain.col_total");


    private static final Message T_no_results =
            message("xmlui.property.ManageShoppingcartMain.no_results");

    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 20;


    public void addPageMeta(PageMeta pageMeta) throws WingException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_shoppingcart_trail);
    }


    public void addBody(Body body) throws WingException, SQLException {
        /* Get and setup our parameters */
        int page = new Float(parameters.getParameterAsFloat("page", 0)).intValue();

        String query = decodeFromURL(parameters.getParameter("query", null));

        String baseURL = contextPath + "/admin/shoppingcart?administrative-continue=" + knot.getId();

        ShoppingCart[] shoppingCarts = ShoppingCart.search(context, query, page * PAGE_SIZE, PAGE_SIZE);

        ShoppingCart[] totalCount = ShoppingCart.findAll(context);

        // DIVISION: Shoppingcart-main
        Division main = body.addInteractiveDivision("shoppingcart-main", contextPath
                        + "/admin/shoppingcart", Division.METHOD_POST,
                "primary administrative shoppingcart");

        int resultCount = totalCount.length;


        main.setHead(T_main_head);

        // DIVISION: shoppingcart-actions
        Division actions = main.addDivision("shoppingcart-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL + "&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");

        if (query != null) {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: shoppingcart-search
        Division search = main.addDivision("shoppingcart-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE) {
            // If there are enough results then paginate the results
            int firstIndex = page * PAGE_SIZE + 1;
            int lastIndex = page * PAGE_SIZE + shoppingCarts.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE)) {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0) {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount, firstIndex, lastIndex, prevURL, nextURL);
        }

        Table table = search.addTable("shoppingcart-search-table", shoppingCarts.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_col_select_box);   // 1: T_col_select_box
        header.addCell().addContent(T_col_item_ID);      // 2: T_col_item_ID
        header.addCell().addContent(T_col_item_title);   // 3: T_col_item_title
        header.addCell().addContent(T_col_status);       // 4: T_col_status
        header.addCell().addContent(T_col_sponsor);      // 5: T_col_sponsor
        header.addCell().addContent(T_col_country);      // 6: T_col_country
        header.addCell().addContent(T_col_voucher);      // 7: T_col_voucher
        header.addCell().addContent(T_col_total);        // 8: T_col_total


        CheckBox selectShoppingcartService;
        for (ShoppingCart shoppingCart : shoppingCarts) {
            String shoppingcartID = String.valueOf(shoppingCart.getID());

            Row row = table.addRow();

            // 1: T_col_select_box
            selectShoppingcartService = row.addCell().addCheckBox("select_shoppingcart");
            selectShoppingcartService.setLabel(shoppingcartID);
            selectShoppingcartService.addOption(shoppingcartID);

            // 2: T_col_item_ID
            Integer resourceId = shoppingCart.getItem();
            row.addCellContent(resourceId.toString());

            // 3: T_col_item_title
            Item resource = Item.find(context, resourceId);
            try {
                String targetURL = baseURL + "&submit_edit&shoppingcart_id=" + shoppingcartID;
                DCValue[] name = resource.getDC("title", null, Item.ANY);
                if (name == null || name.length == 0) {
                    row.addCell().addXref(targetURL, "Untitled");
                } else if (resource == null) {
                    row.addCell().addXref(targetURL, "Item not found");
                } else {
                    row.addCell().addXref(targetURL, name[0].value);
                }

            } catch (Exception e) {
                row.addCell().addXref(url, "Unknown");
                System.out.println(e.getMessage());
            }

            // 4: T_col_status
            row.addCellContent(shoppingCart.getStatus());

            // 5: T_col_sponsor
            String sponsorName = "none";
            DryadOrganizationConcept sponsor = shoppingCart.getSponsoringOrganization(context);
            if (sponsor != null) {
                if (sponsor.getSubscriptionPaid()) {
                    sponsorName = sponsor.getFullName();
                }
            }
            row.addCellContent(sponsorName);

            // 6: T_col_country
            String country = shoppingCart.getCountry();
            if (country != null) {
                row.addCellContent(shoppingCart.getCountry());
            } else {
                row.addCellContent("");
            }

            // 7: T_col_voucher
            Integer voucherId = shoppingCart.getVoucher();
            Voucher voucher = null;
            if (voucherId != null) {
                voucher = Voucher.findById(context, voucherId);
            }
            if (voucher != null) {
                row.addCellContent(voucher.getCode());
            } else {
                row.addCellContent("");
            }

            // 8: T_col_total
            row.addCellContent(Double.toString(shoppingCart.getTotal()));
        }

        if (shoppingCarts.length <= 0) {
            Cell cell = table.addRow().addCell(1, 5);
            cell.addHighlight("italic").addContent(T_no_results);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

    }
}
