/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import au.com.bytecode.opencsv.CSVWriter;
import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.statistics.StatisticsTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Bitstream;
import org.dspace.content.MetadataValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Provides the usage statistics in CSV form
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class CSVOutputter extends AbstractReader implements Recyclable 
{
    protected static final Logger log = Logger.getLogger(CSVOutputter.class);
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>(){
                    @Override
                    protected DateFormat initialValue() {
                        return new SimpleDateFormat("yyyy-MM-dd");
                    }
                  };

    protected Response response;
    protected Request request;
    protected Context context;
    protected CSVWriter writer = null;

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public void setup(SourceResolver sourceResolver, Map objectModel, String src, Parameters parameters) throws IOException, SAXException, ProcessingException {
        log.info("CSV Writer for stats");
        super.setup(sourceResolver, objectModel, src, parameters);

        try {
            //super.setup(sourceResolver, objectModel, src, parameters);
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);

            context = ContextUtil.obtainContext(objectModel);

            String requestURI = request.getRequestURI();
            String[] uriSegments = requestURI.split("/");
            String requestedReport = uriSegments[uriSegments.length-1];
            if(requestedReport == null || requestedReport.length() < 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            response.setContentType("text/csv; encoding='UTF-8'");
            response.setStatus(HttpServletResponse.SC_OK);
            writer = new CSVWriter(response.getWriter());
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            response.setHeader("Content-Disposition", "attachment; filename=KBStats-" + dso.getHandle() + "-" + requestedReport +".csv");

            Map<String, String> params = new HashMap<String, String>();
            for (Enumeration<String> paramNames = (Enumeration<String>) request.getParameterNames(); paramNames.hasMoreElements(); ) {
                String param = paramNames.nextElement();
                params.put(param, request.getParameter(param));
            }

            String fromValue = "";
            if(params.containsKey("from")) {
                fromValue = params.get("from");
            }
            
            String toValue = "";
            if(params.containsKey("to")) {
                toValue = params.get("to");
            }

            Date fromDate;
            ReportGenerator reportGeneratorInstance = new ReportGenerator();

            if(fromValue.length() > 0) {
                fromDate = reportGeneratorInstance.tryParse(fromValue);
            } else {
                fromDate = null;
            }

            Date toDate;
            if(toValue.length() > 0) {
                toDate = reportGeneratorInstance.tryParse(toValue);
            } else {
                toDate = null;
            }
            
            ElasticSearchStatsViewer esStatsViewer = new ElasticSearchStatsViewer(dso, fromDate, toDate);
            if(requestedReport.equalsIgnoreCase("topCountries"))
            {                
                SearchRequestBuilder requestBuilder = esStatsViewer.facetedQueryBuilder(esStatsViewer.facetTopCountries);
                SearchResponse searchResponse = requestBuilder.execute().actionGet();

                TermsFacet topCountriesFacet = searchResponse.getFacets().facet(TermsFacet.class, "top_countries");
                addTermFacetToWriter(topCountriesFacet, "");
            }
            else if (requestedReport.equalsIgnoreCase("fileDownloads"))
            {
                SearchRequestBuilder requestBuilder = esStatsViewer.facetedQueryBuilder(esStatsViewer.facetMonthlyDownloads);
                SearchResponse searchResponse = requestBuilder.execute().actionGet();

                DateHistogramFacet monthlyDownloadsFacet = searchResponse.getFacets().facet(DateHistogramFacet.class, "monthly_downloads");
                addDateHistogramFacetToWriter(monthlyDownloadsFacet);
            }
            else if(requestedReport.equalsIgnoreCase("topDownloads"))
            {
                SearchRequestBuilder requestBuilder = esStatsViewer.facetedQueryBuilder(esStatsViewer.facetTopBitstreamsAllTime);
                SearchResponse searchResponse = requestBuilder.execute().actionGet();
                log.info(searchResponse.toString());

                TermsFacet topBitstreams = searchResponse.getFacets().facet(TermsFacet.class, "top_bitstreams_alltime");
                addTermFacetToWriter(topBitstreams, "bitstream");
            }
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

        } catch (SQLException e) {
            log.error("Some Error:" + e.getMessage());
        } catch (WingException e) {
            log.error("Some Error:" + e.getMessage());
        } catch (IOException e) {
            log.error("Some Error:" + e.getMessage());
        } finally {
            try {
                if(writer != null) {
                    writer.close();
                } else {
                    log.error("CSV Writer was null!!");
                }
            } catch (IOException e) {
                log.error("Hilarity Ensues... IO Exception while closing the csv writer.");
            }

        }
    }

    private void addTermFacetToWriter(TermsFacet termsFacet, String termType) throws SQLException {
        List<? extends TermsFacet.Entry> termsFacetEntries = termsFacet.getEntries();

        if(termType.equalsIgnoreCase("bitstream")) {
            writer.writeNext(new String[]{"BitstreamID", "Bitstream Name", "Bitstream Bundle", "Item Title", "Item Handle", "Item Creator", "Item Publisher", "Item Issue Date", "Count"});
        } else {
            writer.writeNext(new String[]{"term", "count"});
        }
        if(termsFacetEntries.size() == 0) {
            return;
        }
        
        for(TermsFacet.Entry facetEntry : termsFacetEntries)
        {
            if(termType.equalsIgnoreCase("bitstream"))
            {
                Bitstream bitstream = bitstreamService.findByIdOrLegacyId(context, facetEntry.getTerm().string());
                Item item = (Item) bitstreamService.getParentObject(context, bitstream);
                
                String[] entryValues = new String[9];
                
                entryValues[0] = bitstream.getID() + "";
                entryValues[1] = bitstream.getName();
                entryValues[2] = bitstream.getBundles().get(0).getName();
                entryValues[3] = item.getName();
                entryValues[4] = "http://hdl.handle.net/" + item.getHandle();
                entryValues[5] = wrapInDelimitedString(itemService.getMetadataByMetadataString(item, "dc.creator"));
                entryValues[6] = wrapInDelimitedString(itemService.getMetadataByMetadataString(item, "dc.publisher"));
                entryValues[7] = wrapInDelimitedString(itemService.getMetadataByMetadataString(item, "dc.date.issued"));
                entryValues[8] = facetEntry.getCount() + "";
                writer.writeNext(entryValues);
            } else {
                writer.writeNext(new String[]{facetEntry.getTerm().string(), String.valueOf(facetEntry.getCount())});
            }
        }
    }
    
    public String wrapInDelimitedString(List<MetadataValue> metadataEntries) {
        StringBuilder metadataString = new StringBuilder();

        for(MetadataValue metadataEntry : metadataEntries) {
            if(metadataString.length() > 0) {
                // Delimit entries with the || double pipe character sequence.
                metadataString.append("\\|\\|");
            }
            metadataString.append(metadataEntry.getValue());
        }
        
        return metadataString.toString();
    }
    
    private void addDateHistogramFacetToWriter(DateHistogramFacet dateHistogramFacet) {
        List<? extends DateHistogramFacet.Entry> monthlyFacetEntries = dateHistogramFacet.getEntries();

        if(monthlyFacetEntries.size() == 0) {
            return;
        }

        writer.writeNext(new String[]{"Month", "Count"});

        for(DateHistogramFacet.Entry histogramEntry : monthlyFacetEntries) {
            Date facetDate = new Date(histogramEntry.getTime());
            writer.writeNext(new String[]{dateFormat.get().format(facetDate), String.valueOf(histogramEntry.getCount())});
        }
    }

    public void generate() throws IOException {
        log.info("CSV Writer generator for stats");
        out.flush();
        out.close();
    }
    
    public void recycle() {
        this.request = null;
        this.response = null;
        this.context = null;
        this.writer = null;
        super.recycle();
    }
    
}
