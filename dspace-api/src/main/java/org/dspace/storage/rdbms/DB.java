package org.dspace.storage.rdbms;

//Imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.AuthorProfile;
import org.dspace.core.ConfigurationManager;
import org.postgresql.util.PSQLException;

/**
 * @author Sergio Nieto Caramés
 * @author Adán Román Ruiz
 */

public class DB {
	public static DB db = null;
	// ICV
	private Connection autoritiesConection;
	private Connection dspaceConection;
	private final static Logger log = Logger.getLogger(DB.class);

	// Constructor

	private DB(String driver, String url, String username, String password,
			String driver2, String url2, String username2, String password2) {
		boolean isConnected = false;
		try {
			Class.forName(driver);
			dspaceConection = DriverManager.getConnection(url, username,
					password);
			autoritiesConection = DriverManager.getConnection(url2, username2,
					password2);
			if (dspaceConection != null && autoritiesConection != null) {
				isConnected = true;
			}
			System.out.println("isConnected? " + isConnected);
			log.debug("isConnected? " + isConnected);
		} catch (SQLException ex) {
			log.error(ex);
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			log.error(ex);
			ex.printStackTrace();
		} catch (Exception e) {
			log.error("Se ha producido una excepcion", e);
		}
	}

	public static DB getInstance() {
		if (db == null) {
			String driver = ConfigurationManager.getProperty("db.driver");
			String url = ConfigurationManager.getProperty("db.url");
			String username = ConfigurationManager.getProperty("db.username");
			String password = ConfigurationManager.getProperty("db.password");
			String driver2 = ConfigurationManager.getProperty("db2.driver");
			String url2 = ConfigurationManager.getProperty("db2.url");
			String username2 = ConfigurationManager.getProperty("db2.username");
			String password2 = ConfigurationManager.getProperty("db2.password");
			log.debug("Conexion a la base de datos: " + driver + ";" + url
					+ ";" + username + ";" + password);
			db = new DB(driver, url, username, password, driver2, url2,
					username2, password2);
		}
		return db;
	}

	public Vector getRevistas(String sql, String key) {
		Vector revistas = new Vector();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			try {
				pstmt = autoritiesConection.prepareStatement(sql);
				if (key.equals(""))
					return revistas;
				pstmt.setString(1, "%" + key + "%");
				rs = pstmt.executeQuery();
				int nCols = rs.getMetaData().getColumnCount();
				String[] values = new String[nCols];
				try {
					while (rs.next()) {
						for (int i = 0; i < nCols; i++) {
							if (rs.getString(i + 1) != null)
								values[i] = rs.getString(i + 1);
							else
								values[i] = "";
						}
						revistas.add(values[1] + " (" + values[0] + ")");

					}

				} finally {
					if (rs != null)
						rs.close();
					if (pstmt != null)
						pstmt.close();
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
		return revistas;
	}

	public Vector getPersons(String sql, String key) {
		Vector persons = new Vector();
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			try {
				stmt = autoritiesConection.createStatement();
				if (key.equals(""))
					return persons;
				else {
					// Solo nos interesa coger el campo apellidos
					String[] newKey = key.split(",");
					sql = sql.replace("?", "'" + newKey[0] + "'");
					// en caso de que lleve un segundo parámetro
					if (sql.contains("@") && newKey.length>1){
						sql = sql.replace("@", "'"	+ newKey[1].replaceAll("^\\s*","")+"'");
					}else{
						sql = sql.replace("@", "''");
					}
				}
				rs = stmt.executeQuery(sql);
				int nCols = rs.getMetaData().getColumnCount();
				String[] values = new String[nCols];
				try {
					while (rs.next()) {
						for (int i = 0; i < nCols; i++) {
							if (rs.getString(i + 1) != null)
								values[i] = rs.getString(i + 1);
							else
								values[i] = "";
						}
						persons.add(values[1] + ", " + values[2] + " ("
								+ values[0] + ")" + " | " + values[3]);

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
	
	/**
	 * No cerrar la conexion
	 * @return
	 */
	public final Connection getAutoritiesConection() {
	    return autoritiesConection;
	}
	/**
	 * No cerrar la conexion
	 * @param autoritiesConection
	 */
	public final Connection getDspaceConection() {
	    return dspaceConection;
	}

	public String[] executeQueryUnique(String sql, String... params)
			throws SQLException {
		String[] result = null;

		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {

			stmt = autoritiesConection.createStatement();
			for (int i = 0; i < params.length; i++) {
				sql = sql.replace("?", params[i]);
			}
			rs = stmt.executeQuery(sql);
			try {
				while (rs.next()) {
					int nCols = rs.getMetaData().getColumnCount();
					result = new String[nCols];
					for (int i = 0; i < nCols; i++) {
						result[i] = rs.getString(i + 1);
					}
				}
			} finally {
				if (rs != null)
					rs.close();
			}

		} catch (PSQLException ex) {
			log.error("Fallo en consulta:" + sql + " Con claves de busqueda:"
					+ params);
			ex.printStackTrace();
		} catch (SQLException ex) {
			log.error("Fallo en consulta:" + sql + " Con claves de busqueda:"
					+ params);
			ex.printStackTrace();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
		}
		return result;
	}

	public String select(String query) {
		String message = "";
		Statement st = null;
		ResultSet rs = null;
		try {
			st = autoritiesConection.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				message += rs.getString(1);// devuelve el primer parametro de la
											// peticion sql siempre que sea
											// String

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return message;
	}

	public boolean existTable(String tablename) throws SQLException {
		boolean result = false;

		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {

			stmt = autoritiesConection.createStatement();
			String sql = "SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_schema = 'public' AND table_name ilike '"
					+ tablename + "');";
			rs = stmt.executeQuery(sql);
			try {
				rs.next();
				result = rs.getBoolean(1);
			} finally {
				if (rs != null)
					rs.close();
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
		}
		return result;

	}

	// cerramos la conexión
	public void closeConnection() {
		if (autoritiesConection != null) {
			try {
				autoritiesConection.close();
			} catch (SQLException sqle) {
			}
		}
		if (autoritiesConection != null) {
			try {
				dspaceConection.close();
			} catch (SQLException sqle) {
			}
		}
	}

	public void createTable(String name, List<String> headings,
			List<Integer> sizes) {
		boolean result = false;

		Statement stmt = null;
		PreparedStatement pstmt = null;

		try {
			StringBuffer query = new StringBuffer();
			query.append("CREATE TABLE ").append(name).append("( ");
			for (int i = 0; i < headings.size(); i++) {
				if (i != 0) {
					query.append(",");
				}
				query.append(headings.get(i)).append(" ")
						.append("character varying(").append(sizes.get(i))
						.append(")");
			}
			query.append(");");
			StringBuffer owner = new StringBuffer("ALTER TABLE ")
					.append(name)
					.append(" OWNER TO ")
					.append(ConfigurationManager
							.getBooleanProperty("db.username")).append(";");

			stmt = autoritiesConection.createStatement();

			stmt.executeUpdate(query.toString());
			stmt.executeUpdate(owner.toString());

		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {

		}
	}

	public int executeUpdate(String insert) throws SQLException {
		int result = 0;
		Statement stmt = null;
		try {
			stmt = autoritiesConection.createStatement();
			result = stmt.executeUpdate(insert);
		} finally {

		}
		return result;
	}

	public int emptyTable(String tablename) throws SQLException {
		int result = 0;
		Statement stmt = null;
		try {
			String delete = "delete from " + tablename + ";";
			stmt = autoritiesConection.createStatement();
			result = stmt.executeUpdate(delete);
		} finally {

		}
		return result;
	}

	public HashMap<String, String> getDatatypes(String tablename) {

		HashMap<String, String> datosColumnas = new HashMap<String, String>();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = autoritiesConection.createStatement();
			rs = st.executeQuery("select column_name, data_type from information_schema.columns where table_name ilike '"
					+ tablename + "';");
			while (rs.next()) {
				datosColumnas.put(rs.getString(1), rs.getString(2));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return datosColumnas;
	}

	public ArrayList<String> getTableNames() {

		ArrayList<String> tablenames = new ArrayList<String>();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = autoritiesConection.createStatement();
			rs = st.executeQuery("SELECT tablename FROM pg_catalog.pg_tables where schemaname='public';");
			while (rs.next()) {
				tablenames.add(rs.getString(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return tablenames;
	}

	public ArrayList<ArrayList<String>> getFullTable(String tableName)
			throws SQLException {
		ArrayList<ArrayList<String>> filas = new ArrayList<ArrayList<String>>();
		;

		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {

			stmt = autoritiesConection.createStatement();

			rs = stmt.executeQuery("select * from " + tableName);
			try {
				// nombres de columnas
				ArrayList<String> fila = new ArrayList<String>();
				int nCols = rs.getMetaData().getColumnCount();
				for (int i = 0; i < nCols; i++) {
					fila.add(rs.getMetaData().getColumnLabel(i + 1));
				}
				filas.add(fila);
				while (rs.next()) {
					fila = new ArrayList<String>();
					for (int i = 0; i < nCols; i++) {
						fila.add(rs.getString(i + 1));
					}
					filas.add(fila);
				}
			} finally {
				if (rs != null)
					rs.close();
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
		}
		return filas;
	}

	public int getMaxIdSipi() throws SQLException {

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = dspaceConection
					.prepareStatement("select max(to_number(mv.text_value,'99999999999')) from metadatavalue mv, metadatafieldregistry mf where mv.metadata_field_id=mf.metadata_field_id and mf.element='identifier' and mf.qualifier='sipi'");

			rs = pstmt.executeQuery();
			while (rs.next()) {

				if (rs.getString(1) != null)
					return rs.getInt(1);
			}

		} finally {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
		}
		return 0;
	}
	
	public DatosPersona getDatosPersona(String sql, String... params) throws SQLException {

	    DatosPersona datosPersona=new DatosPersona();
	    Statement stmt = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {

		stmt = autoritiesConection.createStatement();
		for(int i=0;i<params.length;i++){
		    sql = sql.replace("?", params[i]);
		}		    
		rs = stmt.executeQuery(sql);
		try {
		    while (rs.next()) {
			datosPersona.orcid=rs.getString("id_orcid");    			
			datosPersona.googleScholar=rs.getString("id_google");
			datosPersona.researcherID= rs.getString("id_researcher");
			datosPersona.scopusID= rs.getString("id_scopus");
			datosPersona.dialnet= rs.getString("id_dialnet");
		    }
		} finally {
		    if (rs != null)
			rs.close();
		}

	    } catch (SQLException ex) {
		ex.printStackTrace();
	    } finally {
		if (pstmt != null){
		    pstmt.close();
		}
	    }
	    return datosPersona;
	}

	public List<DatosPersona> getAutoridades() throws SQLException {
	    log.info("entrando en getAutoridades(");
	    List<DatosPersona> personas=new ArrayList<DatosPersona>();
	    Statement stmt = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
		stmt = autoritiesConection.createStatement();
		rs = stmt.executeQuery(ConfigurationManager.getProperty("authorProfile.getAutoridades.sql"));
		log.info("Conexion a autoridades valida? "+autoritiesConection.toString());
		log.info("stmt:"+stmt);
		try {
		    while (rs.next()) {
			log.info("Procesando autoridad "+rs.getInt("id"));
			personas.add(poblarPersona(rs));
		    }
		} finally {
		    if (rs != null)
			rs.close();
		}

	    } catch (SQLException ex) {
		log.info("Error en getAutoridades:"+ex);
		ex.printStackTrace();
	    } finally {
		if (pstmt != null){
		    pstmt.close();
		}
	    }
	    return personas;
	}
	
	private DatosPersona poblarPersona(ResultSet rs) throws SQLException {
	    DatosPersona persona=new DatosPersona();
	    persona.setId(rs.getInt("id"));
	    persona.setApellidos(rs.getString("apellidos"));
	    persona.setCentro(rs.getString("centro"));
	    persona.setDialnet(rs.getString("id_dialnet"));
	    persona.setEmail(rs.getString("email"));
	    persona.setGoogleScholar(rs.getString("id_google"));
	    persona.setNombre(rs.getString("nombre"));
	    persona.setOrcid(rs.getString("id_orcid"));
	    persona.setResearcherID(rs.getString("id_researcher"));
	    persona.setScopusID(rs.getString("id_scopus"));
	   
	    return persona;
	}




	public static  class DatosPersona {

		public String orcid;		
		public String googleScholar;
		public String researcherID;
		public String scopusID;
		public String dialnet;

		public int id;
		public String nombre;
		public String apellidos;
		public String centro;
		public String email;
		
		public DatosPersona() {

		}

		public DatosPersona(String orcid, String googleScholar,String researcherID, String scopusID, String dialnet) {
			this.orcid = orcid;			
			this.googleScholar = googleScholar;
			this.researcherID = researcherID;
			this.scopusID = scopusID;
			this.dialnet = dialnet;
		}
		
		public DatosPersona(AuthorProfile authorProfile) {
		    if(authorProfile!=null){
			this.orcid= authorProfile.getMetadata("authorProfile.id.orcid");
			this.googleScholar = authorProfile.getMetadata("authorProfile.id.google");
			this.researcherID = authorProfile.getMetadata("authorProfile.id.researcher");
			this.scopusID = authorProfile.getMetadata("authorProfile.id.scopus");
			this.dialnet = authorProfile.getMetadata("authorProfile.id.dialnet");
			
			String id=authorProfile.getMetadata("authorProfile.authority.id");
			Integer integerId=null;
			try {
			    if(StringUtils.isNotBlank(id)){
				integerId = Integer.parseInt(id);
			    }
			} catch (NumberFormatException e) {
			    //nada
			}
			if(integerId!=null){
			    this.id=integerId;
			}
			
			this.nombre=authorProfile.getMetadata("authorProfile.name.first");
			this.apellidos=authorProfile.getMetadata("authorProfile.name.last");
			this.centro=authorProfile.getMetadata("authorProfile.department.name");
			//firma
			this.email=authorProfile.getMetadata("authorProfile.email");
		    }
		}

		public String getResearcherID() {
			return researcherID;
		}

		public void setResearcherID(String researcherID) {
			this.researcherID = researcherID;
		}

		public String getScopusID() {
			return scopusID;
		}

		public void setScopusID(String scopusID) {
			this.scopusID = scopusID;
		}

		public String getDialnet() {
			return dialnet;
		}

		public void setDialnet(String dialnet) {
			this.dialnet = dialnet;
		}

		public String getGoogleScholar() {
			return googleScholar;
		}

		public void setGoogleScholar(String googleScholar) {
			this.googleScholar = googleScholar;
		}

		public String getOrcid() {
			return orcid;
		}

		public void setOrcid(String orcid) {
			this.orcid = orcid;
		}

		public int getId() {
		    return id;
		}

		public void setId(int id) {
		    this.id = id;
		}

		public String getNombre() {
		    return nombre;
		}

		public void setNombre(String nombre) {
		    this.nombre = nombre;
		}

		public String getApellidos() {
		    return apellidos;
		}

		public void setApellidos(String apellidos) {
		    this.apellidos = apellidos;
		}

		public String getCentro() {
		    return centro;
		}

		public void setCentro(String centro) {
		    this.centro = centro;
		}

		public String getEmail() {
		    return email;
		}

		public void setEmail(String email) {
		    this.email = email;
		}	
		
		// Compuesto
		public String getAutor(){
		    if(StringUtils.isNotBlank(nombre) && StringUtils.isNotBlank(apellidos)){
			return apellidos+", "+nombre;
		    }else{
			if(StringUtils.isNotBlank(apellidos)){
			    return apellidos;
			}else{
			    return nombre;
			}
		    }
		}

		@Override
		public String toString() {
		    StringBuffer s=new StringBuffer();
		    s.append("orcid:").append(orcid).append("\n");
		    s.append("googleScholar:").append(googleScholar).append("\n");
		    s.append("researcherID:").append(researcherID).append("\n");
		    s.append("scopusID:").append(scopusID).append("\n");
		    s.append("dialnet:").append(dialnet).append("\n");
		    s.append("id:").append(id).append("\n");
		    s.append("nombre:").append(nombre).append("\n");
		    s.append("apellidos:").append(apellidos).append("\n");
		    s.append("centro:").append(centro).append("\n");
		    s.append("email:").append(email).append("\n");
		    
		    return s.toString();
		}
    		

	}

}// class

