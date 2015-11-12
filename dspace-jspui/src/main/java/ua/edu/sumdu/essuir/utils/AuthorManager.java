package ua.edu.sumdu.essuir.utils;


import org.dspace.core.ConfigurationManager;
import ua.edu.sumdu.essuir.statistics.AuthorStatInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;


public class AuthorManager {

	private static ArrayList<AuthorStatInfo> getTop30Author() {
		ArrayList<AuthorStatInfo> authors = new ArrayList<AuthorStatInfo>();
		
		try {
		    Connection c = null;
		    try {
		        Class.forName(ConfigurationManager.getProperty("db.driver"));
		    
		        c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
		                                        ConfigurationManager.getProperty("db.username"),
		                                        ConfigurationManager.getProperty("db.password"));
		
		        Statement s = c.createStatement();
		
		        ResultSet res = s.executeQuery(
			        		"SELECT text_value, SUM(sum) AS total_sum " +
			        		"FROM metadatavalue " +	
		        			" " +
			        		"RIGHT JOIN ( " +
			        		"	SELECT item_id, SUM(view_cnt) AS sum " +
			        		"		FROM statistics " +
			        		"		WHERE sequence_id > 0 " +
			        		"		GROUP BY item_id " +
			        		") AS stat ON stat.item_id = metadatavalue.resource_id " +
		        			" " +
			        		"WHERE metadata_field_id = 3 " +
                            /*
                                 remove not SSU authors from top10
                               - Litnarovich the first 22/12/2012
                            */
                            "AND (" +
                            "text_value != 'Літнарович, Руслан Миколайович'" +
                            "AND " +
                            "text_value != 'Litnarovych, Ruslan Mykolaiovych'" +
                            "AND " +
                            "text_value != 'Литнарович, Руслан Николаевич'" +
                            ")"+

                            "GROUP BY text_value " +
                            "ORDER BY total_sum DESC " +
                            "LIMIT 33 "
		        		);
		        
		        while (res.next()) {
		            authors.add(new AuthorStatInfo(res.getString("text_value"), res.getLong("total_sum")));
		        }
		
		        s.close();
		    } finally {
		        if (c != null) 
		            c.close();
		    }
		} catch (Exception e) {
			e.printStackTrace();		
		}
		
		return authors;
	}
	
	
	private static Hashtable<String, String> getAuthors(ArrayList<String> authors, String locale) {
		Hashtable<String, String> res = new Hashtable<String, String>();  

		try {
		    Connection c = null;
		    try {
		        Class.forName(ConfigurationManager.getProperty("db.driver"));
		    
		        c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
		                                        ConfigurationManager.getProperty("db.username"),
		                                        ConfigurationManager.getProperty("db.password"));
		
		        Statement s = c.createStatement();
		
		        StringBuilder sb = new StringBuilder();
		        
		        for (String name : authors) {
		        	sb.append("UNION (SELECT '" + name + "' AS name, surname_" + locale + " AS surname, initials_" + locale + " AS initials FROM authors " +
		        			  "WHERE " +
		        			  "(surname_uk || ', ' || initials_uk = '" + name + "') " +
		        			  		"OR " +
		        			  "(surname_ru || ', ' || initials_ru = '" + name + "') " +
		        			  		"OR " +
		        			  "(surname_en || ', ' || initials_en = '" + name + "') " +
		        			  		"LIMIT 1) ");
		        }
		        
		        ResultSet rs = s.executeQuery(sb.toString().substring(6));
		        
		        while (rs.next()) {
		            res.put(rs.getString("name"), 
		            		rs.getString("surname") + ", " + rs.getString("initials"));
		        }
		
		        s.close();
		    } finally {
		        if (c != null) 
		            c.close();
		    }
		} catch (Exception e) {
			e.printStackTrace();		
		}
		
		return res;
	}
	
	
	public static ArrayList<AuthorStatInfo> getTop10Author(String locale) {
		ArrayList<AuthorStatInfo> authors = getTop30Author();
		ArrayList<String> list = new ArrayList<String>();
		
		for (AuthorStatInfo author : authors) {
			list.add(author.getName());
		}
		
		Hashtable<String, String> names = getAuthors(list, locale);
		
		HashSet<String> visited = new HashSet<String>();
		ArrayList<AuthorStatInfo> res = new ArrayList<AuthorStatInfo>();
		
		for (AuthorStatInfo author : authors) {
			String name = names.get(author.getName());
			
			if (name == null)
				name = author.getName();
			
			if (!visited.contains(name)) {
				res.add(new AuthorStatInfo(name, author.getDownloads()));
				visited.add(name);
			}
		}
		
		return res;
	}
	
	
	
	
}
