package org.dspace.app.xmlui.utils;

import java.util.Map;

import org.apache.cocoon.components.flow.javascript.fom.FOM_Cocoon;;

public class FlowscriptUtils {

     public static Map getObjectModel(FOM_Cocoon cocoon) {
        return cocoon.getObjectModel();
    }
	
}
