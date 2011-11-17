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
	public void testSearchDog() {
	    Assert.assertTrue("This is supposed to be true.", true);
	    
		driver.get("http://datadryad.org");
		driver.findElement(By.id("ds-header-logo")).click();
		driver.findElement(By.name("query")).clear();
		driver.findElement(By.name("query")).sendKeys("dog*");
		driver.findElement(By.name("submit")).click();
		driver.findElement(By.cssSelector("span.artifact-title")).click();
		driver.findElement(By.linkText("Show Full Metadata")).click();
		Assert.assertEquals("Data from: Exploring the mechanisms underlying a heterozygosity-fitness correlation for canine size in the Antarctic fur seal Arctocephalus gazella", driver.getTitle());
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

    	public static void main(String... args) {
		new SearchingTest().testSearchDog();
	}

}
