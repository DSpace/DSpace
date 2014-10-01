package it;

import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.By;

public class WidgetLoad extends WidgetSeleniumTest {

  @Before
  public void setUp() throws Exception {
      super.setUp();
  }
  @After
  public void tearDown() throws Exception {
      super.tearDown();
  }

  @Test
  public void testWidgetLoad() throws Exception {
    driver.get(baseUrl + "/test.html");
    waitOnWidgetLoaded();
    
    driver.switchTo().frame(0);
    assertTrue(isElementPresent(By.cssSelector("img[alt=\"Data in Dryad\"]")));
  }
}
