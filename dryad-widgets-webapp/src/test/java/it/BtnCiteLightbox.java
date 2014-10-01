
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
    driver.get(baseUrl + "/test.html");
    
    // wait until the widget's frame has loaded 
    waitOnWidgetLoaded();
    assertTrue(isElementPresent(By.cssSelector("iframe.dryad-ddw")));
    
    // into widget frame
    driver.switchTo().frame(0);
    assertTrue(isElementPresent(By.cssSelector(btn_selector)));
    driver.findElement(By.cssSelector(btn_selector)).click();
    
    // out of frame
    driver.switchTo().defaultContent(); 
    driver.findElement(By.cssSelector(close_selector)).click();
  }
}
