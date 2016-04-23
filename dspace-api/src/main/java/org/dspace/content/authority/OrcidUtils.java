/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.AuthorProfile;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.AuthorProfileUtil;
import org.dspace.storage.rdbms.DB;
import org.dspace.storage.rdbms.DB.DatosPersona;


/**
 * @author Adán Román Ruiz
 * @author Sergio Nieto Caramés
 */
public class OrcidUtils{
    private static Logger log = Logger.getLogger(OrcidUtils.class);
    
    static Context context;
    static{
	try {
	    context= new Context();
	} catch (SQLException e) {
	    e.printStackTrace();
	    log.error("Error inicializando OrcidUtils",e);
	}
    }
	
    static Hashtable<String,DatosPersona> idsCache=new Hashtable<String,DatosPersona>();

    
    /**
     * Obtiene el idOrcid a partir del codigo de autoridad
     */
//    public static String getIdOrcid(String clave) {
//    	boolean cacheDisabled= ConfigurationManager.getBooleanProperty("disableIdOrcidCache", false);	
//    	if(!cacheDisabled && idsCache.containsKey(clave)){
//    		return idsCache.get(clave).orcid;
//    	}
//    	DB db = DB.getInstance();
//    	if(StringUtils.isNotBlank(clave)){
//
//    		String sql= ConfigurationManager.getProperty("select.idOrcidByCodigo");
//    		DatosPersona datosPersona = null;
//    		try {
//    			datosPersona = db.getDatosPersona(sql, clave);
//    		} catch (SQLException e) {
//    			log.warn("ERROR Obteniendo Id de Orcid:"+e.getMessage(),e);
//    			return "";
//    		}
//    		if(datosPersona != null){
//    			idsCache.put(clave, datosPersona);
//    			return datosPersona.orcid;
//    		}
//    	}
//    	idsCache.put(clave,db.new DatosPersona("","","","", null));
//    	return "";
//    }
    private static DatosPersona getDatosPersona(String clave){
	boolean cacheDisabled= ConfigurationManager.getBooleanProperty("disableIdOrcidCache", false);	
	if(!cacheDisabled && idsCache.containsKey(clave)){
	    return idsCache.get(clave);
	}
	if(StringUtils.isNotBlank(clave)){
	    //Ojo que el id es de profile, no de autoridad
	    AuthorProfile authorProfile;
	    try {
		context.turnOffAuthorisationSystem();
		
		authorProfile = AuthorProfileUtil.findAuthorProfileByAuthorityId(context, clave);
		if(authorProfile==null){
		    // Lo intentamos por el nombre. Puede que se introdujese a pelo en el interfaz de perfil de autor
		    
		}
		DatosPersona datosPersona = new DatosPersona(authorProfile);
		if(datosPersona != null){
		    idsCache.put(clave, datosPersona);
		
		    return datosPersona;
		}
	    } catch (NumberFormatException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }finally{
		 context.restoreAuthSystemState();
	    }
	}
	idsCache.put(clave,new DatosPersona("","","","", null));
	return idsCache.get(clave);
    }

    public static String getIdOrcid(String clave) {
	DatosPersona datosPersona=getDatosPersona(clave);
	return datosPersona.orcid;
    }

    public static String getIdScholar(String clave) {
	DatosPersona datosPersona=getDatosPersona(clave);
	return datosPersona.googleScholar;
    }

    public static String getIdResearcher(String clave) {
	DatosPersona datosPersona=getDatosPersona(clave);
	return datosPersona.researcherID;
    }

    public static String getIdScopus(String clave) {
	DatosPersona datosPersona=getDatosPersona(clave);
	return datosPersona.scopusID;
    }

    public static String getIdDialnet(String clave) {
	DatosPersona datosPersona=getDatosPersona(clave);
	return datosPersona.dialnet;
    }

    public static String getIdFromName(String filter) {
	boolean cacheDisabled= ConfigurationManager.getBooleanProperty("disableIdOrcidCache", false);	
	if(StringUtils.isNotBlank(filter)){
	    AuthorProfile authorProfile=null;
	    try {
		context.turnOffAuthorisationSystem();

		try {
		    authorProfile = AuthorProfileUtil.findAuthorProfile(context, filter);
		} catch (NullPointerException e) {/* No pasa nada*/}
		if(authorProfile==null){
		    return null;			    
		}
		DatosPersona datosPersona = new DatosPersona(authorProfile);
		if(datosPersona != null){
		    idsCache.put(""+datosPersona.getId(), datosPersona);

		    return ""+datosPersona.getId();
		}
	    } catch (NumberFormatException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }finally{
		context.restoreAuthSystemState();
	    }
	}
	return null;
    }
}
