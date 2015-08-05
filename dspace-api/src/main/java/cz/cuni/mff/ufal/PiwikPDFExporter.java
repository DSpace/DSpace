package cz.cuni.mff.ufal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.ShapeUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.PiwikReport;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

public class PiwikPDFExporter  {

    /** Piwik configurations */
    private static String PIWIK_REPORTS_OUTPUT_PATH;

    /** Piwik configurations */
    private static String PIWIK_API_URL;
    private static String PIWIK_AUTH_TOKEN;
    private static String PIWIK_SITE_ID;
    private static String PIWIK_DOWNLOAD_SITE_ID;
    private static boolean PIWIK_KEEP_REPORTS;

    private static String LINDAT_LOGO;
    
	private static SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMM-dd");
	
	
	private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(PiwikPDFExporter.class);
	
	public static void main(String args[]) throws Exception {
		log.info("Generating PIWIK pdf reports ....");
		initialize();
		generateReports();
		log.info("PIWIK pdf reports generation finished.");
	}
		
	public static void initialize() {
        PIWIK_REPORTS_OUTPUT_PATH = ConfigurationManager.getProperty("lr", "lr.statistics.report.path");
        PIWIK_KEEP_REPORTS = ConfigurationManager.getBooleanProperty("lr", "lr.statistics.keep.reports", true);
        PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
        PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
        PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
        PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");
        LINDAT_LOGO = ConfigurationManager.getProperty("lr", "lr.lindat.logo.mono");
	}
	
	public static void generateReports() throws SQLException {
		
		Context context = new Context();

		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();			
		List<PiwikReport> piwikReports = functionalityManager.getAllPiwikReports();
		functionalityManager.closeSession();
		
		File outputDir = new File(PIWIK_REPORTS_OUTPUT_PATH);
		if(!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		HashSet<Item> done = new HashSet<Item>();
		
		for(PiwikReport pr : piwikReports) {
			Item item = null;
			try {
				item = Item.find(context, pr.getItemId());
			} catch(SQLException e) {
				log.info(e);
			}
			if(item!=null) {
				if(!done.contains(item)) {
					if(item.getHandle()!=null && !item.getHandle().isEmpty()) {
						try {
							log.info("Processing Item : " + item.getHandle());
							generateItemReport(item);
							done.add(item);
						} catch(Exception e) {
							log.error("Unable to generate report.", e);
							continue;
						}
					} else {
						log.info("Item handle not found : item_id=" + item.getID());
					}				
				}
				EPerson to = EPerson.find(context, pr.getEpersonId());
				try {
					sendEmail(context, to, item);
				} catch(Exception e) {
					log.error(e);
				}
			}
		}
		//cleanup
		if(!PIWIK_KEEP_REPORTS) {
			try {
				FileUtils.deleteDirectory(outputDir);
			} catch (IOException e) {
				log.error(e);
			}
		}
	}
	
	public static void sendEmail(Context context, EPerson to, Item item) throws IOException, MessagingException {

		// Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(to);
        
	    String itemTitle = item.getMetadata("dc", "title", null, Item.ANY)[0].value;
	    String hdlURL = item.getMetadata("dc", "identifier", "uri", Item.ANY)[0].value;
        
		Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "piwik_report"));
		email.addArgument(itemTitle);
		email.addArgument(to.getName());
		email.addRecipient(to.getEmail());
		email.addAttachment(new File(PIWIK_REPORTS_OUTPUT_PATH + "/" + item.getID() + ".pdf"), "MonthlyStats.pdf");
		email.send();
	}
	
	public static void generateItemReport(Item item) throws Exception {
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.DATE, 1);
		Date firstDay = cal.getTime();
		cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		Date lastDay = cal.getTime();
		
		
		String viewsReportURL = PIWIK_API_URL + "index.php"
				+ "?module=API"
				+ "&method=API.get"
				+ "&idSite=" + PIWIK_SITE_ID
				+ "&period=day"
				+ "&date=" + inputDateFormat.format(firstDay) + "," + inputDateFormat.format(lastDay)
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&format=xml"
				+ "&segment=pageUrl=@" + item.getHandle();

		String downloadReportURL = PIWIK_API_URL + "index.php"
				+ "?module=API"
				+ "&method=API.get"
				+ "&idSite=" + PIWIK_DOWNLOAD_SITE_ID
				+ "&period=day"
				+ "&date=" + inputDateFormat.format(firstDay) + "," + inputDateFormat.format(lastDay)
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&format=xml"
				+ "&segment=pageUrl=@" + item.getHandle();
				
		String countryReportURL = PIWIK_API_URL + "index.php"
					+ "?module=API"
					+ "&method=UserCountry.getCountry"
					+ "&idSite=" + PIWIK_SITE_ID
					+ "&period=month"
					+ "&date=" + inputDateFormat.format(firstDay)												
					+ "&expanded=1"
					+ "&token_auth=" + PIWIK_AUTH_TOKEN
					+ "&filter_limit=10"
					+ "&format=xml"
					+ "&segment=pageUrl=@" + item.getHandle();
							
		
		String viewsXML = PiwikHelper.readFromURL(viewsReportURL);
		String downloadXML = PiwikHelper.readFromURL(downloadReportURL);
		
		viewsXML = PiwikHelper.mergeXML(viewsXML, downloadXML);
		String countriesXML = PiwikHelper.readFromURL(countryReportURL);
		
		Map<String, Integer> summary = new HashMap<String, Integer>();
		
		JFreeChart viewsChart = createViewsChart(viewsXML, summary);
		List<String[]> countryData = getCountryData(countriesXML);
		
		geneartePDF(item, firstDay, viewsChart, summary, countryData);
	}
	
	public static List<String[]> getCountryData(String xml) throws Exception {
		
		Document doc = parseXML(xml);
		
		if(doc==null) throw new Exception("Unable to parse XML");
		
		List<String[]> data = new ArrayList<String[]>();
		
		XPath xPath =  XPathFactory.newInstance().newXPath();
		XPathExpression eachResultNode = xPath.compile("//result/row");
		
		NodeList results = (NodeList)eachResultNode.evaluate(doc, XPathConstants.NODESET);
		
		for(int i=0;i<results.getLength();i++) {
			Element row = (Element)results.item(i);
			String country = row.getElementsByTagName("label").item(0).getTextContent();
			String count = row.getElementsByTagName("nb_visits").item(0).getTextContent();
			data.add(new String[]{country, count});
		}
		
		return data;
		
	}

	public static JFreeChart createViewsChart(String xml, Map<String, Integer> summary) throws Exception {

		Document doc = parseXML(xml);
		
		if(doc==null) throw new Exception("Unable to parse XML");
		
		JFreeChart lineChart = null;
		
		XPath xPath =  XPathFactory.newInstance().newXPath();
		XPathExpression eachResultNode = xPath.compile("//result");
		
		NodeList results = (NodeList)eachResultNode.evaluate(doc, XPathConstants.NODESET);
		
		int maxPageViews = Integer.MIN_VALUE;		
		
		TimeSeries viewsSeries = new TimeSeries("Views");
		TimeSeries downloadsSeries = new TimeSeries("Downloads");
		
		int totalViews = 0;
		int totalUniqueViews = 0;
		int totalVisitors = 0;
		int totalUniqueVisitors = 0;
		int totalDownloads = 0;
		int totalUniqueDownloads = 0;
		
		for(int i=0;i<results.getLength();i++) {
			Element result = (Element)results.item(i);
			String date = result.getAttribute("date");
			Date dateObj = inputDateFormat.parse(date);
			int iPageViews = 0;
			int iDownloads = 0;
			try{							
				String pageViews = result.getElementsByTagName("nb_pageviews").item(0).getTextContent();
				String downloads = result.getElementsByTagName("nb_downloads").item(0).getTextContent();
				iPageViews = Integer.parseInt(pageViews);
				iDownloads = Integer.parseInt(downloads);
				
				totalViews += iPageViews;
				totalDownloads += iDownloads;				
				totalUniqueViews += Integer.parseInt(result.getElementsByTagName("nb_uniq_pageviews").item(0).getTextContent());
				totalUniqueDownloads += Integer.parseInt(result.getElementsByTagName("nb_uniq_downloads").item(0).getTextContent());
				totalVisitors += Integer.parseInt(result.getElementsByTagName("nb_visits").item(0).getTextContent());
				totalUniqueVisitors += Integer.parseInt(result.getElementsByTagName("nb_uniq_visitors").item(0).getTextContent());				
				
			}catch(Exception e) {
			}
						
			if(maxPageViews < iPageViews) maxPageViews = iPageViews;
			viewsSeries.add(new Day(dateObj), iPageViews);
			downloadsSeries.add(new Day(dateObj), iDownloads);			
		}
		
		summary.put("pageviews", totalViews);
		summary.put("unique pageviews", totalUniqueViews);
		summary.put("visits", totalVisitors);
		summary.put("unique visitors", totalUniqueVisitors);
		summary.put("downloads", totalDownloads);
		summary.put("unique downloads", totalUniqueDownloads);
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(viewsSeries);
		dataset.addSeries(downloadsSeries);		
		
		lineChart = ChartFactory.createTimeSeriesChart("Views Over Time", "", "", dataset);
		lineChart.setBackgroundPaint(Color.WHITE);
		lineChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
		
		XYPlot plot = (XYPlot) lineChart.getPlot();
		
		plot.setOutlineVisible(false);
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
        	XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
        	renderer.setBaseShapesVisible(true);
        	renderer.setBaseShapesFilled(true);
        	Shape circle = new Ellipse2D.Double(-1f, -1f, 2, 2);
        	renderer.setSeriesShape(0, circle);
        	renderer.setSeriesShape(1, circle);
        	renderer.setSeriesPaint(0, new Color(212, 40, 30));
        	renderer.setSeriesPaint(1, new Color(30, 120, 180));
        	renderer.setSeriesStroke(0, new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        	renderer.setSeriesStroke(1, new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        }
        DateAxis xAxis = (DateAxis) plot.getDomainAxis();
        xAxis.setDateFormatOverride(outputDateFormat);
        xAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 8));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 8));
        
		int diff = maxPageViews / 3;		
		if(diff<=1) {
			diff = 1;			
		}
		yAxis.setTickUnit(new NumberTickUnit(diff));

        LegendTitle legend = lineChart.getLegend();
        legend.setPosition(RectangleEdge.TOP);
        legend.setBorder(0, 0, 0, 0);
        legend.setItemFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 8));
                        
        return lineChart;
	}
	
	public static void geneartePDF(Item item, Date date, JFreeChart viewsChart, Map<String, Integer> summary, List<String[]> countryData) throws Exception {
		com.itextpdf.text.Document pdf = new com.itextpdf.text.Document(PageSize.A4, 36, 36, 54, 54);
		PdfWriter writer = PdfWriter.getInstance(pdf, new FileOutputStream(PIWIK_REPORTS_OUTPUT_PATH + "/" + item.getID() + ".pdf"));

		pdf.open();
				
	    Font[] FONT = new Font[8];
	    FONT[0] = new Font(FontFamily.HELVETICA, 20, Font.BOLD);
	    FONT[1] = new Font(FontFamily.HELVETICA, 14, Font.BOLD);
	    FONT[1].setColor(85, 200, 250);
	    FONT[2] = new Font(FontFamily.HELVETICA, 16, Font.BOLD);
	    FONT[3] = new Font(FontFamily.HELVETICA, 12, Font.BOLD);
	    FONT[4] = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    FONT[4].setColor(85, 200, 250);	    
	    FONT[5] = new Font(FontFamily.HELVETICA, 8, Font.BOLD);	    
	    FONT[6] = new Font(FontFamily.HELVETICA, 8);
	    FONT[7] = new Font(FontFamily.HELVETICA, 10, Font.BOLD);

	    
	    Image logo = Image.getInstance(LINDAT_LOGO);
	    logo.scaleAbsolute(82, 48);
	    logo.setAlignment(Image.RIGHT);

	    Paragraph titleText = new Paragraph();
	    titleText.setFont(FONT[0]);
	    titleText.add("Monthly Item Statistics");
	    
	    Paragraph titleMonth = new Paragraph();
	    titleMonth.setFont(FONT[1]);
	    titleMonth.add(new SimpleDateFormat("MMM, yyyy").format(date));
	    
	    PdfPTable title = new PdfPTable(new float[]{80, 20});
	    title.setWidthPercentage(100);

	    PdfPCell titleC1 = new PdfPCell();
	    titleC1.setVerticalAlignment(PdfPCell.ALIGN_BOTTOM);
	    titleC1.setBorder(0);
	    titleC1.addElement(titleText);
	    	    
	    PdfPCell titleC2 = new PdfPCell();
	    titleC2.setVerticalAlignment(PdfPCell.ALIGN_TOP);
	    titleC2.setBorder(0);
	    titleC2.setRowspan(2);
	    titleC2.addElement(logo);

	    PdfPCell titleC3 = new PdfPCell();
	    titleC3.setVerticalAlignment(PdfPCell.ALIGN_TOP);
	    titleC3.setBorder(0);	    
	    titleC3.addElement(titleMonth);	
	    
	    title.addCell(titleC1);
	    title.addCell(titleC2);
	    title.addCell(titleC3);

	    pdf.add(title);
	    
	    LineSeparator line = new LineSeparator();
	    line.setPercentage(100);
	    line.setLineWidth(1);
	    line.setOffset(5);
	    line.setLineColor(BaseColor.LIGHT_GRAY);
	    
	    Chunk cl = new Chunk(line);
	    cl.setLineHeight(10);
	    
	    pdf.add(cl);

	    String itemTitle = item.getMetadata("dc", "title", null, Item.ANY)[0].value;
	    String hdlURL = item.getMetadata("dc", "identifier", "uri", Item.ANY)[0].value;
	    
	    Paragraph itemName = new Paragraph();
	    itemName.setFont(FONT[3]);
	    itemName.add(itemTitle);
	    
	    pdf.add(itemName);
	    
	    Chunk hdl = new Chunk(hdlURL, FONT[4]);
	    hdl.setAction(new PdfAction(new URL(hdlURL)));
	    
	    pdf.add(hdl);
	    
	    PdfPTable stats = new PdfPTable(new float[]{70, 30});
	    stats.setWidthPercentage(100);
	    
	    Paragraph byCountry = new Paragraph();
	    byCountry.setFont(FONT[5]);
	    byCountry.add("Visitors By Country");
	    
	    PdfPCell statsC1 = new PdfPCell();
	    statsC1.setBorder(0);
	    statsC1.setPaddingTop(220);
	    statsC1.setPaddingBottom(200);
	    
	    PdfPTable summaryStats = new PdfPTable(1);	    

	    Paragraph summaryHeadTxt = new Paragraph();
	    summaryHeadTxt.setFont(FONT[5]);
	    summaryHeadTxt.add("Summary");	    
	    
	    PdfPCell summaryHead = new PdfPCell();
	    summaryHead.setBorder(0);
	    summaryHead.addElement(summaryHeadTxt);
	    
	    summaryStats.addCell(summaryHead);
	    
	    Paragraph text = new Paragraph();
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("pageviews"));
	    text.setFont(FONT[6]);
	    text.add(" pageviews, ");
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("unique pageviews"));
	    text.setFont(FONT[6]);
	    text.add(" unique pageviews");
	    
	    PdfPCell srow = new PdfPCell();
	    srow.setBorder(0);
	    srow.addElement(text);
	    
	    summaryStats.addCell(srow);
	    
	    text = new Paragraph();
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("visits"));
	    text.setFont(FONT[6]);
	    text.add(" visits, ");
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("unique visitors"));
	    text.setFont(FONT[6]);
	    text.add(" unique visitors");
	    
	    srow = new PdfPCell();
	    srow.setBorder(0);
	    srow.addElement(text);
	    
	    summaryStats.addCell(srow);
	    
	    text = new Paragraph();
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("downloads"));
	    text.setFont(FONT[6]);
	    text.add(" downloads, ");
	    text.setFont(FONT[7]);
	    text.add("" + summary.get("unique downloads"));
	    text.setFont(FONT[6]);
	    text.add(" unique downloads");
	    
	    srow = new PdfPCell();
	    srow.setBorder(0);
	    srow.addElement(text);
	    
	    summaryStats.addCell(srow);

	    
	    statsC1.addElement(summaryStats);

	    PdfPCell statsC2 = new PdfPCell();
	    statsC2.setBackgroundColor(new BaseColor(240, 240, 240));
	    statsC2.setBorder(0);
	    statsC2.setPadding(5);
	    statsC2.addElement(byCountry);
	    
	    PdfPTable countryStats = new PdfPTable(new float[]{80, 20});
	    
	    for(String[] cs : countryData) {
		    
	    	Paragraph label = new Paragraph();
		    label.setFont(FONT[6]);
		    label.add(cs[0]);
		    
		    Paragraph value = new Paragraph();
		    value.setFont(FONT[5]);
		    value.add(cs[1]);
		    
		    PdfPCell cs1 = new PdfPCell();
		    cs1.setBorder(0);
		    cs1.setPadding(2);
		    cs1.addElement(label);

		    PdfPCell cs2 = new PdfPCell();
		    cs2.setBorder(0);
		    cs2.setPadding(2);
		    cs2.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
		    cs2.addElement(value);

		    countryStats.addCell(cs1);
		    countryStats.addCell(cs2);
	    }
	   
	    statsC2.addElement(countryStats);
	    
	    stats.addCell(statsC1);
	    stats.addCell(statsC2);
	    
	    pdf.add(stats);
	    
	    pdf.add(cl);
	    
		float width  = 350;
		float height = 200;
		PdfContentByte cb = writer.getDirectContent();
		PdfTemplate chart = cb.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(chart, width, height);
		Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
		viewsChart.draw(g2d, r2d);
		g2d.dispose();
		cb.addTemplate(chart, 40, 470);
		
		pdf.close();			
		writer.close();
	}
	
	public static Document parseXML(String xml) {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		try {
		    builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
		    e.printStackTrace();  
		}
		try {
			doc = builder.parse(new StringInputStream(xml));
		} catch (Exception e) {
			log.error(e);
		}
		return doc;
	}

	private static String readFromURL(String url) throws IOException {
		StringBuilder output = new StringBuilder();		
		URL widget = new URL(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(widget.openStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			output.append(inputLine).append("\n");
		}
		in.close();
		return output.toString();
	}
}

