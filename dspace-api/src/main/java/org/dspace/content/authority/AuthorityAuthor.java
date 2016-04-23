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
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DB;

/**
 * @author Sergio Nieto Caramés
 * @author Adán Román Ruiz
 */
public class AuthorityAuthor implements ChoiceAuthority
{
    private final static Logger log = Logger.getLogger(AuthorityAuthor.class);		
	private DB db= null;    
    
    public AuthorityAuthor() {

	    // Maak een connectie aan uit de DB class
	    db = DB.getInstance();


    }
    //Saca un listado de coincidencias, a partir de una sql en el config
    public Choices getMatches(String field, String query, int collection, int start, int limit, String locale)
    {
    	int dflt = -1;
		Vector persons = new Vector();
		String sql= ConfigurationManager.getProperty("db2.sql");
		persons = db.getPersons(sql,query);
		
		Choice[] v = new Choice[persons.size()];
	
		for (int i = 0; i < persons.size(); ++i){
			String label = (String) persons.elementAt(i);
			String authority = label.substring(label.lastIndexOf("(") + 1, label.lastIndexOf(")"));
			String value = label.substring(0, label.lastIndexOf("("));
			v[i] = new Choice(authority, value, label);
		}
		return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }
    
    //Al añadir un nuevo metadato validado pasa por esta función que devuelve el autor 
    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
    	String sql= ConfigurationManager.getProperty("db2.sql.best.match");
    	Vector persons = new Vector();
    	persons = db.getPersons(sql,text);
    	
    	switch (persons.size()){
    		case 0:
    			return new Choices(Choices.CF_NOTFOUND);
    			
    		case 1:    
    				Choice[] v = new Choice[persons.size()];
    				String label = (String) persons.elementAt(0);
    				String authority = label.substring(label.lastIndexOf("(") + 1, label.lastIndexOf(")"));
    				String value = label.substring(0, label.lastIndexOf("("));
    				v[0]=new Choice(authority, value, label);
    				return new Choices(v,0,v.length, Choices.CF_UNCERTAIN,false, -1);
    			
    		default:
    			return new Choices(Choices.CF_AMBIGUOUS);
    			
    	}
   
		
    }

    public String getLabel(String field, String key, String locale)
    {
		String sql = ConfigurationManager.getProperty("select.nombreByCodigo");
		String[] result;
		try {
		    result = db.executeQueryUnique(sql, key);
		} catch (SQLException e) {
			log.error("Fallo en consulta:"+sql+" Con clave de busqueda:"+key);
		    e.printStackTrace();
		    return "";
		}
		if(result!=null && result.length>1){
		    String retorno=result[0]+", "+result[1];
		    return retorno;
		}else{
		    log.warn("No se han encontrado datos de autoridad para el autor:"+key+" Usando la consulta:"+sql);
		    return "";
		}
		
    }
}
