/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.xml.sax.SAXException;
/**
 * modified for LINDAT/CLARIN
*/
public class StatisticsTransformer extends AbstractDSpaceTransformer {

	private static Logger log = Logger.getLogger(StatisticsTransformer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statistics.title");
    private static final Message T_statistics_trail = message("xmlui.statistics.trail");
    private static final String T_head_visits_total = "xmlui.statistics.visits.total";
    private static final String T_head_visits_year = "xmlui.statistics.visits.year";
    private static final String T_head_visits_views = "xmlui.statistics.visits.views";
    private static final String T_head_visits_countries = "xmlui.statistics.visits.countries";
    private static final String T_head_visits_cities = "xmlui.statistics.visits.cities";
    private static final String T_head_visits_bitstream = "xmlui.statistics.visits.bitstreams";
    
    private static final Message T_most_viewed = message("homepage.most_viewed_items");
    private static final String T_top_week = "homepage.top_week";

    private Date dateStart = null;
    private Date dateEnd = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public StatisticsTransformer(Date dateStart, Date dateEnd) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

        try {
            this.context = new Context();
        } catch (SQLException e) {
            log.error("Error getting context in StatisticsTransformer:" + e.getMessage());
        }
    }

    public StatisticsTransformer() {
        try {
            this.context = new Context();
        } catch (SQLException e) {
            log.error("Error getting context in StatisticsTransformer:" + e.getMessage());
        }
    }

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        if(dso != null)
        {
        	HandleUtil.buildHandleTrailTerminal(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(T_statistics_trail);
        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
    }

    /**
	 * What to add at the end of the body
	 */
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

		try
		{
			if(dso != null)
			{
				renderViewer(body, dso);
			}
			else
			{
				renderHome(body);
			}

        } catch (RuntimeException e) {
            throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public void renderHome(Body body) throws WingException {
		
		Division home = body.addDivision("home", "primary repository");
		Division division = home.addDivision("stats", "secondary stats");
		division.setHead(T_most_viewed);
		try {

			/** List of the top 10 items for the entire repository Last week **/

			StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits());
			
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			Calendar cal = new GregorianCalendar();
			dateFilter.setEndDate(cal.getTime());			
			cal.add(Calendar.WEEK_OF_MONTH, -1);
			dateFilter.setStartDate(cal.getTime());
			statListing.addFilter(dateFilter);			
			statListing.setTitle(T_top_week);

            //Adding a new generator for our top 10 items without a name length delimiter
            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(Constants.ITEM, 10, false, -1);
            statListing.addDatasetGenerator(dsoAxis);

            //Render the list as a table
			addDisplayListing(division.addDivision("top-week"), statListing);

            /** List of the top 10 items for the entire repository All Time 
			
			StatisticsListing statListing2 = new StatisticsListing(new StatisticsDataVisits());				
			statListing2.setTitle("Top All Times");

            //Adding a new generator for our top 10 items without a name length delimiter
            DatasetDSpaceObjectGenerator dsoAxis2 = new DatasetDSpaceObjectGenerator();
            dsoAxis2.addDsoChild(Constants.ITEM, 10, false, -1);
            statListing2.addDatasetGenerator(dsoAxis);            

            //Render the list as a table
			addDisplayListing(division.addDivision("top-all"), statListing2);**/
			
			
			

		} catch (Exception e) {
			log.error("Error occurred while creating statistics for home page", e);
		}

	}

	public void renderViewer(Body body, DSpaceObject dso) throws WingException {

		Division home = body.addDivision(
				Constants.typeText[dso.getType()].toLowerCase() + "-home", 
				"primary repository " + Constants.typeText[dso.getType()].toLowerCase());
		
		// Build the collection viewer division.
		Division division = home.addDivision("stats", "secondary stats");
		division.setHead(T_head_title);

		division = division.addDivision("item_visits");
		
		String sdate = null;

		if(Constants.ITEM == dso.getType()) {				
			org.dspace.content.Item item = (org.dspace.content.Item)dso;
			Metadatum[] dates = item.getMetadata("dc", "date", "issued", org.dspace.content.Item.ANY);
			if(dates!=null) {
				sdate = dates[0].value;
			}
		}			
		
		if(sdate!=null) {
			division.addPara(null, "label label-default").addContent("Showing statistics from " + sdate);
		}
		
		try {
			StatisticsListing statListing = new StatisticsListing(
					new StatisticsDataVisits(dso));

			statListing.setTitle(T_head_visits_total);
			statListing.setId("list1");

			DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
			dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
			statListing.addDatasetGenerator(dsoAxis);

			addDisplayListing(division, statListing);

		} catch (Exception e) {
			log.error(
					"Error occurred while creating statistics for dso with ID: "
							+ dso.getID() + " and type " + dso.getType()
							+ " and handle: " + dso.getHandle(), e);
		}
		
		
		
		try {

			StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

			statisticsTable.setTitle(T_head_visits_year);
			statisticsTable.setId("tab1");

			DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
			int lastNYears = ConfigurationManager.getIntProperty("usage-statistics", "last.n.years", 12);
			String accessioned = dso.getMetadata("dc.date.accessioned");
			if(accessioned != null && !accessioned.isEmpty()){
                SimpleDateFormat format = new SimpleDateFormat("yyyy");
                Date date = format.parse(accessioned);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                Calendar now = Calendar.getInstance();
                now.setTime(new Date());
                int yearsAvail = now.get(Calendar.YEAR)-cal.get(Calendar.YEAR);
                if(yearsAvail < lastNYears){
                        lastNYears = yearsAvail;
                }
			}
			//Year wise breakup of last 12 years + this year
			timeAxis.setDateInterval("year", Integer.toString(0 - lastNYears), "+1");
			statisticsTable.addDatasetGenerator(timeAxis);

			DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
			dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
			statisticsTable.addDatasetGenerator(dsoAxis);

			addDisplayTable(division, statisticsTable);

		} catch (Exception e) {
			log.error(
					"Error occurred while creating statistics for dso with ID: "
							+ dso.getID() + " and type " + dso.getType()
							+ " and handle: " + dso.getHandle(), e);
		}

/*         if(dso instanceof org.dspace.content.Item){
             //Make sure our item has at least one bitstream
             org.dspace.content.Item item = (org.dspace.content.Item) dso;
            try {
                if(item.hasUploadedFiles()){
                    StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));

                    statsList.setTitle(T_head_visits_bitstream);
                    statsList.setId("list-bit");

                    DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                    dsoAxis.addDsoChild(Constants.BITSTREAM, 10, false, -1);
                    statsList.addDatasetGenerator(dsoAxis);

                    addDisplayListing(division, statsList);
                }
            } catch (Exception e) {
                log.error(
                        "Error occurred while creating statistics for dso with ID: "
                                + dso.getID() + " and type " + dso.getType()
                                + " and handle: " + dso.getHandle(), e);
            }
        }*/

        try {
            StatisticsListing statListing = new StatisticsListing(
                       new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_countries);
            statListing.setId("list2");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("countryCode");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating statistics for dso with ID: "
                            + dso.getID() + " and type " + dso.getType()
                            + " and handle: " + dso.getHandle(), e);
        }

        try {
            StatisticsListing statListing = new StatisticsListing(
                       new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_cities);
            statListing.setId("list3");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("city");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating statistics for dso with ID: "
                            + dso.getID() + " and type " + dso.getType()
                            + " and handle: " + dso.getHandle(), e);
        }

        
        if(dso instanceof org.dspace.content.Item){
            //Make sure our item has at least one bitstream
            org.dspace.content.Item item = (org.dspace.content.Item) dso;
           try {
               if(item.hasUploadedFiles()){
            	   
            	   division = division.addDivision("file_visits");
            	   division.setHead(message(T_head_visits_bitstream));

                   StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));

                   statsList.setTitle(T_head_visits_bitstream);
                   statsList.setId("list-bit");

                   DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                   dsoAxis.addDsoChild(Constants.BITSTREAM, 10, false, -1);
                   statsList.addDatasetGenerator(dsoAxis);
                   addDisplayListing(division, statsList);
            	   
            	   for(Bundle bundle : item.getBundles("ORIGINAL")) {
            		   for(Bitstream bitstream : bundle.getBitstreams()) {            			    
            			    StatisticsListing bitstreamStats = new StatisticsListing(new StatisticsDataVisits(bitstream));
            			    bitstreamStats.setTitle(bitstream.getName());
            	            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            	            typeAxis.setType("countryCode");
            	            typeAxis.setMax(10);
            	            bitstreamStats.addDatasetGenerator(typeAxis);

            	            addDisplayListing(division, bitstreamStats);            			   
            		   }
            	   }
            	                      
               }
           } catch (Exception e) {
               log.error(
                       "Error occurred while creating statistics for dso with ID: "
                               + dso.getID() + " and type " + dso.getType()
                               + " and handle: " + dso.getHandle(), e);
           }
       }
        
        
	}

	
	/**
	 * Adds a table layout to the page
	 * 
	 * @param mainDiv
	 *            the div to add the table to
	 * @param display
	 * @throws SAXException
	 * @throws WingException
	 * @throws ParseException
	 * @throws IOException
	 * @throws SolrServerException
	 * @throws SQLException
	 */
	private void addDisplayTable(Division mainDiv, StatisticsTable display)
			throws SAXException, WingException, SQLException,
			SolrServerException, IOException, ParseException {

		String title = display.getTitle();

		Dataset dataset = display.getDataset();

		if (dataset == null) {
			/** activate dataset query */
			dataset = display.getDataset(context);
		}

		if (dataset != null) {

			String[][] matrix = dataset.getMatrix();

			/** Generate Table */
			Division wrapper = mainDiv.addDivision("tablewrapper");
			Table table = wrapper.addTable("list-table", 1, 1,
					title == null ? "detailtable" : "tableWithTitle detailtable");
			if (title != null)
            {
                table.setHead(message(title));
            }

			/** Generate Header Row */
			Row headerRow = table.addRow();
			headerRow.addCell("spacer", Cell.ROLE_HEADER, "labelcell");

			String[] cLabels = dataset.getColLabels().toArray(new String[0]);
			for (int row = 0; row < cLabels.length; row++) {
				Cell cell = headerRow.addCell(0 + "-" + row + "-h",
                        Cell.ROLE_HEADER, "labelcell");
				cell.addContent(cLabels[row]);
			}

			/** Generate Table Body */
			for (int row = 0; row < matrix.length; row++) {
				Row valListRow = table.addRow();

				/** Add Row Title */
				valListRow.addCell("" + row, Cell.ROLE_DATA, "labelcell")
						.addContent(dataset.getRowLabels().get(row));

				/** Add Rest of Row */
				for (int col = 0; col < matrix[row].length; col++) {
					Cell cell = valListRow.addCell(row + "-" + col,
							Cell.ROLE_DATA, "datacell");
					cell.addContent(matrix[row][col]);
				}
			}
		}

	}

	private void addDisplayListing(Division mainDiv, StatisticsListing display)
			throws SAXException, WingException, SQLException,
			SolrServerException, IOException, ParseException {

		String title = display.getTitle();

		Dataset dataset = display.getDataset();

		if (dataset == null) {
			/** activate dataset query */
			dataset = display.getDataset(context);
		}

		if (dataset != null) {

			String[][] matrix = dataset.getMatrix();

			// String[] rLabels = dataset.getRowLabels().toArray(new String[0]);

			Table table = mainDiv.addTable("list-table", matrix.length, 2,
					title == null ? "detailtable" : "tableWithTitle detailtable");
			if (title != null)
            {
                table.setHead(message(title));
            }

			Row headerRow = table.addRow();

			headerRow.addCell("", Cell.ROLE_HEADER, "labelcell");
			
			headerRow.addCell("", Cell.ROLE_HEADER, "labelcell").addContent(message(T_head_visits_views));

			/** Generate Table Body */
			for (int col = 0; col < matrix[0].length; col++) {
				String url = dataset.getColLabelsAttrs().get(col).get("url");
				
				String rend = "";

				/* if(url==null) {
					rend = "hidden";
				}*/
				
				Row valListRow = table.addRow(null, null, rend);

				Cell catCell = valListRow.addCell(col + "1", Cell.ROLE_DATA,
						"labelcell");
								
				if(url != null && !url.equals("")) {
					catCell.addXref(url, dataset.getColLabels().get(col));
				} else {
				catCell.addContent(dataset.getColLabels().get(col));
				}				

				Cell valCell = valListRow.addCell(col + "2", Cell.ROLE_DATA,
						"datacell");
				valCell.addContent(matrix[0][col]);

			}

			if (!"".equals(display.getCss())) {
				List attrlist = mainDiv.addList("divattrs");
				attrlist.addItem("style", display.getCss());
			}

		}

	}

}
