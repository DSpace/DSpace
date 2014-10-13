
package it;

import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import static junit.framework.Assert.assertTrue;
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

  protected long driverTimeoutSeconds = 10;
  protected long widgetLoadedSecondsTimeout = 10;
  protected long widgetPopupWaitSecondsTimeout = 10;

  // some common selectors
  protected String lightbox_container_selector = "div.mfp-container";
  protected String lightbox_close_selector = "button.mfp-close";

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

  protected void waitUntilElementAbsent(final By waitBy, long waitSeconds) {
    ExpectedCondition<Boolean> elementExistsCondition = new ExpectedCondition<Boolean>() {
        @Override
        public Boolean apply(WebDriver f) {
            return !isElementPresent(waitBy);
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

  protected boolean clickFirstDisplayedInFrame(int frameNo, By by) {
    driver.switchTo().frame(frameNo);
    List<WebElement> es = driver.findElements(by);
    assertTrue(es.size() > 0);
    Boolean buttonClicked = false;
    for (WebElement e : es) {
       if (e.isDisplayed()) {
            e.click();
            buttonClicked = true;
            break;
        }
    }
    driver.switchTo().defaultContent();
    return buttonClicked;
  }

}
