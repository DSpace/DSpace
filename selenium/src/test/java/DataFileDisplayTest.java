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

public class DataFileDisplayTest extends TestCase {
	private WebDriver driver;
	private String baseUrl="http://datadryad.org";
	private StringBuffer verificationErrors = new StringBuffer();
	@Before
	public void setUp() throws Exception {
		driver = new HtmlUnitDriver();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void testDataFileDisplay() throws Exception {
		driver.get(baseUrl + "/handle/10255/dryad.58");
		assertEquals("Dryad data file: Morphospace Specimens", driver.getTitle());
		assertTrue(isElementPresent(By.cssSelector("h1.ds-div-head")));
		assertTrue("head contains Morphospace", sectionContains("h1.ds-div-head", "Morphospace"));
		assertTrue("citation contains package info", sectionContains("div.citation-view", "(2007) Testing"));
		assertTrue("citation contains file info", sectionContains("div.citation-view", "(2007) Testing"));
		assertTrue("bitstream present", isElementPresent(By.linkText("Evo_22_Table S1.doc")));
		assertTrue("bitstream format present", sectionContains("div.primary","Microsoft Word"));
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
