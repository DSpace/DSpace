/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.metadataimport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.dspace.app.bulkedit.BulkEditMetadataValue;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import org.dspace.app.bulkedit.BulkEditChange;

/**
 * Web interface to Bulk Metadata Import app.
 * ported from org.dspace.app.webui.servlet.MetadataImportServlet
 *
 * Display form for user to review changes and confirm
 *
 * @author Kim Shepherd
 */

public class MetadataImportUpload extends AbstractDSpaceTransformer {

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_return = message("xmlui.general.return");
    private static final Message T_trail = message("xmlui.administrative.metadataimport.general.trail");
    private static final Message T_no_changes = message("xmlui.administrative.metadataimport.general.no_changes");
    private static final Message T_new_item = message("xmlui.administrative.metadataimport.general.new_item");
    private static final Message T_title = message("xmlui.administrative.metadataimport.general.title");
    private static final Message T_head1 = message("xmlui.administrative.metadataimport.general.head1");

    private static final Message T_para = message("xmlui.administrative.metadataimport.MetadataImportUpload.hint");
    private static final Message T_submit_confirm = message("xmlui.administrative.metadataimport.MetadataImportUpload.submit_confirm");
    private static final Message T_changes_pending = message("xmlui.administrative.metadataimport.MetadataImportUpload.changes_pending");
    private static final Message T_item_addition = message("xmlui.administrative.metadataimport.MetadataImportUpload.item_add");
    private static final Message T_item_deletion = message("xmlui.administrative.metadataimport.MetadataImportUpload.item_remove");
    private static final Message T_collection_newowner = message("xmlui.administrative.metadataimport.MetadataImportUpload.collection_newowner");
    private static final Message T_collection_oldowner = message("xmlui.administrative.metadataimport.MetadataImportUpload.collection_oldowner");
    private static final Message T_collection_mapped = message("xmlui.administrative.metadataimport.MetadataImportUpload.collection_mapped");
    private static final Message T_collection_unmapped = message("xmlui.administrative.metadataimport.MetadataImportUpload.collection_unmapped");
    private static final Message T_item_delete = message("xmlui.administrative.metadataimport.MetadataImportUpload.item_delete");
    private static final Message T_item_withdraw = message("xmlui.administrative.metadataimport.MetadataImportUpload.item_withdraw");
    private static final Message T_item_reinstate = message("xmlui.administrative.metadataimport.MetadataImportUpload.item_reinstate");

    public void addPageMeta(PageMeta pageMeta)
        throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body)
        throws SAXException, WingException, SQLException
    {
        // Get list of changes

        Request request = ObjectModelHelper.getRequest(objectModel);
        ArrayList<BulkEditChange> changes = null;
        int num_changes = 0;

        if (request.getAttribute("changes") != null)
        {
            changes = ((ArrayList<BulkEditChange>)request.getAttribute("changes"));
            num_changes = changes.size();
        }

        // DIVISION: metadata-import
        Division div = body.addInteractiveDivision("metadata-import",
            contextPath + "/admin/metadataimport", Division.METHOD_MULTIPART,
            "primary administrative");
        div.setHead(T_head1);

        if (num_changes > 0)
        {
            div.addPara(T_para);

            Table mdchanges = div.addTable("metadata-changes", num_changes, 2);

            // Display the changes
            for (BulkEditChange change : changes)
            {
                // Get the changes
                List<BulkEditMetadataValue> adds = change.getAdds();
                List<BulkEditMetadataValue> removes = change.getRemoves();
                List<Collection> newCollections = change.getNewMappedCollections();
                List<Collection> oldCollections = change.getOldMappedCollections();

                if ((adds.size() > 0) || (removes.size() > 0) ||
                    (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                    (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null) ||
                    (change.isDeleted()) || (change.isWithdrawn()) || (change.isReinstated()))
                {
                    Row headerrow = mdchanges.addRow(Row.ROLE_HEADER);
                    // Show the item
                    if (!change.isNewItem())
                    {
                        Item i = change.getItem();
                        Cell cell = headerrow.addCell(1, 2); // colspan="2"
                        cell.addContent(T_changes_pending);
                        cell.addContent(" " + i.getID() + " (" + i.getHandle() + ")");

                    }
                    else
                    {
                      headerrow.addCellContent(T_new_item);
                    }
                }

                // Show actions
                if (change.isDeleted())
                {
                    Row mdrow = mdchanges.addRow("addition", Row.ROLE_DATA, "item-delete");

                    Cell cell = mdrow.addCell();
                    cell.addContent(T_item_delete);
                    mdrow.addCellContent("");
                }
                if (change.isWithdrawn())
                {
                    Row mdrow = mdchanges.addRow("addition", Row.ROLE_DATA, "item-withdraw");

                    Cell cell = mdrow.addCell();
                    cell.addContent(T_item_withdraw);
                    mdrow.addCellContent("");
                }
                if (change.isReinstated())
                {
                    Row mdrow = mdchanges.addRow("addition", Row.ROLE_DATA, "item-reinstate");

                    Cell cell = mdrow.addCell();
                    cell.addContent(T_item_reinstate);
                    mdrow.addCellContent("");
                }

                // Show new owning collection
                if (change.getNewOwningCollection() != null)
                {
                    Collection c = change.getNewOwningCollection();
                    if (c != null)
                    {
                        String cHandle = c.getHandle();
                        String cName = c.getName();
                        Row colrow = mdchanges.addRow("addition", Row.ROLE_DATA, "metadata-addition");
                        colrow.addCellContent(T_collection_newowner);
                        colrow.addCellContent(cHandle + " (" + cName + ")");
                    }
                }

                // Show old owning collection
                if (change.getOldOwningCollection() != null)
                {
                    Collection c = change.getOldOwningCollection();
                    if (c != null)
                    {
                        String cHandle = c.getHandle();
                        String cName = c.getName();
                        Row colrow = mdchanges.addRow("deletion", Row.ROLE_DATA, "metadata-deletion");
                        colrow.addCellContent(T_collection_oldowner);
                        colrow.addCellContent(cHandle + " (" + cName + ")");
                    }
                }

                // Show new mapped collections
                for (Collection c : newCollections)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    Row colrow = mdchanges.addRow("addition", Row.ROLE_DATA, "metadata-addition");
                    colrow.addCellContent(T_collection_mapped);
                    colrow.addCellContent(cHandle + " (" + cName + ")");
                }

                // Show old mapped collections
                for (Collection c : oldCollections)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    Row colrow = mdchanges.addRow("deletion", Row.ROLE_DATA, "metadata-deletion");
                    colrow.addCellContent(T_collection_unmapped);
                    colrow.addCellContent(cHandle + " (" + cName + ")");
                }

                // Show additions
                for (BulkEditMetadataValue dcv : adds)
                {
                    Row mdrow = mdchanges.addRow("addition", Row.ROLE_DATA, "metadata-addition");
                    String md = dcv.getSchema() + "." + dcv.getElement();
                    if (dcv.getQualifier() != null)
                    {
                        md += "." + dcv.getQualifier();
                    }
                    if (dcv.getLanguage() != null)
                    {
                        md += "[" + dcv.getLanguage() + "]";
                    }

                    Cell cell = mdrow.addCell();
                    cell.addContent(T_item_addition);
                    cell.addContent(" (" + md + ")");
                    mdrow.addCellContent(dcv.getValue());
                }

                // Show removals
                for (BulkEditMetadataValue dcv : removes)
                {
                    Row mdrow = mdchanges.addRow("deletion", Row.ROLE_DATA, "metadata-deletion");
                    String md = dcv.getSchema() + "." + dcv.getElement();
                    if (dcv.getQualifier() != null)
                    {
                        md += "." + dcv.getQualifier();
                    }
                    if (dcv.getLanguage() != null)
                    {
                        md += "[" + dcv.getLanguage() + "]";
                    }

                    Cell cell = mdrow.addCell();
                    cell.addContent(T_item_deletion);
                    cell.addContent(" (" + md + ")");
                    mdrow.addCellContent(dcv.getValue());
                }
            }
            Para actions = div.addPara();
            Button applychanges = actions.addButton("submit_confirm");
            applychanges.setValue(T_submit_confirm);
            Button cancel = actions.addButton("submit_return");
            cancel.setValue(T_submit_return);
        }
        else
        {
            Para nochanges = div.addPara();
            nochanges.addContent(T_no_changes);
            Para actions = div.addPara();
            Button cancel = actions.addButton("submit_return");
            cancel.setValue(T_submit_return);
        }

        div.addHidden("administrative-continue").setValue(knot.getId());
    }
}
