/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;

import org.apache.commons.jxpath.ri.compiler.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DB;
import org.dspace.content.DCDate;
import org.dspace.content.authority.AuthorityAuthor;

import java.text.Format;
import java.text.SimpleDateFormat;

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
    
    /*
     * FunciÃ³n que determina si un usuario pertenece a un grupo 
     */
    public static boolean isRegistered(String id) {
    	boolean result=false;
    	Context context=null;
    	if (id != "[]" && id != "") {
    		String groupName = ConfigurationManager.getProperty("dspace.disqus.group");
    		try {
    			context = new Context();
    			Group group = Group.findByName(context, groupName);
    			if (group != null) {
    				EPerson e = EPerson.find(context, Integer.parseInt(id));
    				result= group.isMember(e);
    			} 
    		} catch (SQLException e1) {
    			log.warn("Error en isRegistered",e1);
    			e1.printStackTrace();
    		}finally{
    			if(context!=null){
    	    		context.abort();
    	    	}
    		}
    	} 
    	return result;
    }

    public static boolean canMakeRevision(String idEperson,String cadenaId){
	//Anonimo
	if(StringUtils.isBlank(idEperson)){
	    return false;
	}

	Context context=null;
	try {
	    context=new Context();
	    EPerson currentUser=EPerson.find(context, Integer.parseInt(idEperson));
	    //Administrador
	    Group grupoAdministrador=Group.find(context, 1);
	    if(grupoAdministrador.isMember(currentUser)){
		return true;
	    }

	    //autor cualquier autor autoridad (desactivable en dspace.cfg)
	    if(ConfigurationManager.getBooleanProperty("openaire.boton.solicitarrevision.paratodoieo", false)){
		EPerson eperson=EPerson.find(context, Integer.parseInt(idEperson));
		if(eperson!=null){
		    String[] authority=DB.getInstance().executeQueryUnique(ConfigurationManager.getProperty("openaire.sql.author.getByEmail"), eperson.getEmail());
		    if(authority!=null){
			return true;
		    }
		}
	    }

	    //submitter
	    //	    if(item.getSubmitter().getID()==currentUser.getID()){
	    //		return true;
	    //	    }

	    //autor macheando nombre
	    String id=cadenaId.substring(cadenaId.lastIndexOf("=")+1);
	    Item item=Item.find(context, Integer.parseInt(id));
	    Metadatum[] autores = item.getMetadataByMetadataString("dc.contributor.author");

	    for(int i=0;i<autores.length;i++){
		if(autores[i].value.equalsIgnoreCase(currentUser.getFullName())){
		    return true;
		}
	    }
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}finally{
	    if(context!=null && context.isValid()){
		context.abort();
	    }
	}
	return false;
    }
    
    public static boolean canMakeJuicio(String idEperson,String cadenaId){
	//Anonimo No
	if(StringUtils.isBlank(idEperson)){
	    return false;
	}else{
	    return true;
	}


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
    
    public static String SEPARATOR=";";
    /**
	 * Devuelve autor, calificacion separados por ;
	 * @param reference
	 * @return
	 */
	public static String getDataFromReference(String reference){
		Context context=null;
		String handle=null;
		if(reference!=null && (reference.split("//")).length>=2){
			String resto=reference.substring(0,reference.lastIndexOf('/')-1);
			handle=reference.substring(resto.lastIndexOf('/')+1);
		}
		try {
			context = new Context();
			if(handle!=null){
				DSpaceObject obj=HandleManager.resolveToObject(context, handle);
				if(obj!=null){
					String author=obj.getMetadata("dc.contributor.author");
					String puntuacion=obj.getMetadata("oprm.clasificacion");
					String reputacion=obj.getMetadata("oprm.reputacion");
					StringBuffer resultado=new StringBuffer();
					
					resultado.append(author);
					if(puntuacion!=null){
					    resultado.append(SEPARATOR).append(puntuacion);					    
					}else{
					    resultado.append(SEPARATOR);
					}
					if(reputacion!=null){
					    resultado.append(SEPARATOR).append(reputacion);					    
					}else{
					    resultado.append(SEPARATOR);
					}
					return resultado.toString();
					
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(context!=null){
				context.abort();
			}
		}
		return "";
	}
	// PAra obtener los datos del item
	public static String getDataFromReferenceItem(String reference){
		Context context=null;
		String handle=null;
		if(reference!=null && (reference.split("//")).length>=2){
			String resto=reference.substring(0,reference.lastIndexOf('/')-1);
			handle=reference.substring(resto.lastIndexOf('/')+1);
		}
		try {
			context = new Context();
			if(handle!=null){
				DSpaceObject obj=HandleManager.resolveToObject(context, handle);
				if(obj!=null){
					String author=obj.getMetadata("dc.contributor.author");
					String puntuacion=obj.getMetadata("oprm.reputacion");
					if(puntuacion!=null){
						return author+SEPARATOR+puntuacion;					    
					}else{
					    return author;
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(context!=null){
				context.abort();
			}
		}
		return "";
	}
	
	public static String getData(String cadena,int pos){
		String[] datas=cadena.split(SEPARATOR);
		if(pos>=datas.length){
		    return "";
		}else{
		    return datas[pos];
		}
	}
	public static String getSvg(String cadena,int pos){
		String[] datas=cadena.split(SEPARATOR);
		if(pos>=datas.length){
		    return "<svg viewBox=\"0 0 30 30\" width=\"30\" height=\"30\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">"+
                                "<circle class=\"track\" stroke-dasharray=\"81.68140899333463\" stroke-dashoffset=\"0\" stroke-width=\"4\" r=\"13\" cy=\"15\" cx=\"15\" stroke-linecap=\"round\" stroke=\"#dddddd\" fill=\"none\"></circle>"+
                                "<circle class=\"score\" stroke-dasharray=\"0.000000, 81.681409\" stroke-dashoffset=\"20.420352248333657\" stroke-width=\"0\" r=\"13\" cy=\"15\" cx=\"15\" fill=\"none\" stroke-linecap=\"round\" stroke=\"#d9534f\"></circle>"+
                           "</svg>";
		}else{
		    int valoracion=Integer.parseInt(datas[pos]);
		    return getSvg(valoracion);
		}
	}
	public static String getSvg(int value){

	    int valoracion=value;
	    String color=null;
	    if(valoracion<30){
		color="#d9534f";
	    }else if(valoracion<60){
		color="#f0ad4e";
	    }else{
		color="#339977";
	    }
	    float constante=81.68140899333463f;
	    float offset=0;
	    if(valoracion!=0){
		offset=81.68140899333463f*((valoracion/100f));
	    }else{
		offset=0;
	    }
	    return "<svg viewBox=\"0 0 30 30\" width=\"30\" height=\"30\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">"+
	    "<circle class=\"track\" stroke-dasharray=\"81.68140899333463\" stroke-dashoffset=\"0\" stroke-width=\"4\" r=\"13\" cy=\"15\" cx=\"15\" stroke-linecap=\"round\" stroke=\"#dddddd\" fill=\"none\"></circle>"+
	    "<circle class=\"score\" stroke-dasharray=\""+offset+","+(constante-offset)+"\" stroke-dashoffset=\"20.420352248333657\" stroke-width=\"4\" r=\"13\" cy=\"15\" cx=\"15\" fill=\"none\" stroke-linecap=\"round\" stroke=\""+color+"\"></circle>"+
	    "</svg>";
	}
	
    public static String dcDateFormat(String dcDate, String format){
    	Format formatter = new SimpleDateFormat("MMMM d, yyyy");
    	return formatter.format(new DCDate(dcDate).toDate());
    }
}
