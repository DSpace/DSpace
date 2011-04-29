/*
 * MetadataImportConfirm.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
*/

package org.dspace.app.xmlui.aspect.administrative.metadataimport;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

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
import org.dspace.content.DCValue;
import org.xml.sax.SAXException;

import org.dspace.app.bulkedit.BulkEditChange;

/**
 * Web interface to Bulk Metadata Import app.
 * ported from org.dspace.app.webui.servlet.MetadataImportServlet
 *
 * Display summary of committed changes
 * 
 * @author Kim Shepherd
 */

public class MetadataImportConfirm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_trail = message("xmlui.administrative.metadataimport.general.trail");
    private static final Message T_changes = message("xmlui.administrative.metadataimport.general.changes");
    private static final Message T_new_item = message("xmlui.administrative.metadataimport.general.new_item");
    private static final Message T_no_changes = message("xmlui.administrative.metadataimport.general.no_changes");
	private static final Message T_title = message("xmlui.administrative.metadataimport.general.title");
	private static final Message T_head1 = message("xmlui.administrative.metadataimport.general.head1");

    private static final Message T_success = message("xmlui.administrative.metadataimport.MetadataImportConfirm.success");
    private static final Message T_changes_committed = message("xmlui.administrative.metadataimport.MetadataImportConfirm.changes_committed");
    private static final Message T_item_addition = message("xmlui.administrative.metadataimport.MetadataImportConfirm.item_added");
    private static final Message T_item_deletion = message("xmlui.administrative.metadataimport.MetadataImportConfirm.item_removed");
    private static final Message T_collection_newowner = message("xmlui.administrative.metadataimport.MetadataImportConfirm.collection_newowner");
    private static final Message T_collection_oldowner = message("xmlui.administrative.metadataimport.MetadataImportConfirm.collection_oldowner");
    private static final Message T_collection_mapped = message("xmlui.administrative.metadataimport.MetadataImportConfirm.collection_mapped");
    private static final Message T_collection_unmapped = message("xmlui.administrative.metadataimport.MetadataImportConfirm.collection_unmapped");


	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException
	{
		// Get list of changes

		Request request = ObjectModelHelper.getRequest(objectModel);
        ArrayList<BulkEditChange> changes = null;

        if(request.getAttribute("changes") != null)
        {
            changes = ((ArrayList<BulkEditChange>)request.getAttribute("changes"));
        }

		// DIVISION: metadata-import
		Division div = body.addInteractiveDivision("metadata-import",contextPath + "/admin/metadataimport", Division.METHOD_MULTIPART,"primary administrative");
		div.setHead(T_head1);
                Para para = div.addPara();
                para.addContent(T_success);
                para.addContent(" " + changes.size() + " ");
                para.addContent(T_changes);

                if(changes.size() > 0) {
                    Table mdchanges = div.addTable("metadata-changes", changes.size(), 2);


                    // Display the changes
                    int changeCounter = 0;
                    for (BulkEditChange change : changes)
                    {
                        // Get the changes
                        ArrayList<DCValue> adds = change.getAdds();
                        ArrayList<DCValue> removes = change.getRemoves();
                        ArrayList<Collection> newCollections = change.getNewMappedCollections();
                        ArrayList<Collection> oldCollections = change.getOldMappedCollections();

                        if ((adds.size() > 0) || (removes.size() > 0) ||
                            (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                            (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null))
                        {
                            Row headerrow = mdchanges.addRow(Row.ROLE_HEADER);

                            // Show the item
                            if (!change.isNewItem())
                            {
                                Item i = change.getItem();
                                Cell cell = headerrow.addCell();
                                cell.addContent(T_changes_committed);
                                cell.addContent(" " + i.getID() + " (" + i.getHandle() + ")");

                            }
                            else
                            {
                              headerrow.addCellContent(T_new_item);
                            }
                            headerrow.addCell();
                            changeCounter++;
                        }

                        // Show new owning collection
                        if (change.getNewOwningCollection() != null)
                        {
                            Collection c = change.getNewOwningCollection();
                            if (c != null)
                            {
                                String cHandle = c.getHandle();
                                String cName = c.getName();
                                Row colrow = mdchanges.addRow("addition",Row.ROLE_DATA,"metadata-addition");
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
                                Row colrow = mdchanges.addRow("deletion",Row.ROLE_DATA,"metadata-deletion");
                                colrow.addCellContent(T_collection_oldowner);
                                colrow.addCellContent(cHandle + " (" + cName + ")");
                            }
                        }

                        // Show new mapped collections
                        for (Collection c : newCollections)
                        {
                            String cHandle = c.getHandle();
                            String cName = c.getName();
                            Row colrow = mdchanges.addRow("addition",Row.ROLE_DATA,"metadata-addition");
                            colrow.addCellContent(T_collection_mapped);
                            colrow.addCellContent(cHandle + " (" + cName + ")");
                        }

                        // Show old mapped collections
                        for (Collection c : oldCollections)
                        {
                            String cHandle = c.getHandle();
                            String cName = c.getName();
                            Row colrow = mdchanges.addRow("deletion",Row.ROLE_DATA,"metadata-deletion");
                            colrow.addCellContent(T_collection_unmapped);
                            colrow.addCellContent(cHandle + " (" + cName + ")");
                        }

                        // Show additions
                        for (DCValue dcv : adds)
                        {
                            Row mdrow = mdchanges.addRow("addition",Row.ROLE_DATA,"metadata-addition");
                            String md = dcv.schema + "." + dcv.element;
                            if (dcv.qualifier != null)
                            {
                                md += "." + dcv.qualifier;
                            }
                            if (dcv.language != null)
                            {
                                md += "[" + dcv.language + "]";
                            }

                            Cell cell = mdrow.addCell();
                            cell.addContent(T_item_addition);
                            cell.addContent(" (" + md + ")");
                            mdrow.addCellContent(dcv.value);
                        }

                        // Show removals
                        for (DCValue dcv : removes)
                        {
                            Row mdrow = mdchanges.addRow("deletion",Row.ROLE_DATA,"metadata-deletion");
                            String md = dcv.schema + "." + dcv.element;
                            if (dcv.qualifier != null)
                            {
                                md += "." + dcv.qualifier;
                            }
                            if (dcv.language != null)
                            {
                                md += "[" + dcv.language + "]";
                            }

                            Cell cell = mdrow.addCell();
                            cell.addContent(T_item_deletion);
                            cell.addContent(" (" + md + ")");
                            mdrow.addCellContent(dcv.value);
                        }
                    }
                }
                else
                {
                    Para nochanges = div.addPara();
                    nochanges.addContent(T_no_changes);
                }
                Para actions = div.addPara();
                Button cancel = actions.addButton("submit_return");
                cancel.setValue(T_submit_return);

		
		div.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	

}
