/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import junit.framework.TestCase;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;

/**
 *
 * @author rnathanday
 */
public class PagesBaseTest extends TestCase {
    
    protected WebDriver driver;
    protected String baseUrl;
    protected StringBuffer verificationErrors = new StringBuffer();
    
    // seconds to wait for an AJAX response and refresh; ms/s; Selenium max timeout
    protected final int ajaxWaitSeconds = 5;
    protected final int waitSleepInterval = 1000;
    protected final int maxWaitSeconds = 10;

    // this is a @Before method
    public void setUp() throws Exception {
        if ("Chrome".equals(System.getProperty("selenium_test_browser"))) {
            driver = new ChromeDriver();
        } else if ("Safari".equals(System.getProperty("selenium_test_browser"))) {
            driver = new SafariDriver();
        } else {
            LoggingPreferences logs = new LoggingPreferences();
            //logs.enable(LogType.DRIVER, Level.INFO);
            logs.enable(LogType.DRIVER, Level.SEVERE);
            logs.enable(LogType.BROWSER, Level.INFO);
            DesiredCapabilities capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);
            String ffbin = System.getProperty("firefox_binary");
            String ffdisp = System.getProperty("firefox_display");
            if (ffbin != null && !ffbin.equals("")) {
                FirefoxBinary binary = new FirefoxBinary(new File(ffbin));
                binary.setEnvironmentProperty("DISPLAY",ffdisp);
                driver = new FirefoxDriver(binary,null);
            } else {
                driver = new FirefoxDriver(capabilities);
            }
        }
        baseUrl = System.getProperty("selenium_test_url");
        assertTrue(baseUrl != null && !baseUrl.equals(""));
        driver.manage().timeouts().implicitlyWait(maxWaitSeconds, TimeUnit.SECONDS);
    }

    // NOTE: this key sequence is for OSX
    protected void openConsole() {
        if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
            driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.SHIFT, "j"));
        } else if (driver instanceof org.openqa.selenium.chrome.ChromeDriver) {
            driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.ALT, "j"));
        }
    }

    // this is an @After method
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

    protected void login() {
        driver.get(baseUrl + "/");
        driver.findElement(By.id("sign-up-item")).click();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("seleniumtest@datadryad.org");
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("seleniumtest");
        driver.findElement(By.cssSelector("#aspect_eperson_PasswordLogin_item_loginsubmit-item > div.ds-form-content > #aspect_eperson_PasswordLogin_field_submit")).click();
    }

    protected void waitOnXpathsPresent(List<String> xpaths) throws InterruptedException {
      // wait for form submission to complete
        for (int second = 0;; second++) {
            boolean done = true;
            if (second >= ajaxWaitSeconds) fail("timeout");
            for (String xpath : xpaths)
                done = done && isElementPresent(By.xpath(xpath));
            if (done) break;
            sleepMS(waitSleepInterval);
        }
    }
    
    // put the current thread to sleep for n ms.
    protected void sleepMS(long n) {
        try {
            Thread.sleep(n);
        } catch (Exception e) {}
    }
    
}
