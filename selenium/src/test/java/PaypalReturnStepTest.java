package test;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import junit.framework.TestCase;

import org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;

/**
 * Tests that the payment endpoint /submit-paypal-checkout is responding
   The Paypal payment processor POSTs to this address to complete a transaction
   While the test doesn't check for specific behaviors, it confirms our endpoint
   is handling requests, and that the Cocoon pipeline is not failing.
*/
public class PaypalReturnStepTest extends TestCase {
    private WebDriver driver;
    private String baseUrl = System.getProperty("selenium_test_url");

    @Before
  public void setUp() throws Exception {
    driver = new SilentHtmlUnitDriver();
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testPaymentEndpointResponds() throws Exception {
    driver.get(baseUrl + "/submit-paypal-checkout");
    assertTrue(isElementPresent(By.cssSelector("div.ds-static-div")));
    assertTrue("body contains 'error response from paypal", sectionContains("p.ds-paragraph", "error response from paypal"));

  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
  }

    private boolean sectionContains(String section, String targetText) {
  return driver.findElement(By.cssSelector(section)).getText().contains(targetText);
    }

  private boolean isElementPresent(By by) {
    try {
      driver.findElement(by);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }
}
