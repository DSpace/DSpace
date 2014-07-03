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


			//cloud.setNormThreshold(0.4);

			
			for (String subject : data.keySet()){
				if (data.get(subject).intValue() > Integer.parseInt(parameters.getCuttingLevel())){
					for (int i=0; i<data.get(subject).intValue(); i++){
						Tag tag2 = new Tag(subject, ((HttpServletRequest) pageContext.getRequest()).getContextPath()+(scope!=null?scope:"")+(type.equals("0")?("/simple-search?filterquery="+URLEncoder.encode(subject,"UTF-8")+"&filtername="+index+"&filtertype=equals"):("/browse?type="+index+"&value="+subject)));   // creates a tag
						//tag2.setScore(subjects.get(subject).doubleValue());
						cloud.addTag(tag2); 
					}
				}
			}

			out.println("<div class=\"tagcloud\" style=\"width:"+parameters.getWidth()+";"+(parameters.isShouldCenter()?"text-align:center":"")+"\">");
			int counter = 0;

			List<Tag> tagList = cloud.tags(new Tag.NameComparatorAsc());
			if (parameters.getOrdering().equals("Tag.NameComparatorAsc"))
				tagList = cloud.tags(new Tag.NameComparatorAsc());
			else if (parameters.getOrdering().equals("Tag.NameComparatorDesc"))
				tagList = cloud.tags(new Tag.NameComparatorDesc());
			//else if (parameters.ordering.equals("Tag.GreekNameComparatorAsc"))
			//	tagList = cloud.tags(new GreekComparatorTagAsc());			
			//else if (parameters.ordering.equals("Tag.GreekNameComparatorDesc"))
			//	tagList = cloud.tags(new GreekComparatorTagDesc());
			else if (parameters.getOrdering().equals("Tag.ScoreComparatorAsc"))
				tagList = cloud.tags(new Tag.ScoreComparatorAsc());
			else if (parameters.getOrdering().equals("Tag.ScoreComparatorDesc"))
				tagList = cloud.tags(new Tag.ScoreComparatorDesc());

			for (Tag tag : tagList) { 

				String colorPart = "";
				String theColor = "";
				String weightPart = "";

				if (parameters.isRandomColors()){
					if (counter==0){
						colorPart = "color:#"+parameters.getColorLevel1();
						theColor = parameters.getColorLevel1();
						weightPart = "font-weight:"+parameters.getWeightLevel1();
					}
					else if (counter==1){
						colorPart = "color:#"+parameters.getColorLevel2();
						theColor = parameters.getColorLevel2();
						weightPart = "font-weight:"+parameters.getWeightLevel2();
					}
					else if (counter==2){
						colorPart = "color:#"+parameters.getColorLevel3();
						theColor = parameters.getColorLevel3();
						weightPart = "font-weight:"+parameters.getWeightLevel3();
					}
				}
				else {
					if (tag.getNormScore()>0.3f){
						colorPart = "color:#"+parameters.getColorLevel1();
						theColor = parameters.getColorLevel1();
						weightPart = "font-weight:"+parameters.getWeightLevel1();
					}
					else if (tag.getNormScore()>0.2f){
						colorPart = "color:#"+parameters.getColorLevel2();
						theColor = parameters.getColorLevel2();
						weightPart = "font-weight:"+parameters.getWeightLevel2();
					}
					else if (tag.getNormScore()>0.1f){
						colorPart = "color:#"+parameters.getColorLevel3();
						theColor = parameters.getColorLevel3();
						weightPart = "font-weight:"+parameters.getWeightLevel3();
					}
				}

				String scoreSup = "";
				if (parameters.isDisplayScore()){
					scoreSup = "<span style=\"font-size:1em\"><sup>("+tag.getScoreInt()+")</sup></span>";
				}
				
				out.println("<a class=\"tagcloud_"+counter+"\" href=\"" + tag.getLink().replace(" & ", " %26 ") +"\" style=\"font-size: "+ tag.getWeight() +"em;"+colorPart+";"+weightPart+"; margin-right:"+parameters.getMarginRight()+"px\" onmouseout=\"this.style.color='#"+theColor+"'\" onmouseover=\"this.style.color='#0581a7'\">"+ tag.getName() + scoreSup +"</a>"); 


				counter ++;
				if (counter == 3) 
					counter = 0;
			}
			out.println("</div>");

			/*out.println("<br/>");
			out.println("<br/>");
			out.println("<br/>");
			out.println("<br/>");

			out.println("<div>");
			for (Tag tag : cloud.tags()) { 
				String classS;
				if (tag.getNormScore()>0.3f)
					classS = "tagcloud2_0";
				else if (tag.getNormScore()>0.2f)
					classS = "tagcloud2_1";
				else if (tag.getNormScore()>0.1f)
					classS = "tagcloud2_2";
				else 
					classS = "tagcloud2_3";

				String title = "Score = "+tag.getScore()+", Normalized score = "+tag.getNormScore()+", Weight = "+tag.getWeight()+", Category = "+classS.replace("tagcloud2_", "");

				out.println("<a class=\""+classS+"\" title = \""+title+"\"href=\"" + tag.getLink() +"\" style=\"font-size: "+ tag.getWeight() +"px;\">"+ tag.getName() +"</a>"); 

			}
			out.println("</div>");*/

		} catch (IOException e) {
			// TODO Auto-generated catch block
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