
package it;

import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;

public class BtnCiteLightbox extends WidgetSeleniumTest {

  @Before
  public void setUp() throws Exception {
      super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testBtnCiteLightbox() throws Exception {
    String btn_selector = "i.fa.fa-quote-left:nth-of-type(1)";
    String close_selector = "button.mfp-close";
    String citation_popup_selector = "#dryad-ddw-citation";
    driver.get(baseUrl + "/test.html");

    // wait until the widget's frame has loaded 
    waitOnWidgetLoaded();

    // click quote button in widget frame
    driver.switchTo().frame(0);
    assertTrue(isElementPresent(By.cssSelector(btn_selector)));
    driver.findElement(By.cssSelector(btn_selector)).click();

    // confirm quote content visible outer page
    driver.switchTo().defaultContent();
    By popupBy = new By.ByCssSelector(citation_popup_selector);
    waitUntilElementPresent(popupBy,widgetPopupWaitSecondsTimeout);
    assertTrue(isElementPresent(popupBy));

    // out of frame
    driver.switchTo().defaultContent();
    assertTrue(isElementPresent(By.cssSelector(close_selector)));
    driver.findElement(By.cssSelector(close_selector)).click();
  }
}
