/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.submit.step;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

import org.jdom.Element;
import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;

/**
 * Test class for org.dspace.submit.step.SelectPublicationStep
 * from dryad-repo/dspace/modules/api
 * 1) Verify that the CrossRef API used for auto-filling data package metadata
 *    hasn't been deprecated or reconfigured
 * @author Nathan Day
 */
public class SelectPublicationStepTest {
    private static Logger log = Logger.getLogger(SelectPublicationStepTest.class);
    
    // UPDATE HERE IF org.dspace.submit.step.SelectPublicationStep changes
    public final static String crossRefApiRoot = "http://api.crossref.org/works/";
    public final static String crossRefApiFormat = "/transform/application/vnd.crossref.unixref+xml";

    /**
     * Partial test of processDOI method (minus crosswalk), of class SelectPublicationStep.
     */
    @Test
    public void processDOIRetrieval() throws Exception {
        try {
            String url = crossRefApiRoot
                       + "10.1007/s003590050092"
                       + crossRefApiFormat;        
            log.debug("Trying Crossref url: " + url);
            Element jElement = retrieveXML(url);
            assertNotNull(jElement);
            log.debug("Confirming Crossref xml is valid.");
            assertTrue(isAValidDOI(jElement));
        } catch (Exception ex){
            fail("Could not retrieve Crossref XML.");
        }
    }   
    
    // UPDATE HERE IF org.dspace.submit.step.SelectPublicationStep changes
    private static boolean isAValidDOI(Element element) {
        List<Element> children = element.getChildren();
        for(Element e : children){
            if(e.getName().equals("doi_record")){
                List<Element> doiRecordsChildren = e.getChildren();
                for(Element e1 : doiRecordsChildren){

                    if(e1.getName().equals("crossref")){
                        List<Element> crossRefChildren = e1.getChildren();
                        for(Element e2 : crossRefChildren){
                            if(e2.getName().equals("error")){
                                return false;
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return true;
    }
    // UPDATE HERE IF org.dspace.submit.step.SelectPublicationStep changes
    private Element retrieveXML(String urls) throws Exception{
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document doc = builder.build(urls);
        return doc.getRootElement();
    }

}
