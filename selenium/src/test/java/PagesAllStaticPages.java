
package test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * Confirm that the static /pages/{...} pages load.
 * 
 * @author Nathan Day
 */
public class PagesAllStaticPages extends PagesBase {

    private final static String pageRoot = "/pages/";
    // time to wait for the refreshing pages to refresh
    private final static long refreshSleep = 2000;
    // time to wait for a page to load completely
    private final static long loadSleep = 500;
    // @id attr for the page not found <div>
    private final static String pageNotFoundId = "aspect_general_PageNotFoundTransformer_div_page-not-found";

    // non-redirecting pages
    private static final List<String> pageNames200 = Arrays.asList(
        "dryadlab", "employment", "error", "faq", "filetypes", "institutionalSponsors",
        "jdap", "journalLookup", "maintenance", "membershipMeeting", "membershipMeeting2013",
        "membershipMeeting2014", "membershipMeeting2015", "membershipOverview",
        "organization", "ourTeam", "payment", "paymentPlanComparisonTool",
        "policies", "publicationBlackout", "readme", "repository", "searching",
        "submissionIntegration"
    );
    // this pagees have a refresh:
    //      <meta http-equiv="refresh" content="0;URL=..."/>
    // the map is in the form
    //      { k:"from-page", v:"to-page" }
    private static final Map<String,String> pageNamesRefresh = new HashMap();
    static {
        pageNamesRefresh.put("integratedJournals","journalLookup");
        pageNamesRefresh.put("journalIntegration","submissionIntegration");
        pageNamesRefresh.put("pricing","payment");
        pageNamesRefresh.put("pricingPlanComparisonTool","paymentPlanComparisonTool");
        pageNamesRefresh.put("whoWeAre","ourTeam");
    }    

    // confirm that the static pages load and are not the not-found page
    // (which is returned with a 200 code)
    public void testPageLoad() throws Exception {
        for (String p : pageNames200) {
            String url = baseUrl + pageRoot + p;
            driver.get(url);
            sleepMS(loadSleep);
            // will raise an exception 
            WebElement pnfDiv = null;
            try {
                pnfDiv = driver.findElement(By.id(pageNotFoundId));
            } catch(NoSuchElementException e) {}
            assertTrue("Not-found div found in page", pnfDiv == null);
        }
    }

    // confirm that these static pages refresh
    public void testPageRefresh() throws Exception {
        for (Entry<String,String> e : pageNamesRefresh.entrySet()) {
            String from = baseUrl + pageRoot + e.getKey();
            String to = baseUrl + pageRoot + e.getValue();
            driver.get(from);
            sleepMS(refreshSleep);
            String current = driver.getCurrentUrl(); 
            String msg = String.format("Refresh to %s expected but current url is: %s", to, current);
            assertTrue(msg , current.equals(to));
        }
    }

}
