/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.tagcloud;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.log4j.Logger;
import org.dspace.discovery.configuration.TagCloudConfiguration;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.mcavallo.opencloud.Cloud.Case;

/**
 * @author kstamatis
 *
 */
public class TagCloudTag extends SimpleTagSupport{

	private static Logger log = Logger.getLogger(TagCloudTag.class);

	TagCloudConfiguration parameters;
	Map<String, Integer> data;
	String index;
	String scope;
	String type = "0"; // 0=facet, 1=browse

	/**
	 * 
	 */
	public TagCloudTag() {
		// TODO Auto-generated constructor stub
	}

	public void doTag() throws JspException { 

		PageContext pageContext = (PageContext) getJspContext(); 
		JspWriter out = pageContext.getOut(); 

		if (parameters == null)
			parameters = new TagCloudConfiguration();

		try {
			Cloud cloud = new Cloud();  // create cloud 
			if (parameters.getCloudCase().equals("Case.LOWER"))
				cloud.setTagCase(Case.LOWER);
			else if (parameters.getCloudCase().equals("Case.UPPER"))
				cloud.setTagCase(Case.UPPER);
			else if (parameters.getCloudCase().equals("Case.CAPITALIZATION"))
				cloud.setTagCase(Case.CAPITALIZATION);
			else if (parameters.getCloudCase().equals("Case.PRESERVE_CASE"))
				cloud.setTagCase(Case.PRESERVE_CASE);
			else if (parameters.getCloudCase().equals("Case.CASE_SENSITIVE"))
				cloud.setTagCase(Case.CASE_SENSITIVE);
			cloud.setMaxWeight(Double.parseDouble(parameters.getFontTo()));   // max font size
			cloud.setMinWeight(Double.parseDouble(parameters.getFontFrom()));
			if (parameters.getTotalTags()==-1)
				cloud.setMaxTagsToDisplay(1000000);
			else
				cloud.setMaxTagsToDisplay(parameters.getTotalTags());
			
			for (String subject : data.keySet()){
				if (data.get(subject).intValue() > Integer.parseInt(parameters.getCuttingLevel())){
					for (int i=0; i<data.get(subject).intValue(); i++){
						Tag tag2 = new Tag(subject, ((HttpServletRequest) pageContext.getRequest()).getContextPath()+(scope!=null?scope:"")+(type.equals("0")?("/simple-search?filterquery="+URLEncoder.encode(subject,"UTF-8")+"&filtername="+index+"&filtertype=equals"):("/browse?type="+index+"&value="+subject)));
						cloud.addTag(tag2); 
					}
				}
			}

			out.println("<div class=\"tagcloud\">");
			int counter = 0;

			List<Tag> tagList = cloud.tags(new Tag.NameComparatorAsc());
			if (parameters.getOrdering().equals("Tag.NameComparatorAsc"))
				tagList = cloud.tags(new Tag.NameComparatorAsc());
			else if (parameters.getOrdering().equals("Tag.NameComparatorDesc"))
				tagList = cloud.tags(new Tag.NameComparatorDesc());
			else if (parameters.getOrdering().equals("Tag.ScoreComparatorAsc"))
				tagList = cloud.tags(new Tag.ScoreComparatorAsc());
			else if (parameters.getOrdering().equals("Tag.ScoreComparatorDesc"))
				tagList = cloud.tags(new Tag.ScoreComparatorDesc());

			for (Tag tag : tagList) { 

				String tagClass = "";
				
				if (parameters.isRandomColors()){
					if (counter==0){
						tagClass = "tagcloud_1";
					}
					else if (counter==1){
						tagClass = "tagcloud_2";
					}
					else if (counter==2){
						tagClass = "tagcloud_3";
					}
				}
				else {
					if (tag.getNormScore()>0.3f){
						tagClass = "tagcloud_1";
					}
					else if (tag.getNormScore()>0.2f){
						tagClass = "tagcloud_2";
					}
					else if (tag.getNormScore()>0.1f){
						tagClass = "tagcloud_3";
					}
				}

				String scoreSup = "";
				if (parameters.isDisplayScore()){
					scoreSup = "<span class=\"tag_sup\"><sup>("+tag.getScoreInt()+")</sup></span>";
				}
				
				out.println("<a class=\""+tagClass+"\" href=\"" + tag.getLink().replace(" & ", " %26 ") +"\" style=\"font-size: "+ tag.getWeight() +"em;\">"+ tag.getName() + scoreSup +"</a>");


				counter ++;
				if (counter == 3) 
					counter = 0;
			}
			out.println("</div>");

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(TagCloudConfiguration parameters) {
		this.parameters = parameters;
	}

	public void setData(Map<String, Integer> data) {
		this.data = data;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public void setType(String type) {
		this.type = type;
	}
}