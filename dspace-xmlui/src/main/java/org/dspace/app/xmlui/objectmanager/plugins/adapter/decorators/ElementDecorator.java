/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager.plugins.adapter.decorators;

import org.dspace.app.xmlui.objectmanager.AbstractAdapter;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Bavo Van Geit
 * Date: 25/11/14
 * Time: 13:51
 */
public interface ElementDecorator {

    public List<String> getApplicableSections();

    public List<Integer> getApplicableTypes();

    public void decorate(Context context, AbstractAdapter adapter, String section, int dsoType) throws SQLException, SAXException;

}
