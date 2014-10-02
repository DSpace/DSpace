/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionHistoryForm extends AbstractDSpaceTransformer {
    /** Language strings */

    private static final Message T_head2 = message("xmlui.aspect.versioning.VersionHistoryForm.head2");
    private static final Message T_column1 = message("xmlui.aspect.versioning.VersionHistoryForm.column1");
    private static final Message T_column2 = message("xmlui.aspect.versioning.VersionHistoryForm.column2");
    private static final Message T_column3 = message("xmlui.aspect.versioning.VersionHistoryForm.column3");
    private static final Message T_column4 = message("xmlui.aspect.versioning.VersionHistoryForm.column4");
    private static final Message T_column5 = message("xmlui.aspect.versioning.VersionHistoryForm.column5");
    private static final Message T_column6 = message("xmlui.aspect.versioning.VersionHistoryForm.column6");
    private static final Message T_submit_update = message("xmlui.aspect.versioning.VersionHistoryForm.update");
    private static final Message T_submit_cancel = message("xmlui.aspect.versioning.VersionHistoryForm.return");
    private static final Message T_submit_delete = message("xmlui.aspect.versioning.VersionHistoryForm.delete");
    private static final Message T_legend = message("xmlui.aspect.versioning.VersionHistoryForm.legend");


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        boolean isItemView=parameters.getParameterAsInteger("itemID",-1) == -1;
        Item item = getItem();

        if(item==null || !AuthorizeManager.isAdmin(this.context, item.getOwningCollection()))
        {
            if(isItemView)
            {
                //Check if only administrators can view the item history
                if(new DSpace().getConfigurationService().getPropertyAsType("versioning.item.history.view.admin", false))
                {
                    return;
                }
            }else{
                //Only admins can delete versions
                throw new AuthorizeException();
            }
        }



        VersionHistory versionHistory = retrieveVersionHistory(item);
        if(versionHistory!=null)
        {
            Division main = createMain(body);
            createTable(main, versionHistory, isItemView, item);

            if(!isItemView)
            {
                addButtons(main, versionHistory);
                main.addHidden("versioning-continue").setValue(knot.getId());
            }

            Para note = main.addPara();
            note.addContent(T_legend);
        }
    }


    private Item getItem() throws WingException
    {
        try
        {
            if(parameters.getParameterAsInteger("itemID",-1) == -1)
            {
                DSpaceObject dso;
                dso = HandleUtil.obtainHandle(objectModel);
                if (!(dso instanceof Item))
                {
                    return null;
                }
                return (Item) dso;
            }else{
                return Item.find(context, parameters.getParameterAsInteger("itemID", -1));
            }
        } catch (SQLException e) {
            throw new WingException(e);
        }


    }

    private VersionHistory retrieveVersionHistory(Item item) throws WingException
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        return versioningService.findVersionHistory(context, item.getID());
    }


    private Division createMain(Body body) throws WingException
    {
        Division main = body.addInteractiveDivision("view-verion-history", contextPath+"/item/versionhistory", Division.METHOD_POST, "view version history");
        main.setHead(T_head2);
        return main;
    }

    private void createTable(Division main, VersionHistory history, boolean isItemView, Item item) throws WingException, SQLException
    {
        Table table = main.addTable("versionhistory", 1, 1);
        
        Row header = table.addRow(Row.ROLE_HEADER);
        if(!isItemView)
        {
            header.addCell().addContent("");
        }
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
        header.addCell().addContent(T_column4);
        header.addCell().addContent(T_column5);

        if(!isItemView)
        {
            header.addCell().addContent(T_column6);
        }

        if(history != null)
        {
            for(Version version : history.getVersions())
            {

                //Skip items currently in submission
                if(isItemInSubmission(version.getItem()))
                {
                    continue;
                }

                Row row = table.addRow(null, Row.ROLE_DATA,"metadata-value");
                if(!isItemView)
                {
                    CheckBox remove = row.addCell().addCheckBox("remove");
				    remove.setLabel("remove");
				    remove.addOption(version.getVersionId());
                }

                row.addCell().addContent(version.getVersionNumber());
                addItemIdentifier(row.addCell(), item, version);

                EPerson editor = version.getEperson();
                row.addCell().addXref("mailto:" + editor.getEmail(), editor.getFullName());
                row.addCell().addContent(new DCDate(version.getVersionDate()).toString());
                row.addCell().addContent(version.getSummary());


                if(!isItemView)
                {
                    row.addCell().addXref(contextPath + "/item/versionhistory?versioning-continue="+knot.getId()+"&versionID="+version.getVersionId() +"&itemID="+ version.getItem().getID() + "&submit_update", T_submit_update);
                }
            }
        }
    }


    private boolean isItemInSubmission(Item item) throws SQLException
    {
        WorkspaceItem workspaceItem = WorkspaceItem.findByItem(context, item);
        InProgressSubmission workflowItem;
        if(ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
        {
            workflowItem = XmlWorkflowItem.findByItem(context, item);
        }else{
            workflowItem = WorkflowItem.findByItem(context, item);
        }

        return workspaceItem != null || workflowItem != null;
    }

    private void addItemIdentifier(Cell cell, Item item, Version version) throws WingException
    {
        String itemHandle = version.getItem().getHandle();

        Metadatum[] identifiers = version.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        String itemIdentifier=null;
        if(identifiers!=null && identifiers.length > 0)
        {
            itemIdentifier = identifiers[0].value;
        }

        if(itemIdentifier!=null)
        {
            cell.addXref(contextPath + "/resource/" + itemIdentifier, itemIdentifier);
        }else{
            cell.addXref(contextPath + "/handle/" + itemHandle, itemHandle);
        }

        if(item.getID()==version.getItemID())
        {
            cell.addContent("*");
        }
    }

    private void addButtons(Division main, VersionHistory history) throws WingException {
        Para actions = main.addPara();

        if(history!=null && history.getVersions().size() > 0)
        {
            actions.addButton("submit_delete").setValue(T_submit_delete);
        }

        actions.addButton("submit_cancel").setValue(T_submit_cancel);		
    }
}
