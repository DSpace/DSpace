package ua.edu.sumdu.essuir.cache;


import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.DiscoverResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;


public class AuthorCache {

	public static Metadatum[] getLocalizedAuthors(Metadatum[] metadataValues, String locale) {
		ArrayList<Metadatum> newValues = new ArrayList<Metadatum>();
		TreeSet<String> res = new TreeSet<String>();
		
		String name;
		for (Metadatum author : metadataValues) {
			name = getAuthor(author.value).map(retrieved -> retrieved.getName(locale)).orElse(author.value);
			
			if (!res.contains(name)) {
				res.add(name);
				
				author.value = name;
				newValues.add(author);
			}
		}
		
		return newValues.toArray(new Metadatum[0]);
	}


	public static String getLocalizedAuthor(String name, String locale) {
		return getAuthor(name).map(author -> author.getName(locale)).orElse(name);
	}

	public static List<String> getLocalizedAuthors(List<String> authors, String locale) {
		return authors.stream()
				.map(name -> getLocalizedAuthor(name, locale))
				.distinct()
				.collect(Collectors.toList());
	}

	public static Optional<String> getOrcid(String authorName) {
		String res;

		checkUpdate();

		synchronized (authors) {
			Author a = authors.get(authorName);
			res = a != null ? a.getOrcid() : null;
		}

		return Optional.ofNullable(res);
	}

    public static void makeLocalizedAuthors(List<DiscoverResult.FacetResult> authors, String locale, int facetPage) {
        int offset = facetPage * 10;
        int remain = 11;
        ListIterator<DiscoverResult.FacetResult> it = authors.listIterator();
        while (it.hasNext()) {
            DiscoverResult.FacetResult author = it.next();
            String name = author.getDisplayedValue();
			String authorName = getAuthor(name).map(retrieved -> retrieved.getName(locale)).orElse(name);
            if (name.equals(authorName)) {
                if (offset == 0) {
                    if (remain > 0) {
                        remain--;
                    } else {
                        it.remove();
                    }
                } else {
                    offset--;
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }
	
	public static Optional<Author> getAuthor(String name) {
		checkUpdate();

		synchronized(authors) {
			return Optional.ofNullable(authors.get(name));
		}
	}
	
	public static void checkUpdate() {
		synchronized(authors) {
			long now = System.currentTimeMillis();
			
			if (now - lastUpdate > 300000) {
				update();
				
				lastUpdate = now;
			}
		}
	}
	
	
	public static void update() {
		authors.clear();
		
		try {
		    Connection c = null;
		    try {
		        Class.forName(ConfigurationManager.getProperty("db.driver"));
		    
		        c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
		                                        ConfigurationManager.getProperty("db.username"),
		                                        ConfigurationManager.getProperty("db.password"));
		
		        Statement s = c.createStatement();
		
		        ResultSet res = s.executeQuery(
			        		"SELECT * " +
			        		"FROM authors; "	
		        		);
		        
		        while (res.next()) {
		            Author author = new Author();
		            author.setName(res.getString("surname_uk") + ", " + res.getString("initials_uk"), "uk");
		            author.setName(res.getString("surname_en") + ", " + res.getString("initials_en"), "en");
		            author.setName(res.getString("surname_ru") + ", " + res.getString("initials_ru"), "ru");
		        	author.setOrcid(res.getString("orcid"));

		        	authors.put(author.getName("uk"), author);
		        	authors.put(author.getName("en"), author);
		        	authors.put(author.getName("ru"), author);
		        }
		
		        s.close();
		    } finally {
		        if (c != null) 
		            c.close();
		    }
		} catch (Exception e) {
			e.printStackTrace();		
		}
	}
	
	
	private static long lastUpdate = 0;
	private static Hashtable<String, Author> authors = new Hashtable<String, Author>();
}
