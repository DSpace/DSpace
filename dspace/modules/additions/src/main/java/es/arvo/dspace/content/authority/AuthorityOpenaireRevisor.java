/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package es.arvo.dspace.content.authority;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DB;

/**
 * @author Sergio Nieto Caramés
 * @author Adán Román Ruiz
 */
public class AuthorityOpenaireRevisor implements ChoiceAuthority
{
    private final static Logger log = Logger.getLogger(AuthorityOpenaireRevisor.class);		
	private static Connection connection= null;    
    
    public AuthorityOpenaireRevisor() {
	if(connection==null){
        	String driver = ConfigurationManager.getProperty("openaire.evaluador.driver");
        	String url = ConfigurationManager.getProperty("openaire.evaluador.url");
        	String username = ConfigurationManager.getProperty("openaire.evaluador.username");
        	String password = ConfigurationManager.getProperty("openaire.evaluador.password");
        	log.debug("Conexion a la base de datos: " + driver + ";" + url	+ ";" + username + ";" + password);
        	try {
		    connection = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
		    log.error("No se puede conectar a la bbdd de AuthorityOpenaireRevisor:",e);
		}
	}

    }
    //Saca un listado de coincidencias, a partir de una sql en el config
    public Choices getMatches(String field, String query, int collection, int start, int limit, String locale)
    {
	int dflt = -1;
	Vector<RevisorVO> persons = new Vector<RevisorVO>();
	persons = getPersons(query);

	Choice[] v = new Choice[persons.size()];

	for (int i = 0; i < persons.size(); ++i){
	    RevisorVO revisor=persons.get(i);
	    String label = revisor.getLabel();
	    String authority = revisor.getId();
	    String value = revisor.getValue();
	    v[i] = new Choice(authority, value, label);
	}
	return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }
    
    public Vector<RevisorVO> getPersons(String text) {
	    Vector<RevisorVO> persons = new Vector<RevisorVO>();
	    if(StringUtils.isEmpty(text)){
		return persons;
	    }
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    //		String querySoloApe=   "SELECT id, apellidos, nombre, centro FROM persona WHERE translate(lower(apellidos),'áéíóúñü','aeiounu') ILIKE translate(lower(?),'áéíóúñü','aeiounu') ORDER BY apellidos, nombre";
	    //		String queryApeYNombre="SELECT id, apellidos, nombre, centro FROM persona WHERE translate(lower(apellidos),'áéíóúñü','aeiounu') ILIKE translate(lower(?),'áéíóúñü','aeiounu') and translate(lower(nombre),'áéíóúñü','aeiounu') ILIKE translate(lower(?),'áéíóúñü','aeiounu') ORDER BY apellidos, nombre";
	    String queryTodasParcialesPre= " "+ ConfigurationManager.getProperty("openaire.sql.author.queryTodasParcialesPre") +" ";
	    String queryTodasParcialesRep= " "+ ConfigurationManager.getProperty("openaire.sql.author.queryTodasParcialesRep") +" ";
	    String queryTodasParcialesPost= " "+ ConfigurationManager.getProperty("openaire.sql.author.queryTodasParcialesPost") +" ";
	    StringBuffer query=new StringBuffer();
	    String[] textSplitted=text.split("\\s+|,\\s*");
	    try {
		try {
		    if (text.equals(""))
		    	return persons;
		    else if(textSplitted.length>0){
				query.append(queryTodasParcialesPre);
				for(int i=0;i<textSplitted.length;i++){
				    if(i!=0){
				    	query.append(" AND ");
				    }
				    query.append(queryTodasParcialesRep);
				}
				query.append(queryTodasParcialesPost);
				pstmt=connection.prepareStatement(query.toString());

				for(int i=0;i<textSplitted.length;i++){
				    pstmt.setString(i+1, textSplitted[i].trim());
				}
		    }

		    rs =pstmt.executeQuery();

		    try {
			while (rs.next()) {
			    RevisorVO apav=new RevisorVO();
			    apav.setFirstName(rs.getString("nombre"));
			    apav.setLastName(rs.getString("apellidos"));
			    apav.setEmail(rs.getString("email"));
			    apav.setId(rs.getString("id"));
			    persons.add(apav);
			}

		    } finally {
			if (rs != null)
			    rs.close();
		    }
		} catch (Exception ex) {
		    ex.printStackTrace();
		    log.error(ex);
		} finally {
		    if (pstmt != null)
			pstmt.close();
		}

	    } catch (SQLException ex) {
		log.error(ex);
	    }
	    return persons;
	}
    
    //Al añadir un nuevo metadato validado pasa por esta función que devuelve el autor 
    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
	  Vector<RevisorVO> persons = new Vector<RevisorVO>();
    	persons =getPersons(text);
    	
    	switch (persons.size()){
    		case 0:
    			return new Choices(Choices.CF_NOTFOUND);
    			
    		case 1:    
    			Choice[] v = new Choice[persons.size()];
  			RevisorVO revisor=persons.get(0);
    			String label = revisor.getLabel();
    			String authority = revisor.getId();
    			String value = revisor.getValue();
    			v[0] = new Choice(authority, value, label);
    			return new Choices(v,0,v.length, Choices.CF_UNCERTAIN,false, -1);
    		default:
    			return new Choices(Choices.CF_AMBIGUOUS);
    			
    	}
   
		
    }

    public String getLabel(String field, String key, String locale)
    {
//		String sql = ConfigurationManager.getProperty("select.nombreByCodigo");
//		String[] result;
//		try {
//		    result = db.executeQueryUnique(sql, key);
//		} catch (SQLException e) {
//			log.error("Fallo en consulta:"+sql+" Con clave de busqueda:"+key);
//		    e.printStackTrace();
//		    return "";
//		}
//		if(result!=null && result.length>1){
//		    String retorno=result[0]+", "+result[1];
//		    return retorno;
//		}else{
//		    log.warn("No se han encontrado datos de autoridad para el autor:"+key+" Usando la consulta:"+sql);
//		    return "";
//		}
	return "Completar el getLabel Del AuthorityOpenaireRevisor";
		
    }
    class RevisorVO{
	private String id;
	private String firstName;
	private String lastName;
	private String email;
	
	public String getId() {
	    return id;
	}
	public String getValue() {
	    return lastName+", "+firstName;
	}
	public String getLabel() {
	    return lastName+", "+firstName;
	}
	public void setId(String id) {
	    this.id = id;
	}
	public String getFirstName() {
	    return firstName;
	}
	public void setFirstName(String firstName) {
	    this.firstName = firstName;
	}
	public String getLastName() {
	    return lastName;
	}
	public void setLastName(String lastName) {
	    this.lastName = lastName;
	}
	public String getEmail() {
	    return email;
	}
	public void setEmail(String email) {
	    this.email = email;
	}
    }
    // Devuelve el mail de una autoridad
	public String getMail(String authority) {
		if(authority!=null){
			String queryTodasParcialesPost= " "+ ConfigurationManager.getProperty("openaire.sql.author.getMailFromId") +" ";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			String mail=null;
	
			try {
				pstmt=connection.prepareStatement(queryTodasParcialesPost);
				pstmt.setInt(1,Integer.parseInt(authority));
				rs =pstmt.executeQuery();
				
				if(rs.next()){
					mail= rs.getString(1);
				}
			} catch (SQLException e) {
				log.error("No se ha podido obtener el mail de la autoridad",e);
				e.printStackTrace();
			}finally{
				if(rs!=null){
					try {
						rs.close();
					} catch (SQLException e) {/*nada*/}
				}
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {/*nada*/}
				}
			}
			return mail;
		}else{
			return null;
		}
	}
}
