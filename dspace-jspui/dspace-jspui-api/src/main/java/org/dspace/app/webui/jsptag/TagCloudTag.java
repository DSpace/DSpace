/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.TagCloudParameters;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseIndex;

import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.mcavallo.opencloud.Cloud.Case;
import org.mcavallo.opencloud.formatters.HTMLFormatter;


/**
 * @author kstamatis
 *
 */
public class TagCloudTag extends TagSupport{

	private static Logger log = Logger.getLogger(TagCloudTag.class);

	TagCloudParameters parameters;
	BrowseInfo bi;

	/**
	 * 
	 */
	public TagCloudTag() {
		// TODO Auto-generated constructor stub
	}

	public void doTag() throws JspException { 

		JspWriter out = pageContext.getOut(); 

		if (parameters == null)
			parameters = new TagCloudParameters();

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
			if (parameters.getTotalTags().equals("all"))
				cloud.setMaxTagsToDisplay(1000000);
			else
				cloud.setMaxTagsToDisplay(Integer.parseInt(parameters.getTotalTags()));

			BrowseIndex bix = bi.getBrowseIndex();
			String linkBase = ((HttpServletRequest)pageContext.getRequest()).getContextPath() + "/";
			String direction = (bi.isAscending() ? "ASC" : "DESC");
			
			String[][] results = bi.getStringResults();
			
			for (String[] result : results){
				String freq = result[2]; 
				String sharedLink = linkBase + "browse?type=" + URLEncoder.encode(bix.getName()) + 
						"&amp;order=" + URLEncoder.encode(direction) + 
						"&amp;rpp=" + URLEncoder.encode(Integer.toString(bi.getResultsPerPage()));
				if (freq.equals("")) freq="0";
				if (Integer.parseInt(freq) > Integer.parseInt(parameters.getCuttingLevel())){
					if (result[1] != null) 
					{ 
						sharedLink = sharedLink + "&amp;authority="+ URLEncoder.encode(result[1], "UTF-8");
					}
					else {
						sharedLink = sharedLink + "&amp;value="+ URLEncoder.encode(result[0], "UTF-8");
					}
					for (int i=0; i<Integer.parseInt(freq); i++){
						Tag tag2 = new Tag(result[0], sharedLink/*"http://www.ekt.gr"*//*((HttpServletRequest) pageContext.getRequest()).getContextPath()+"/browse?type="+index+"&order=DESC&rpp=250&value="+subject+"&sort_by=1"*/);   // creates a tag
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
	public void setParameters(TagCloudParameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * @param index the index to set
	 */
	public void setBi(BrowseInfo bi) {
		this.bi = bi;
	}
}