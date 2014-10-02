package it;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BtnDownload extends WidgetSeleniumTest {
  private File downloadDir;
  private String downloadDirName = "selenium_downloads";
  private long dlWaitTimeout = 5;

  @Before
  public void setUp() throws Exception {
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
    super.setUp();
  }
  @After
  public void tearDown() throws Exception {
    if (downloadDir.exists()) {
        for (File f : downloadDir.listFiles()) {
            f.delete();
        }
        downloadDir.delete();
    }
    super.tearDown();
  }

  @Test
  public void testBtnDownload() throws Exception {
    String btn_selector = "a.dryad-ddw-download";
    driver.get(baseUrl + "/test.html");
    waitOnWidgetLoaded();

    // into widget frame
    // click button in widget frame
    Boolean buttonWasClicked = clickFirstDisplayedInFrame(0, By.cssSelector(btn_selector));
    assertTrue(buttonWasClicked);

    final File dlFile = new File(downloadDir + "/" + "invert.data-may19.csv");
    dlFile.deleteOnExit();
    ExpectedCondition<Boolean> fileExistsCondition = new ExpectedCondition<Boolean>() {
        @Override
        public Boolean apply(WebDriver f) {
            return dlFile.exists();
        }
    };

    WebDriverWait wait = new WebDriverWait(driver,dlWaitTimeout);
    wait.until(fileExistsCondition);
    assertTrue("Downloaded file exists: " + new File(downloadDir + "/" + "invert.data-may19.csv").getAbsolutePath(), dlFile.exists());
    dlFile.delete();
  }
}
