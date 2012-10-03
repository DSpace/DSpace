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
import org.openqa.selenium.support.ui.Select;

public class HomePageTest extends TestCase {
    private WebDriver driver;
    private String baseUrl="http://datadryad.org";
    private StringBuffer verificationErrors = new StringBuffer();

    @Before
    public void setUp() throws Exception {
	driver = new HtmlUnitDriver();
	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }
    
    @Test
    public void testHomePageDisplay() throws Exception {
	driver.get(baseUrl + "/");
	assertEquals("Dryad Home", driver.getTitle());
	assertTrue("recently published list", isElementPresent(By.cssSelector("li.ds-artifact-item.even")));
    }
    
    @After
    public void tearDown() throws Exception {
	driver.quit();
	String verificationErrorString = verificationErrors.toString();
	if (!"".equals(verificationErrorString)) {
	    fail(verificationErrorString);
	}
    }
    
    private boolean sectionContains(String section, String targetText) {
	return driver.findElement(By.cssSelector(section)).getText().contains(targetText);
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
