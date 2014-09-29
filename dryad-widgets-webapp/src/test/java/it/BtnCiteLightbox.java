package it;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class BtnCiteLightbox extends TestCase {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    // baseUrl = "http://localhost:1234/dryad-widgets-webapp/src/test/java/it";
    baseUrl = System.getProperty("selenium_test_url"); 
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testBtnCiteLightbox() throws Exception {
    driver.get(baseUrl + "/test.html");
System.out.println("1");
    assertTrue(isElementPresent(By.cssSelector("#ddw-body > div.dryad-ddw-control > ul > li:nth-child(4) > a > i")));
System.out.println("2");
    driver.findElement(By.cssSelector("#ddw-body > div.dryad-ddw-control > ul > li:nth-child(4) > a > i")).click();
System.out.println("3");
    // ERROR: Caught exception [Error: locator strategy either id or name must be specified explicitly.]
    driver.findElement(By.cssSelector("button.mfp-close")).click();
System.out.println("4");
    // ERROR: Caught exception [Error: locator strategy either id or name must be specified explicitly.]
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

  private boolean isElementPresent(By by) {
    try {
      driver.findElement(by);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  private boolean isAlertPresent() {
    try {
      driver.switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      return false;
    }
  }

  private String closeAlertAndGetItsText() {
    try {
      Alert alert = driver.switchTo().alert();
      String alertText = alert.getText();
      if (acceptNextAlert) {
        alert.accept();
      } else {
        alert.dismiss();
      }
      return alertText;
    } finally {
      acceptNextAlert = true;
    }
  }
}
