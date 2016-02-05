
package test;

import java.net.URL;

import static junit.framework.Assert.assertTrue;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class PagesPaymentPlanComparisonTool extends PagesBase {

    private final static String jsonDataPath = "/static/json/payment-calculator.json";
    private final static String paymentPlanComparisonToolPage = "/pages/paymentPlanComparisonTool";

    private final static String calculateButtonXpath = "//a[@id='calculate_button']";
    private final static String apyXpath = "//input[@id='articles_per_year']";
    private final static String pwdXpath = "//input[@id='percentage_with_deposits']";
    private final static String under10mXpath = "//label[@for='under_10_million']";
    private final static String over10mXpath = "//label[@for='over_10_million']";
    private final static String percentageErrorXpath = "//div[@id='percentage_with_deposits_error']";
    private final static String articlesErrorXpath = "//div[@id='articles_per_year_error']";
    private final static long postClickSleepMS = 500;
    
    // rows in table
    private static final List<String> plans = Arrays.asList(
        "plan-sustainer", "plan-supporter", "plan-advocate", "plan-nonmember"
    );
    // columns in table
    private List<String> columns = Arrays.asList("cost-voucher", "cost-deferred", "cost-subscription");    
    
    private static final ObjectMapper mapper = new ObjectMapper();

    // make sure data file can be retrieved and has expected currencies
    public void testJsonAvailable() throws Exception {
        String jsonUrl = baseUrl + jsonDataPath;
        Map<String, Map<String, String>> membershipData = mapper.readValue(new URL(jsonUrl), Map.class);
        assertTrue(membershipData.keySet().containsAll(plans));
    }

    @Ignore
    public class PageResult {
        public String articlesPerYear;
        public String percentageWithDeposits;
        public boolean under10m;
        public List<List<String>> table;
        public boolean error;
        public List<String> errorXpaths;
        public PageResult(String a, String p, Boolean l, List<List<String>> t, boolean e, List<String> exps) {
            this.articlesPerYear = a;
            this.percentageWithDeposits = p;
            this.under10m = l;
            this.table = t;
            this.error = e;
            this.errorXpaths = exps;
        }
    }
    private List<List<String>> t1 = Arrays.asList(
        Arrays.asList("n/a","n/a","n/a"),
        Arrays.asList("$4,250","$4,500","$3,500"),
        Arrays.asList("$4,000","$4,250","$3,500"),
        Arrays.asList("$3,500","$3,750","$3,000")
    );
    private List<List<String>> t2 = Arrays.asList(
        Arrays.asList("$8,250","$8,500","$7,500"),
        Arrays.asList("n/a","n/a","n/a"),
        Arrays.asList("$4,000","$4,250","$3,500"),
        Arrays.asList("$3,500","$3,750","$3,000")
    );
    private List<PageResult> pageResults = Arrays.asList(
        new PageResult("100", "50", true,  t1,   false, null),
        new PageResult("100", "50", false, t2,   false, null),
        new PageResult("",    "50", true,  null, true,  Arrays.asList(percentageErrorXpath)),
        new PageResult("100", "",   true,  null, true,  Arrays.asList(articlesErrorXpath)),
        new PageResult("",    "",   true,  null, true,  Arrays.asList(percentageErrorXpath, articlesErrorXpath))
    );
    
    // click on the "Show all amounts in" options and confirm updates on page
    public void testChangeCurrency() throws Exception {
        String jsonUrl = baseUrl + jsonDataPath;
        Map<String, Map<String, String>> membershipData = mapper.readValue(new URL(jsonUrl), Map.class);
        assertTrue(membershipData.keySet().containsAll(plans));
        
        String pageUrl = baseUrl + paymentPlanComparisonToolPage;
        driver.get(pageUrl);
        
        WebElement btnCalculate = driver.findElement(By.xpath(calculateButtonXpath));
        
        for (PageResult r : pageResults) {
            //  How many research articles do you publish per year?
            WebElement apy = driver.findElement(By.xpath(apyXpath)); // articles_per_year
            apy.clear();
            apy.sendKeys(r.articlesPerYear);
            //  What percentage of articles do you expect will have data in Dryad?
            WebElement pwd = driver.findElement(By.xpath(pwdXpath)); // percentage_with_deposits
            pwd.clear();
            pwd.sendKeys(r.percentageWithDeposits);
            //  Is your gross annual income LESS than 10 Million Dollars? Yes No 
            WebElement radio = null;
            if (r.under10m) {
                radio = driver.findElement(By.xpath(under10mXpath));
            } else {
                radio = driver.findElement(By.xpath(over10mXpath));
            }
            radio.click();
            btnCalculate.click();
            sleepMS(postClickSleepMS);
            for (int i = 0; i < plans.size(); ++i) {
                for (int j = 0; j < columns.size(); ++j) {
                    if (r.error) {                        
                        assertTrue(xpathsPresent(r.errorXpaths));
                    } else {
                        String xp = "//tr[@id='" + plans.get(i) + "']/td[@class='" + columns.get(j) + "']";
                        WebElement cell = driver.findElement(By.xpath(xp));
                        String expected = r.table.get(i).get(j);
                        String got = cell.getText();
                        String msg = "Table value for " + xp + " expected '" + expected + "' got '" + got + "'";
                        assertTrue(msg, got.equals(expected));
                    }
                    
                }
            }
            btnCalculate.click();
        }
        driver.close();
    }
    
}
