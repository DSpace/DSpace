package test;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import junit.framework.Assert;
import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.openqa.selenium.support.ui.Select;

public class DescribePublicationAJAXTest {
  private WebDriver driver;
  private String baseUrl;
  private boolean acceptNextAlert = true;
  private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    baseUrl = System.getProperty("selenium_test_url"); 
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testDescribePublicationAJAX() throws Exception {
    driver.get(baseUrl + "/");
    driver.findElement(By.id("login-item")).click();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("seleniumtest@datadryad.org");
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("seleniumtest");
    driver.findElement(By.cssSelector("#aspect_eperson_PasswordLogin_item_loginsubmit-item > div.ds-form-content > #aspect_eperson_PasswordLogin_field_submit")).click();
    driver.findElement(By.linkText("Submit data now")).click();
    new Select(driver.findElement(By.name("country"))).selectByVisibleText("Armenia");
    driver.findElement(By.id("xmlui_submit_publication_article_status_in_review")).click();
    new Select(driver.findElement(By.id("aspect_submission_StepTransformer_field_journalIDStatusInReview"))).selectByVisibleText("Dryad Testing Journal");
    driver.findElement(By.name("license_accept")).click();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).sendKeys("Ttitle for Test Manuscript");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).sendKeys("Smith");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).sendKeys("Donald F.");
    driver.findElement(By.name("submit_dc_contributor_author_add")).click();
    assertTrue(isElementPresent(By.cssSelector("span.ds-interpreted-field")));
    assertEquals("Smith, Donald F.", driver.findElement(By.cssSelector("span.ds-interpreted-field")).getText());
    driver.findElement(By.name("dc_contributor_author_selected")).click();
    driver.findElement(By.name("submit_dc_contributor_author_delete")).click();
    assertFalse(isElementPresent(By.cssSelector("span.ds-interpreted-field")));
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).sendKeys("Smith");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).sendKeys("Donald F.");
    driver.findElement(By.name("submit_dc_contributor_author_add")).click();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_cancel")).click();
    driver.findElement(By.id("aspect_submission_submit_SaveOrRemoveStep_field_submit_back")).click();
    assertTrue(isElementPresent(By.cssSelector("span.ds-interpreted-field")));
    assertEquals("Smith, Donald F.", driver.findElement(By.cssSelector("span.ds-interpreted-field")).getText());
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_cancel")).click();
    driver.findElement(By.id("aspect_submission_submit_SaveOrRemoveStep_field_submit_save")).click();
    driver.close();
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
