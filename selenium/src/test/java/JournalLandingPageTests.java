
package test;

import java.net.HttpURLConnection;
import java.net.URL;
import static junit.framework.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class JournalLandingPageTests extends PagesBase {

    private final static String goodPage = "/journal/Evolution";
    private final static String badPage = "/journal/__asdf--asdfasdfa";
    
    private final static long clickWait = 250; // ms after tab click
    private final static long pageLoadWait = 250;
    
    // minimal set of banner paragraphs
    private List<String> bannerParaXpaths = Arrays.asList(
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-spo']",
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-int']",
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-aut']",
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-dat']",
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-met']",
        "//p[@id='aspect_journal_landing_Banner_p_journal-landing-banner-pac']"
    );
    private String searchFormXpath = "//form[@id='aspect_journal_landing_JournalSearch_div_journal-landing-search']";
    private String searchInputXpath = "//input[@id='aspect_journal_landing_JournalSearch_field_query']";
    
    private int buttonMin = 1;
    private int buttonMax = 4;
    private String tabButtonFmt = "//a[@href='#aspect_journal_landing_JournalStats_div_journal-landing-stats-%d']";
    private String tabDivFmt = "//div[@id='aspect_journal_landing_JournalStats_div_journal-landing-stats-%d']";
    
    private String searchText = "test text for Evolution search";
    private String searchResultsPageTitle = "Search Results - Dryad";
    private String searchUrlPath = "/discover?";
    
    // confirm minimal content for page banner
    public void testBannerLoaded() throws Exception {
        String pageUrl = baseUrl + goodPage;
        driver.get(pageUrl);
        sleepMS(pageLoadWait);
        assertTrue(xpathsPresent(bannerParaXpaths));
        driver.close();
    }

    // cycle through the tab buttons and confirm panel contents visibility
    public void testBrowseButtons() throws Exception {
        String pageUrl = baseUrl + goodPage;
        driver.get(pageUrl);
        for (int i = buttonMin; i <= buttonMax; ++i) {
            driver.findElement(By.xpath(String.format(tabButtonFmt, i))).click();
            sleepMS(clickWait);
            assertTrue(driver.findElement(By.xpath(String.format(tabDivFmt, i))).isDisplayed());
        }
        driver.close();
    }
    
    // confirm that the search page loads when the search form is submitted
    public void testSearchForm() throws Exception {
        String pageUrl = baseUrl + goodPage;
        driver.get(pageUrl);
        sleepMS(pageLoadWait);
        WebElement form = driver.findElement(By.xpath(searchFormXpath));
        WebElement input = driver.findElement(By.xpath(searchInputXpath));
        assertTrue(input.isDisplayed());
        input.sendKeys(searchText);
        form.submit();
        assertTrue(driver.getTitle().equals(searchResultsPageTitle));
        String expectedUrlBasePath = baseUrl + searchUrlPath;
        String actualUrlAll = driver.getCurrentUrl();
        assertTrue(actualUrlAll.length() >= expectedUrlBasePath.length());
        assertTrue(actualUrlAll.substring(0, expectedUrlBasePath.length()).equals(expectedUrlBasePath));        
        driver.close();
    }    
    
    // confirm 404 for non-existant journal
    // NOTE: http request w/out selenium webdriver, but does require a 
    // running Dryad application
    public void test404() throws Exception {
        String badPageUrl = baseUrl + badPage;
        URL url = new URL(badPageUrl);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.connect();
        assertTrue(huc.getResponseCode() == 404);
    }
    
}
