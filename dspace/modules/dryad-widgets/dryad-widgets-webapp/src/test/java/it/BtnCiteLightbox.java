
package it;

import java.util.List;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
    String btn_selector = "a.dryad-ddw-cite";
    String citation_popup_selector = "#dryad-ddw-citation";
    driver.get(baseUrl + "/test.html");
    waitOnWidgetLoaded();

    // click button in widget frame
    Boolean buttonWasClicked = clickFirstDisplayedInFrame(0, By.cssSelector(btn_selector));
    assertTrue(buttonWasClicked);

    // confirm quote content visible outer page
    waitUntilElementPresent(By.cssSelector(lightbox_container_selector), widgetPopupWaitSecondsTimeout);
    assertTrue(isElementPresent(By.cssSelector(citation_popup_selector)));

    // out of frame
    assertTrue(isElementPresent(By.cssSelector(lightbox_close_selector)));
    driver.findElement(By.cssSelector(lightbox_close_selector)).click();
    waitUntilElementAbsent(By.cssSelector(lightbox_close_selector),widgetLoadedSecondsTimeout);
    assertFalse(isElementPresent(By.cssSelector(lightbox_close_selector)));
  }
}
