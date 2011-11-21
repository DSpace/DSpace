package test;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.openqa.selenium.support.ui.Select;

public class SubmissionTest extends TestCase {
    private WebDriver driver;
    private String baseUrl="http://datadryad.org";
    private StringBuffer verificationErrors = new StringBuffer();
    
    @Before
    public void setUp() throws Exception {
	driver = new HtmlUnitDriver();
	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }
    
    @Test
    public void testJournalMetadataImport() throws Exception {

	// login
	driver.get(baseUrl+ "/");
	driver.findElement(By.linkText("Login")).click();
	driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
	driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("seleniumtest@datadryad.org");
	driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
	driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("seleniumtest");
	WebElement loginBox = driver.findElement(By.id("aspect_eperson_PasswordLogin_div_login"));
	loginBox.findElement(By.id("aspect_eperson_PasswordLogin_field_submit")).click();
	
	// begin submission with a known manuscript (using the URL interface to set the fields)
	driver.get(baseUrl + "/submit?journalID=SystBiol&manu=test1");
	driver.findElement(By.name("license_accept")).click();
	driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();

	// assert the the page is correct and the correct manuscript metadata was imported
	assertEquals("Dryad Submission", driver.getTitle());
	assertTrue("imported keywords contain Nummulites", idContains("aspect_submission_StepTransformer_div_submit-describe-publication", "Nummulites"));
		
	// remove the partial submission
	driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_cancel")).click();
	driver.findElement(By.id("aspect_submission_submit_SaveOrRemoveStep_field_submit_remove")).click();
    }

    @After
    public void tearDown() throws Exception {
	driver.quit();
	String verificationErrorString = verificationErrors.toString();
	if (!"".equals(verificationErrorString)) {
	    fail(verificationErrorString);
	}
    }


    private boolean idContains(String cssID, String targetText) {
	return driver.findElement(By.id(cssID)).getText().contains(targetText);
    }

    
    private boolean cssClassContains(String cssClass, String targetText) {
	return driver.findElement(By.cssSelector(cssClass)).getText().contains(targetText);
    }

    
    private boolean isElementPresent(By by) {
	try {
	    driver.findElement(by);
	    return true;
	} catch (NoSuchElementException e) {
	    return false;
	}
    }
}
