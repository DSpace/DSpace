/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Utilities that are needed in XSL transformations.
 *
 * @author Art Lowel (art dot lowel at atmire dot com)
 */
public class XSLUtils {
	  private static final Logger log = Logger.getLogger(XSLUtils.class);
    /*
     * Cuts off the string at the space nearest to the targetLength if there is one within
     * maxDeviation chars from the targetLength, or at the targetLength if no such space is
     * found
     */
    public static String shortenString(String string, int targetLength, int maxDeviation) {
        targetLength = Math.abs(targetLength);
        maxDeviation = Math.abs(maxDeviation);
        if (string == null || string.length() <= targetLength + maxDeviation)
        {
            return string;
        }


        int currentDeviation = 0;
        while (currentDeviation <= maxDeviation) {
            try {
                if (string.charAt(targetLength) == ' ')
                {
                    return string.substring(0, targetLength) + " ...";
                }
                if (string.charAt(targetLength + currentDeviation) == ' ')
                {
                    return string.substring(0, targetLength + currentDeviation) + " ...";
                }
                if (string.charAt(targetLength - currentDeviation) == ' ')
                {
                    return string.substring(0, targetLength - currentDeviation) + " ...";
                }
            } catch (Exception e) {
                //just in case
            }

            currentDeviation++;
        }

        return string.substring(0, targetLength) + " ...";

    }
    public static boolean isRevision(String cadenaId) {
	boolean isRevision=false;
	if(StringUtils.isNotBlank(cadenaId)){
	    String id=cadenaId.substring(cadenaId.lastIndexOf("=")+1);
	    String colectionRevisiones = ConfigurationManager.getProperty("openaire.coleccion.evaluaciones");
	    Context context=null;
	    try{
		context = new Context();
		Item item=Item.find(context, Integer.parseInt(id));
		Collection[] colecciones=item.getCollections();
		for(int i=0;i<colecciones.length;i++){
		    if (colecciones[i].getHandle().equalsIgnoreCase(colectionRevisiones)){
			isRevision=true;
			break;
		    }
		}
	    } catch (SQLException e) {
		log.error("Error comparando colecciones en XSLUtils.isRevision",e);
		e.printStackTrace();
	    }finally{
		if(context!=null){
		    context.abort();
		}
	    }
	}else{
	    isRevision=false;
	}

	return isRevision;
    }
    public static boolean isItem(String cadenaId) {
	boolean isItem=true;
	if(StringUtils.isNotBlank(cadenaId)){
	    String id=cadenaId.substring(cadenaId.lastIndexOf("=")+1);
	    String colectionRevisiones = ConfigurationManager.getProperty("openaire.coleccion.evaluaciones");
	    String colectionJuicios = ConfigurationManager.getProperty("openaire.coleccion.juicios");
	    Context context=null;
	    try{
		context = new Context();
		Item item=Item.find(context, Integer.parseInt(id));
		Collection[] colecciones=item.getCollections();
		for(int i=0;i<colecciones.length;i++){
		    if (colecciones[i].getHandle().equalsIgnoreCase(colectionRevisiones) || colecciones[i].getHandle().equalsIgnoreCase(colectionJuicios)){
			isItem=false;
			break;
		    }
		}
	    } catch (SQLException e) {
		log.error("Error comparando colecciones en XSLUtils.isItem",e);
		e.printStackTrace();
	    }finally{
		if(context!=null){
		    context.abort();
		}
	    }
	}else{
	    isItem=false;
	}

	return isItem;
    }

    public static String dcDateFormat(String dcDate, String format){
	Format formatter = new SimpleDateFormat("MMMM d, yyyy");
	return formatter.format(new DCDate(dcDate).toDate());
    }
}
