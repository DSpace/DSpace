package org.dspace.app.xmlui.aspect.shoppingcart;

/**
 * User: lantian @ atmire . com
 * Date: 7/11/13
 * Time: 11:20 AM
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

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
import org.dspace.paymentsystem.Voucher;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.utils.DSpace;

import javax.swing.*;

/**
 * The manage voucher page is the starting point page for managing
 * voucher. From here the user is able to browse or search for voucher,
 * once identified the user can selected them for deletion by selecting
 * the checkboxes and clicking delete or click their name to edit the
 * voucher.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ManageVoucherMain extends AbstractDSpaceTransformer {

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.voucher.ManageVoucherMain.title");

    private static final Message T_Voucher_trail =
            message("xmlui.voucher.general.Voucher_trail");

    private static final Message T_main_head =
            message("xmlui.voucher.ManageVoucherin.main_head");

    private static final Message T_actions_head =
            message("xmlui.voucher.ManageVoucherin.actions_head");

    private static final Message T_actions_create =
            message("xmlui.voucher.ManageVoucherMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.voucher.ManageVoucherMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.voucher.ManageVoucherMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.voucher.ManageVoucherMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.voucher.ManageVoucherMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.voucher.ManageVoucherMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.voucher.ManageVoucher.search_head");

    private static final Message T_search_column1 =
            message("xmlui.voucher.ManageVoucherMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.voucher.ManageVoucherMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.voucher.ManageVoucherMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.voucher.ManageVoucherMain.search_column4");


    private static final Message T_search_column5 =
            message("xmlui.voucher.ManageVoucherMain.search_column5");


    private static final Message T_search_column6 =
            message("xmlui.voucher.ManageVoucherMain.search_column6");





    private static final Message T_no_results =
            message("xmlui.property.ManageVoucherMain.no_results");

    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 20;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_Voucher_trail);
    }


    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters */
        int page          = new Float(parameters.getParameterAsFloat("page",0)).intValue();
        int highlightID   = new Float(parameters.getParameterAsFloat("highlightID",-1)).intValue();
        int dsoID   = parameters.getParameterAsInteger("dsoID",-1);
        int typeID   = new Float(parameters.getParameterAsFloat("typeID",-1)).intValue();

        String query      = decodeFromURL(parameters.getParameter("query",null));
        String baseURL    = null;


        Division main = null;

        baseURL=  contextPath+"/voucher?administrative-continue="+knot.getId();
        ArrayList<Voucher> totalVouchers = Voucher.findAll(context);


        Voucher[] vouchers = Voucher.search(context, query, page*PAGE_SIZE, PAGE_SIZE);

        //  properties = Voucher.findAllByName(context,query);

        // DIVISION: Voucher-main
        main = body.addInteractiveDivision("voucher-main", contextPath
                + "/voucher", Division.METHOD_POST,
                "primary administrative voucher");

        int resultCount   = totalVouchers.size();



        main.setHead(T_main_head);

        // DIVISION: voucher-actions
        Division actions = main.addDivision("voucher-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
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

        // DIVISION: voucher-search
        Division search = main.addDivision("voucher-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + vouchers.length;

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

        Table table = search.addTable("voucher-search-table", vouchers.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("");
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);

        header.addCell().addContent(T_search_column5);
        header.addCell().addContent(T_search_column6);
        CheckBox selectVoucherService;
        for (Voucher voucher : vouchers)
        {
            String voucherID = String.valueOf(voucher.getID());

            String code = voucher.getCode();
            String status = voucher.getStatus();
            Date creation = voucher.getCreation();
            String explanation = voucher.getExplanation();
            EPerson generator = EPerson.find(context,voucher.getGenerator());

            String url = baseURL+"&submit_edit&voucher_id="+voucherID;

            Row row;
            if (voucher.getID() == highlightID)
            {
                // This is a highlighted voucher
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }
            row = table.addRow();
            selectVoucherService = row.addCell().addCheckBox("select_voucher");
            selectVoucherService.setLabel(voucherID);
            selectVoucherService.addOption(voucherID);

            row.addCellContent(voucherID);
            row.addCell().addXref(url, code);
            row.addCellContent(status);
            row.addCell().addContent(creation.toString());
            if(explanation!=null){
                row.addCell().addContent(explanation);
            }
            else
            {
                row.addCell().addContent("");
            }
            if(generator!=null){
                row.addCell().addContent(generator.getFullName());
            }
            else
            {
                row.addCell().addContent("");
            }


        }

        if (vouchers.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 5);
            cell.addHighlight("italic").addContent(T_no_results);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

    }
}
