
package it;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

@Ignore
public class WidgetSeleniumTest extends TestCase {
  protected WebDriver driver;
  protected String baseUrl;
  protected boolean acceptNextAlert = true;
  protected StringBuffer verificationErrors = new StringBuffer();
  
  protected long driverTimeoutSeconds = 30;
  protected long widgetLoadedSecondsTimeout = 30;
  protected long widgetPopupWaitSecondsTimeout = 10;

  @Before
  public void setUp() throws Exception {
    baseUrl = System.getProperty("seleniumTestURL");
    if (driver == null) {
        driver = new FirefoxDriver();
    }
    driver.manage().timeouts().implicitlyWait(driverTimeoutSeconds, TimeUnit.SECONDS);
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

  protected boolean isElementPresent(By by) {
    try {
      driver.findElement(by);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }
  
  protected void waitUntilElementPresent(final By waitBy, long waitSeconds) {
    ExpectedCondition<Boolean> elementExistsCondition = new ExpectedCondition<Boolean>() {
        @Override
        public Boolean apply(WebDriver f) {
            return isElementPresent(waitBy);
        }
    };
    WebDriverWait wait = new WebDriverWait(driver, waitSeconds);
    wait.until(elementExistsCondition);
  }

  protected boolean isAlertPresent() {
    try {
      driver.switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      return false;
    }
  }

  protected String closeAlertAndGetItsText() {
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
  
  protected void waitOnWidgetLoaded() throws Exception {
    // wait until "iframe.dryad-ddw" is present
    By.ByCssSelector by = new By.ByCssSelector("iframe.dryad-ddw");
    waitUntilElementPresent(by,widgetLoadedSecondsTimeout);
    assertTrue(isElementPresent(By.cssSelector("iframe.dryad-ddw")));
  }  
  
  protected void executeJavascript(String js) {
	if (driver instanceof JavascriptExecutor) {
		//String ret = (String) ((JavascriptExecutor) driver).executeScript(js);
	}      
  }

}
