package cz.cuni.mff.ufal;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URL;

/**
 * Created by okosarko on 3.8.15.
 */
public class PiwikHelperTest {

    @Test
    public void testJSONmerge() throws Exception{
        URL url = this.getClass().getResource("/piwik/view_report.json");
        File vrJson = new File(url.getFile());
        url = this.getClass().getResource("/piwik/download_report.json");
        File drJson = new File(url.getFile());
        String jsonResult = PiwikHelper.mergeJSON(FileUtils.readFileToString(vrJson), FileUtils.readFileToString(drJson));
        int mergedPVS = 0;
        int mergedUPVS = 0;
        int mergedDS = 0;
        int mergedUDS = 0;
        JSONParser parser = new JSONParser();
        JSONObject merged = (JSONObject)parser.parse(jsonResult);
        for(Object key : merged.keySet()) {
            if(merged.get(key) instanceof JSONObject) {
                JSONObject result = (JSONObject) merged.get(key);
                for (Object resultKey : result.keySet()) {
                    if ("nb_pageviews".equals(resultKey)) {
                        mergedPVS += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_pageviews".equals(resultKey)) {
                        mergedUPVS += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_downloads".equals(resultKey)) {
                        mergedDS += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_downloads".equals(resultKey)) {
                        mergedUDS += ((Long) result.get(resultKey)).intValue();
                    }
                }
            }else if(merged.get(key) instanceof JSONArray){
                JSONArray arr = (JSONArray) merged.get(key);
                assertEquals("Expecting empty array", 0, arr.size());
            }
        }

        int ds = 0;
        int uds = 0;
        int pvs = 0;
        int upvs = 0;
        JSONObject download = (JSONObject)parser.parse(FileUtils.readFileToString(drJson));
        for(Object key : download.keySet()) {
            if(download.get(key) instanceof JSONObject) {
                JSONObject result = (JSONObject) download.get(key);
                for (Object resultKey : result.keySet()) {
                    if ("nb_pageviews".equals(resultKey)) {
                        ds += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_pageviews".equals(resultKey)) {
                        uds += ((Long) result.get(resultKey)).intValue();
                    }
                }
            }else if(download.get(key) instanceof JSONArray){
                JSONArray arr = (JSONArray) download.get(key);
                assertEquals("Expecting empty array", 0, arr.size());
            }
        }

        JSONObject views = (JSONObject)parser.parse(FileUtils.readFileToString(vrJson));
        for(Object key : views.keySet()) {
            if(views.get(key) instanceof JSONObject) {
                JSONObject result = (JSONObject) views.get(key);
                for (Object resultKey : result.keySet()) {
                    if ("nb_pageviews".equals(resultKey)) {
                        pvs += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_pageviews".equals(resultKey)) {
                        upvs += ((Long) result.get(resultKey)).intValue();
                    }
                }
            }else if(views.get(key) instanceof JSONArray){
                JSONArray arr = (JSONArray) views.get(key);
                assertEquals("Expecting empty array", 0, arr.size());
            }
        }
        assertEquals("Downloads not merged properly", ds, mergedDS);
        assertEquals("Uniq downloads not merged properly", uds, mergedUDS);
        assertEquals("Page views not merged properly", pvs, mergedPVS);
        assertEquals("Uniq page views not merged properly", upvs, mergedUPVS);

    }

    @Test
    public void testXMLMerge() throws Exception{
        URL url = this.getClass().getResource("/piwik/download_report.xml");
        File drXml = new File(url.getFile());
        url = this.getClass().getResource("/piwik/view_report.xml");
        File vrXml = new File(url.getFile());
        String xmlResult = PiwikHelper.mergeXML(FileUtils.readFileToString(vrXml), FileUtils.readFileToString(drXml));

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression resCountX = xpath.compile("count(//result)");
        XPathExpression pageViewsSumX = xpath.compile("sum(//nb_pageviews/text())");
        XPathExpression uniqPageViewsSumX = xpath.compile("sum(//nb_uniq_pageviews/text())");
        XPathExpression downSumX = xpath.compile("sum(//nb_downloads/text())");
        XPathExpression uniqDownSumX = xpath.compile("sum(//nb_uniq_downloads/text())");

        Document merged = PiwikHelper.loadXMLFromString(xmlResult);
        int mergedResCount = ((Double)resCountX.evaluate(merged, XPathConstants.NUMBER)).intValue();
        int mergedPageViewsSum = ((Double)pageViewsSumX.evaluate(merged, XPathConstants.NUMBER)).intValue();
        int mergedUniqPageViewsSum = ((Double)uniqPageViewsSumX.evaluate(merged, XPathConstants.NUMBER)).intValue();
        int mergedDownSum = ((Double)downSumX.evaluate(merged, XPathConstants.NUMBER)).intValue();
        int mergedUniqDownSum = ((Double)uniqDownSumX.evaluate(merged, XPathConstants.NUMBER)).intValue();

        Document downloads = PiwikHelper.loadXMLFromString(FileUtils.readFileToString(drXml));
        int downSum = ((Double)pageViewsSumX.evaluate(downloads, XPathConstants.NUMBER)).intValue();
        int uniqDownSum = ((Double)uniqPageViewsSumX.evaluate(downloads, XPathConstants.NUMBER)).intValue();

        Document views = PiwikHelper.loadXMLFromString(FileUtils.readFileToString(vrXml));
        int pageViewsSum = ((Double)pageViewsSumX.evaluate(views, XPathConstants.NUMBER)).intValue();
        int uniqPageViewsSum = ((Double)uniqPageViewsSumX.evaluate(views, XPathConstants.NUMBER)).intValue();

        assertEquals("Downloads not merged properly", downSum, mergedDownSum);
        assertEquals("Uniq downloads not merged properly", uniqDownSum, mergedUniqDownSum);
        assertEquals("Page views not merged properly", pageViewsSum, mergedPageViewsSum);
        assertEquals("Uniq page views not merged properly", uniqPageViewsSum, mergedUniqPageViewsSum);
    }

    @Test
    public void testMergeSanity() throws Exception{
        URL url = this.getClass().getResource("/piwik/download_report.xml");
        File drXml = new File(url.getFile());
        url = this.getClass().getResource("/piwik/view_report.xml");
        File vrXml = new File(url.getFile());
        url = this.getClass().getResource("/piwik/view_report.json");
        File vrJson = new File(url.getFile());
        url = this.getClass().getResource("/piwik/download_report.json");
        File drJson = new File(url.getFile());

        String xmlResult = PiwikHelper.mergeXML(FileUtils.readFileToString(vrXml), FileUtils.readFileToString(drXml));
        String jsonResult = PiwikHelper.mergeJSON(FileUtils.readFileToString(vrJson), FileUtils.readFileToString(drJson));

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression resCountX = xpath.compile("count(//result)");
        XPathExpression pageViewsSumX = xpath.compile("sum(//nb_pageviews/text())");
        XPathExpression uniqPageViewsSumX = xpath.compile("sum(//nb_uniq_pageviews/text())");
        XPathExpression downSumX = xpath.compile("sum(//nb_downloads/text())");
        XPathExpression uniqDownSumX = xpath.compile("sum(//nb_uniq_downloads/text())");

        Document doc = PiwikHelper.loadXMLFromString(xmlResult);
        int xmlResCount = ((Double)resCountX.evaluate(doc, XPathConstants.NUMBER)).intValue();
        int xmlPageViewsSum = ((Double)pageViewsSumX.evaluate(doc, XPathConstants.NUMBER)).intValue();
        int xmlUniqPageViewsSum = ((Double)uniqPageViewsSumX.evaluate(doc, XPathConstants.NUMBER)).intValue();
        int xmlDownSum = ((Double)downSumX.evaluate(doc, XPathConstants.NUMBER)).intValue();
        int xmlUniqDownSum = ((Double)uniqDownSumX.evaluate(doc, XPathConstants.NUMBER)).intValue();

        JSONParser parser = new JSONParser();
        JSONObject reportJSON = (JSONObject)parser.parse(jsonResult);
        int jsonResCount = reportJSON.size();
        int jsonPageViewsSum = 0;
        int jsonUniqPageViewsSum = 0;
        int jsonDownSum = 0;
        int jsonUniqDownSum = 0;
        for(Object key : reportJSON.keySet()) {
            if(reportJSON.get(key) instanceof JSONObject) {
                JSONObject result = (JSONObject) reportJSON.get(key);
                for (Object resultKey : result.keySet()) {
                    if ("nb_pageviews".equals(resultKey)) {
                        jsonPageViewsSum += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_pageviews".equals(resultKey)) {
                        jsonUniqPageViewsSum += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_downloads".equals(resultKey)) {
                        jsonDownSum += ((Long) result.get(resultKey)).intValue();
                    } else if ("nb_uniq_downloads".equals(resultKey)) {
                        jsonUniqDownSum += ((Long) result.get(resultKey)).intValue();
                    }
                }
            }else if(reportJSON.get(key) instanceof JSONArray){
                JSONArray arr = (JSONArray) reportJSON.get(key);
                assertEquals("Expecting empty array", 0, arr.size());
            }
        }
        assertEquals("Result count differs", jsonResCount, xmlResCount);
        assertEquals("Page views sum differs", jsonPageViewsSum, xmlPageViewsSum);
        assertEquals("Uniq page views sum differs", jsonUniqPageViewsSum, xmlUniqPageViewsSum);
        assertEquals("Download sum differs", jsonDownSum, xmlDownSum);
        assertEquals("Uniq download sum differs", jsonUniqDownSum, xmlUniqDownSum);
    }

}