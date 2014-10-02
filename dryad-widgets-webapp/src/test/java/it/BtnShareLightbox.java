package it;

import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;

public class BtnShareLightbox extends WidgetSeleniumTest {

  @Before
  public void setUp() throws Exception {
      super.setUp();
  }
  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }


  @Test
  public void testBtnShareLightbox() throws Exception {
   
    String btn_selector = "i.fa.fa-share-alt:nth-of-type(1)";
    String close_selector = "button.mfp-close";
    driver.get(baseUrl + "/test.html");
    waitOnWidgetLoaded();
    
    // click button in widget frame
    Boolean buttonWasClicked = clickFirstDisplayedInFrame(0, By.cssSelector(btn_selector));
    assertTrue(buttonWasClicked);
    
    // close lightbox
    assertTrue(isElementPresent(By.cssSelector(lightbox_close_selector)));
    driver.findElement(By.cssSelector(lightbox_close_selector)).click();
    waitUntilElementAbsent(By.cssSelector(lightbox_close_selector),widgetLoadedSecondsTimeout);
    assertFalse(isElementPresent(By.cssSelector(lightbox_close_selector)));
  }
}
