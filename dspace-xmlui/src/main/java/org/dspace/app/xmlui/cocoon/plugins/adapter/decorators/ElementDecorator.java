/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon.plugins.adapter.decorators;

import java.sql.*;
import java.util.*;
import org.dspace.app.xmlui.objectmanager.*;
import org.dspace.core.*;
import org.xml.sax.*;

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
