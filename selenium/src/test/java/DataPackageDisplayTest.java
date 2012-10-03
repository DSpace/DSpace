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

public class DataPackageDisplayTest extends TestCase {
    private WebDriver driver;
    private String baseUrl="http://datadryad.org";
    private StringBuffer verificationErrors = new StringBuffer();

    @Before
    public void setUp() throws Exception {
	driver = new HtmlUnitDriver();
	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }
    
    @Test
    public void testDataPackageDisplay() throws Exception {
	driver.get(baseUrl + "/handle/10255/dryad.20");
	assertEquals("Data from: Testing for unequal rates of morphological diversification in the absence of a detailed phylogeny: a case study from characiform fishes", driver.getTitle());
	assertTrue(isElementPresent(By.cssSelector("h1.ds-div-head")));
	assertTrue("head contains title", sectionContains("h1.ds-div-head", "Data from: Testing"));
	assertTrue("citation contains publication info", sectionContains("div.citation-view", "(2007) Testing"));
	assertTrue("citation contains package info", sectionContains("div.citation-view", "(2007) Data from: Testing"));
	assertTrue("spatial coverage", sectionContains("div.primary","South America"));
	assertTrue("data file description", sectionContains("div.primary","Relative Warps"));
	assertTrue("data file size", sectionContains("div.primary","131.5Kb"));
	assertTrue("file download link", isElementPresent(By.linkText("Sidlauskas 2007 Data.xls")));
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
