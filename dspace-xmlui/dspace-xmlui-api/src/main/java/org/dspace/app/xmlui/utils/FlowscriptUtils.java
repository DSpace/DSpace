/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.util.Map;

import org.apache.cocoon.components.flow.javascript.fom.FOM_Cocoon;;

public class FlowscriptUtils {

     public static Map getObjectModel(FOM_Cocoon cocoon) {
        return cocoon.getObjectModel();
    }
	
}
