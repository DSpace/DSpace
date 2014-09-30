package it;

import java.io.File;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BtnDownload extends TestCase {
  private WebDriver driver;
  private String baseUrl;
  private File downloadDir;
  private String downloadDirName = "selenium_downloads";
  private long dlWaitTimeout = 5;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
    baseUrl = System.getProperty("seleniumTestURL");
    downloadDir = new File(System.getProperty("user.dir") + "/" + downloadDirName);
    if (!downloadDir.exists()) {
        downloadDir.mkdir();
    }
    downloadDir.deleteOnExit();
    FirefoxProfile fp = new FirefoxProfile();
    fp.setPreference("browser.download.folderList",2);
    fp.setPreference("browser.download.manager.showWhenStarting",false);
    fp.setPreference("browser.helperApps.neverAsk.saveToDisk","text/csv");
    fp.setPreference("browser.download.dir", downloadDir.getAbsolutePath());
    driver = new FirefoxDriver(fp);
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }
  @After
  public void tearDown() throws Exception {
    driver.quit();
    if (downloadDir.exists()) {
        for (File f : downloadDir.listFiles()) {
            f.delete();
        }
        downloadDir.delete();
    }
    String verificationErrorString = verificationErrors.toString();
    if (!"".equals(verificationErrorString)) {
      fail(verificationErrorString);
    }
  }

  @Test
  public void testBtnDownload() throws Exception {
    String btn_selector = "i.fa.fa-download:nth-of-type(1)";
    driver.get(baseUrl + "/test.html");
    
    // into widget frame
    driver.switchTo().frame(0);
    assertTrue(isElementPresent(By.cssSelector(btn_selector)));
    driver.findElement(By.cssSelector(btn_selector)).click();

    final File dlFile = new File(downloadDir + "/" + "invert.data-may19.csv");
    dlFile.deleteOnExit();
    
    ExpectedCondition<Boolean> fileExistsCondition = new ExpectedCondition<Boolean>() {
        @Override
        public Boolean apply(WebDriver f) {
System.out.println("HERE: " + dlFile.getPath());
            return dlFile.exists();
        }
    };

    WebDriverWait wait = new WebDriverWait(driver,dlWaitTimeout);
    wait.until(fileExistsCondition);
    assertTrue("Downloaded file exists: " + new File(downloadDir + "/" + "invert.data-may19.csv").getAbsolutePath(), dlFile.exists());
    dlFile.delete();
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
