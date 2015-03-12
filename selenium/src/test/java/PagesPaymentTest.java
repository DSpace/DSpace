
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

public class PagesPaymentTest extends PagesBase {

    private final static String jsonDataPath = "/static/json/payment-plan.json";
    private final static String paymentPage = "/pages/payment";
    private static final List<String> currencies = Arrays.asList(
        "USD", "EUR", "GBP", "CAD", "JPY", "AUD"
    );
    private static final String defaultCurrency = "USD";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> displayClasses = Arrays.asList(
        "msg-memberDPC_voucher" ,"msg-nonMemberDPC_voucher" ,"msg-memberDPC_deferred"
        ,"msg-nonMemberDPC_deferred" ,"msg-memberDPC_subscription" 
        ,"msg-nonMemberDPC_subscription" ,"msg-DPC_pay_on_submission"
        ,"msg-excessDataStorageFee_first_GB" ,"msg-excessDataStorageFee_per_additional_GB"
    );

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
        
        String pageUrl = baseUrl + paymentPage;
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
        
        String pageUrl = baseUrl + paymentPage;
        driver.get(pageUrl);
        waitOnXpathsPresent(Arrays.asList("//select[@id='displayed-currency']/option[@value='" + defaultCurrency + "']"));

        for (Entry<String, Map<String, String>> entry : membershipData.entrySet()) {
            String currency = entry.getKey();
            Map<String, String> values = entry.getValue();
            WebElement elt = driver.findElement(By.xpath("//select[@id='displayed-currency']/option[@value='" + currency + "']"));
            elt.click();
            for (Entry<String,String> value: values.entrySet()) {
                String klass = "msg-" + value.getKey();
                if (displayClasses.contains(klass)) {
                    String amount = value.getValue();
                    WebElement span = driver.findElement(By.className(klass));
                    assertTrue(span.getText().equals(amount));
                }
            }
        }
        driver.close();
    }
}
