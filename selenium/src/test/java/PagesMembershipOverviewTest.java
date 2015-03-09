
package test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map.Entry;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class PagesMembershipOverviewTest extends PagesBase {

    private final static String jsonDataPath = "/static/json/membership-form.json";
    private final static String membershipOverviewPage = "/pages/membershipOverview";
    private static final List<String> currencies = Arrays.asList(
        "USD", "EUR", "GBP", "CAD", "JPY", "AUD"
    );
    private static final String defaultCurrency = "USD";
    private static final ObjectMapper mapper = new ObjectMapper();

    // make sure data file can be retrieved and has expected currencies
    public void testJsonAvailable() throws Exception {
        String jsonUrl = baseUrl + jsonDataPath;
        Map<String, Map<String, String>> membershipData = mapper.readValue(new URL(jsonUrl), Map.class);
        assertTrue(membershipData.keySet().containsAll(currencies));
    }

    // confirm an option for each currency in data file
    public void testJsonDataLoaded() throws Exception {
        String jsonUrl = baseUrl + jsonDataPath;
        Map<String, Map<String, String>> membershipData = mapper.readValue(new URL(jsonUrl), Map.class);
        assertTrue(membershipData.keySet().containsAll(currencies));
        
        String pageUrl = baseUrl + membershipOverviewPage;
        driver.get(pageUrl);
        waitOnXpathsPresent(Arrays.asList("//select[@id='displayed-currency']/option[@value='" + defaultCurrency + "']"));
        List<WebElement> options = driver.findElements(By.xpath("//select[@id='displayed-currency']/option"));
        assertTrue(options.size() == currencies.size());
        driver.close();
    }

    // click on the "Show all amounts in" options and confirm updates on page
    public void testChangeCurrency() throws Exception {
        String jsonUrl = baseUrl + jsonDataPath;
        Map<String, Map<String, String>> membershipData = mapper.readValue(new URL(jsonUrl), Map.class);
        assertTrue(membershipData.keySet().containsAll(currencies));
        
        String pageUrl = baseUrl + membershipOverviewPage;
        driver.get(pageUrl);
        waitOnXpathsPresent(Arrays.asList("//select[@id='displayed-currency']/option[@value='" + defaultCurrency + "']"));

        for (Entry<String, Map<String, String>> entry : membershipData.entrySet()) {
            String currency = entry.getKey();
            Map<String, String> values = entry.getValue();
            WebElement elt = driver.findElement(By.xpath("//select[@id='displayed-currency']/option[@value='" + currency + "']"));
            elt.click();
            for (Entry<String,String> value: values.entrySet()) {
                String klass = value.getKey();
                String amount = value.getValue();
                WebElement span = driver.findElement(By.className(klass));
                assertTrue(span.getText().equals(amount));
            }
        }
        driver.close();
    }
    
}
