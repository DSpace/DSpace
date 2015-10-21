/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager.plugins.adapter.decorators;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Bavo Van Geit
 * Date: 25/11/14
 * Time: 13:51
 */
public interface AttributeDecorator {

    public List<String> getApplicableSections();

    public List<Integer> getApplicableTypes();

    public void decorate(Context context, AttributeMap attributes, Item item, Bitstream bitstream, String fileID, String groupID, String admID, String section, int dsoType) throws SQLException;

}
