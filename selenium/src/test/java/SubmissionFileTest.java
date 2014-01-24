package test;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import org.openqa.selenium.*;

public class SubmissionFileTest {
    private WebDriver driver;
    private String baseUrl = System.getProperty("selenium_test_url");
    private String uploadFile;
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

  @Before
  public void setUp() throws Exception {
    driver = new SilentHtmlUnitDriver();
    uploadFile = "/opt/dryad/config/workflow.xml";
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSubmission() throws Exception {
    // Root page
    driver.get(baseUrl + "/");

    // Click Submit and login
    driver.findElement(By.linkText("Submit data now")).click();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("seleniumtest@datadryad.org");
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
    driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("seleniumtest");
    driver.findElement(By.cssSelector("#aspect_eperson_PasswordLogin_item_loginsubmit-item > div.ds-form-content > #aspect_eperson_PasswordLogin_field_submit")).click();

    // 0. Select Journal
    driver.findElement(By.id("xmlui_submit_publication_article_status_accepted")).click();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_prism_publicationName")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_prism_publicationName")).sendKeys("Automated Test Submission Journal");
    driver.findElement(By.name("manu_accepted-cb")).click();
    driver.findElement(By.name("license_accept")).click();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();
    
    // 1. Describe Publication
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).sendKeys("IGNORE: Automated Test Submission");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).sendKeys("Test");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).sendKeys("Selenium");
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();
    
    // 2.1 Upload Data File 1/3
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dataset-file")).clear();
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dataset-file")).sendKeys(uploadFile);
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).clear();
    String title = "IGNORE: Automated Test Submission File";
    driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).sendKeys(title);
    driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();

    // Assert that the page is reached and contains Submission overview
    assertTrue("Submission overview after file upload", driver.findElement(By.cssSelector("h1.ds-div-head")).getText().contains("Submission overview"));

    // Assert that the uploaded file is listed on the page
    assertTrue("Uploaded file appears on page", driver.findElement(By.id("aspect_submission_submit_OverviewStep_list_datasets")).getText().contains(title));

    // 3. Delete submission - can't get this working with selenium
    // 4. Logout
    driver.findElement(By.linkText("Logout")).click();
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
