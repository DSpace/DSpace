package it;

import java.util.List;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class BtnZoomLightbox extends WidgetSeleniumTest {

  @Before
  public void setUp() throws Exception {
      super.setUp();
  }
  @After
  public void tearDown() throws Exception {
      super.tearDown();
  }

  @Test
  public void testBtnZoomLightbox() throws Exception {    
    String btn_selector = "a.dryad-ddw-zoom";
    String close_selector = "button.mfp-close";
    driver.get(baseUrl + "/test.html");
    waitOnWidgetLoaded();
    
    // into widget frame
    driver.switchTo().frame(0);
    List<WebElement> es = driver.findElements(By.cssSelector(btn_selector));
    assertTrue(es.size() > 0);
    for (WebElement e : es) {
       if (e.isDisplayed()) { 
            e.click();
            break;
        }
    }
    
    // out of frame
    driver.switchTo().defaultContent(); 
    driver.findElement(By.cssSelector(close_selector)).click();
  }
}
