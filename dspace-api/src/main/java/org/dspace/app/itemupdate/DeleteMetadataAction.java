/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 *    Action to delete metadata 
 * 
 *
 */
public class DeleteMetadataAction extends UpdateMetadataAction {

	/**
	 *   Delete metadata from item
	 * 
	 * @param context DSpace Context
	 * @param itarch Item Archive
	 * @param isTest test flag
	 * @param suppressUndo undo flag
	 * @throws SQLException if database error
         * @throws AuthorizeException if authorization error
         * @throws ParseException if parse error
	 */
	@Override
    public void execute(Context context, ItemArchive itarch, boolean isTest,
            boolean suppressUndo) throws AuthorizeException, ParseException, SQLException {
		Item item = itarch.getItem();
		for (String f : targetFields)
		{
			DtoMetadata dummy = DtoMetadata.create(f, Item.ANY, "");
			List<MetadataValue> ardcv = itemService.getMetadataByMetadataString(item, f);

			ItemUpdate.pr("Metadata to be deleted: ");
			for (MetadataValue dcv : ardcv)
			{
				ItemUpdate.pr("  " + MetadataUtilities.getDCValueString(dcv));
			}

			if (!isTest)
			{
				if (!suppressUndo)
				{
					for (MetadataValue dcv : ardcv)
					{
                        MetadataField metadataField = dcv.getMetadataField();
                        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                        itarch.addUndoMetadataField(DtoMetadata.create(metadataSchema.getName(), metadataField.getElement(),
                                metadataField.getQualifier(), dcv.getLanguage(), dcv.getValue()));
					}
				}
				
                itemService.clearMetadata(context, item, dummy.schema, dummy.element, dummy.qualifier, Item.ANY);
			}
		}
	}
}
