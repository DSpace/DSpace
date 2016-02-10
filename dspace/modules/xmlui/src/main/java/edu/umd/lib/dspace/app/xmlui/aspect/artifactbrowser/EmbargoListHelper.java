/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.artifactbrowser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cocoon.environment.Request;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/*
 * A Class to organize the SQL request to query for the embargoed items.
 *
 * @author Vivian Thayil
 */

public class EmbargoListHelper
{

    public static final String sql = "SELECT DISTINCT ON (h.handle) h.handle, i1.item_id, bs.bitstream_id, "
            + "(SELECT dc.text_value FROM metadatavalue dc "
            + "WHERE dc.metadata_field_id=? AND dc.resource_id=i1.item_id AND dc.resource_type_id=? LIMIT 1) as title, "
            + "(SELECT dc.text_value FROM metadatavalue dc "
            + "WHERE dc.metadata_field_id=? AND dc.resource_id=i1.item_id LIMIT 1) as advisor, "
            + "(SELECT dc.text_value FROM metadatavalue dc "
            + "WHERE dc.metadata_field_id=? AND dc.resource_id=i1.item_id LIMIT 1) as author, "
            + "(SELECT dc.text_value FROM metadatavalue dc "
            + "WHERE dc.metadata_field_id=? AND dc.resource_id=i1.item_id LIMIT 1) as department, "
            + "(SELECT dc.text_value FROM metadatavalue dc "
            + "WHERE dc.metadata_field_id=? AND dc.resource_id=i1.item_id LIMIT 1) as type, "
            + "rp.end_date "
            + "FROM  handle h, item i1, item2bundle i2b1, bundle2bitstream b2b1, bitstream bs, "
            + "resourcepolicy rp, epersongroup g, metadatavalue mv "
            + "WHERE h.resource_id=i1.item_id AND i1.item_id=i2b1.item_id AND i2b1.bundle_id=b2b1.bundle_id AND "
            + "b2b1.bitstream_id=bs.bitstream_id AND bs.bitstream_id=rp.resource_id AND (rp.end_date > CURRENT_DATE "
            + "OR rp.end_date IS NULL) AND rp.epersongroup_id = g.eperson_group_id AND "
            + "g.eperson_group_id = mv.resource_id AND mv.text_value = ?";

    public static List<TableRow> getEmbargoList(Request request)
            throws SQLException
            {
        Context context = UIUtil.obtainContext(request);

        final int titleId = MetadataField.findByElement(
                context,
                MetadataSchema.find(context, MetadataSchema.DC_SCHEMA)
                        .getSchemaID(), "title", null).getFieldID();

        final int advisorId = MetadataField.findByElement(
                context,
                MetadataSchema.find(context, MetadataSchema.DC_SCHEMA)
                        .getSchemaID(), "contributor", "advisor").getFieldID();

        final int authorId = MetadataField.findByElement(
                context,
                MetadataSchema.find(context, MetadataSchema.DC_SCHEMA)
                        .getSchemaID(), "contributor", "author").getFieldID();

        final int departmentId = MetadataField.findByElement(
                context,
                MetadataSchema.find(context, MetadataSchema.DC_SCHEMA)
                        .getSchemaID(), "contributor", "department")
                .getFieldID();

        final int typeId = MetadataField.findByElement(
                context,
                MetadataSchema.find(context, MetadataSchema.DC_SCHEMA)
                        .getSchemaID(), "type", null).getFieldID();

        final int itemResourceType = Constants.ITEM;

        final String groupName = "ETD Embargo";

        final Object[] params = { titleId, itemResourceType, advisorId,
                authorId, departmentId, typeId, groupName };

        TableRowIterator tri = DatabaseManager.query(context, sql, params);
        ArrayList<TableRow> rowList = (ArrayList<TableRow>) tri.toList();
        return rowList;
            }

}
