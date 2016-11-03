/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

public class EditItem implements InProgressSubmission {
	private Item item;

	public EditItem(Item item) {
		this.item = item;
	}

	@Override
	public int getID() {
		return item.getID();
	}

	@Override
	public void deleteWrapper() throws SQLException, IOException, AuthorizeException {
		// nothing to delete
		return;
	}

	@Override
	public void update() throws SQLException, AuthorizeException {
		item.update();
	}

	@Override
	public Item getItem() {
		return item;
	}

	@Override
	public Collection getCollection() {
		try {
			return item.getParentObject();
		} catch (SQLException e) {
			throw new RuntimeException();
		}
	}

	@Override
	public EPerson getSubmitter() throws SQLException {
		return item.getSubmitter();
	}

	@Override
	public boolean hasMultipleFiles() {
		return true;
	}

	@Override
	public void setMultipleFiles(boolean b) {
	}

	@Override
	public boolean hasMultipleTitles() {
		return true;
	}

	@Override
	public void setMultipleTitles(boolean b) {
	}

	@Override
	public boolean isPublishedBefore() {
		return true;
	}

	@Override
	public void setPublishedBefore(boolean b) {
	}

}
