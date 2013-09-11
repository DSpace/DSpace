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

public class SearchingTest extends TestCase {
	private WebDriver driver;
	private StringBuffer verificationErrors = new StringBuffer();

        @Before
	public void setUp() throws Exception {
	    driver = new HtmlUnitDriver();
	    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}

	@Test
	public void testSearch() {	    
		driver.get("http://datadryad.org");
		driver.findElement(By.name("query")).clear();
		driver.findElement(By.name("query")).sendKeys("dog* Barua");
		driver.findElement(By.name("submit")).click();
		Assert.assertTrue("find ecomorph package", sectionContains("div.primary","Arabidopsis thaliana"));
	}


	@Test
	public void testSearchRedirect() {	    
		driver.get("http://datadryad.org");
		driver.findElement(By.name("query")).clear();
		driver.findElement(By.name("query")).sendKeys("dog ecomorph");
		driver.findElement(By.name("submit")).click();
		Assert.assertTrue("basic search: dog* Barua", driver.getTitle().startsWith("Data from: Do convergent ecomorphs"));
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

	private boolean sectionContains(String section, String targetText) {
	    return driver.findElement(By.cssSelector(section)).getText().contains(targetText);
	}


    	public static void main(String... args) {
		new SearchingTest().testSearch();
		new SearchingTest().testSearchRedirect();
	}

}
