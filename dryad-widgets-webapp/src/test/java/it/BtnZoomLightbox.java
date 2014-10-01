package it;

import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;

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
    String btn_selector = "i.fa.fa-expand:nth-of-type(1)";
    String close_selector = "button.mfp-close";
    driver.get(baseUrl + "/test.html");
    
    // into widget frame
    driver.switchTo().frame(0);
    assertTrue(isElementPresent(By.cssSelector(btn_selector)));
    driver.findElement(By.cssSelector(btn_selector)).click();
    
    // out of frame
    driver.switchTo().defaultContent(); 
    driver.findElement(By.cssSelector(close_selector)).click();
  }
}
