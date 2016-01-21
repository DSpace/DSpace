/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.registries;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;

/**
 * Main management page for bitstream formats, this page lists all known formats
 * enabling the user to add more, updating existing, or delete formats.
 * 
 * @author Scott Phillips
 */
public class FormatRegistryMain
        extends AbstractDSpaceTransformer
{

    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_title =
            message("xmlui.administrative.registries.FormatRegistryMain.title");
    private static final Message T_format_registry_trail =
            message(
            "xmlui.administrative.registries.general.format_registry_trail");
    private static final Message T_head =
            message("xmlui.administrative.registries.FormatRegistryMain.head");
    private static final Message T_para1 =
            message("xmlui.administrative.registries.FormatRegistryMain.para1");
    private static final Message T_new_link =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.new_link");
    private static final Message T_column1 =
            message("xmlui.administrative.registries.FormatRegistryMain.column1");
    private static final Message T_column2 =
            message("xmlui.administrative.registries.FormatRegistryMain.column2");
    private static final Message T_column3 =
            message("xmlui.administrative.registries.FormatRegistryMain.column3");
    private static final Message T_column4 =
            message("xmlui.administrative.registries.FormatRegistryMain.column4");
    private static final Message T_column5 =
            message("xmlui.administrative.registries.FormatRegistryMain.column5");
    private static final Message T_internal =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.internal");
    private static final Message T_support_0 =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.support_0");
    private static final Message T_support_1 =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.support_1");
    private static final Message T_support_2 =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.support_2");
    private static final Message T_submit_delete =
            message(
            "xmlui.administrative.registries.FormatRegistryMain.submit_delete");

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_format_registry_trail);
    }

    public void addBody(Body body)
            throws WingException, SQLException
    {
        // Get our parameters & state
        int highlightID = parameters.getParameterAsInteger("highlightID", -1);
        List<BitstreamFormat> formats = bitstreamFormatService.findAll(context);
        String addURL = contextPath
                + "/admin/format-registry?administrative-continue="
                + knot.getId() + "&submit_add";


        // DIVISION: bitstream-format-registry
        Division main = body.addInteractiveDivision("bitstream-format-registry",
                contextPath + "/admin/format-registry", Division.METHOD_POST,
                "primary administrative format-registry");
        main.setHead(T_head);
        main.addPara(T_para1);
        main.addPara().addXref(addURL, T_new_link);


        Table table = main.addTable("bitstream-format-registry", formats.size()
                + 1, 5);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_column1);
        header.addCellContent(T_column2);
        header.addCellContent(T_column3);
        header.addCellContent(T_column4);
        header.addCellContent(T_column5);

        for (BitstreamFormat format : formats)
        {
            String id = String.valueOf(format.getID());
            String mimeType = format.getMIMEType();
            String name = format.getShortDescription();
            int supportLevel = format.getSupportLevel();
            boolean internal = format.isInternal();

            boolean highlight = false;
            if (format.getID() == highlightID)
            {
                highlight = true;
            }

            String url = contextPath
                    + "/admin/format-registry?administrative-continue="
                    + knot.getId() + "&submit_edit&formatID=" + id;


            Row row;
            if (highlight)
            {
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            // Select checkbox
            Cell cell = row.addCell();
            if (format.getID() > 1)
            {
                // Do not allow unknown to be removed.
                CheckBox select = cell.addCheckBox("select_format");
                select.setLabel(id);
                select.addOption(id);
            }

            // ID
            row.addCell().addContent(id);

            // Name
            row.addCell().addXref(url, name);

            // Mime type
            cell = row.addCell();
            cell.addXref(url, mimeType);
            if (internal)
            {
                cell.addContent(" ");
                cell.addContent(T_internal);
            }

            // support level
            switch (supportLevel)
            {
                case 0:
                    row.addCell().addXref(url, T_support_0);
                    break;
                case 1:
                    row.addCell().addXref(url, T_support_1);
                    break;
                case 2:
                    row.addCell().addXref(url, T_support_2);
                    break;
            }
        }

        main.addPara().addButton("submit_delete").setValue(T_submit_delete);

        main.addHidden("administrative-continue").setValue(knot.getId());

    }
}
