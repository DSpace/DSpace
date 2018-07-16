package ua.edu.sumdu.essuir.cache;


import java.util.TreeMap;


public class Author {

	public String getName(String locale) {
		return names.get(locale);
	}
	
	
	public void setName(String name, String locale) {
		names.put(locale, name);
	}

	public String getOrcid() {
		return orcid;
	}

	public void setOrcid(String orcid) {
		this.orcid = orcid;
	}

	private TreeMap<String, String> names = new TreeMap<String, String>();
	private String orcid;
}
