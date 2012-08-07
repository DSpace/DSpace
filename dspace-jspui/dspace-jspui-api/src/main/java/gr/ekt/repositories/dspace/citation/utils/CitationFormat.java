/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.citation.utils;

public class CitationFormat {
	private String citations;
	private String format;
	private String linebreak;
	
	public CitationFormat(String citations, String format, String outputformat) {
        this.citations = citations;
        this.format = format;
        if("html".equals(outputformat)){
        	this.linebreak = "<br>";
        }
        else
        	this.linebreak = "\n";
        
    }
	
	public void citationHandle(){
		if(this.format.equals("ris")){
			formatSpace();
			formatPages();
		}
		if(this.format.equals("ris") || this.format.equals("endnote")){
			formatAuthor();
			this.citations = this.citations.replaceAll("%0",this.linebreak+"%0");
			this.citations = this.citations.replaceAll("ER  - ","ER  - "+this.linebreak);
		}
	}
	private void formatSpace(){
		this.citations = this.citations.replaceAll("&nbsp;", " ");
	}
	private void formatAuthor(){
		this.citations = this.citations.replaceAll("A1  - ", this.linebreak+"A1  - ");
		this.citations = this.citations.replaceAll("%A ", this.linebreak+"%A ");
		this.citations = this.citations.replaceAll("%E ", this.linebreak+"%E ");
		this.citations = this.citations.replaceAll(this.linebreak+this.linebreak, this.linebreak);
		this.citations = this.citations.replaceAll("><br>",">");
	}
	
	private void formatPages(){
		String startTag = "SP  - ";
		String endTag = "EP  - ";
		String output = "";
		
		if(this.citations.indexOf(endTag)>0){
			String[] parts = this.citations.split(endTag);	
			String pagesStr = "";
			for(int i=0; i<parts.length; i++){
				if(parts[i].contains(startTag)){
					pagesStr = parts[i].substring(parts[i].indexOf(startTag)+startTag.length());
					output += parts[i].substring(0,parts[i].indexOf(startTag));
					if(pagesStr.indexOf("-")>0){
						String[] pages = pagesStr.split("-");
						output += startTag + pages[0] + this.linebreak + endTag + pages[1];
					}
					else
						output += startTag + pagesStr + this.linebreak;	
				}
				else{
					output += parts[i];
				}
			}
			this.citations = output;
		}		
	}
	
	public String getCitations() {
		return this.citations;
	}
}

