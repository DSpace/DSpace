package test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openqa.selenium.*;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.openqa.selenium.logging.*;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.Select;

public class DescribePublicationAJAXTest {
    private WebDriver driver;
    private String baseUrl;
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    // seconds to wait for an AJAX response and refresh; ms/s; Selenium max timeout
    private final int ajaxWaitSeconds = 10;
    private final int waitSleepInterval = 1000;
    private final int maxWaitSeconds = 10;

    @Before
    public void setUp() throws Exception {
        if ("Chrome".equals(System.getProperty("selenium_test_browser"))) {
            driver = new ChromeDriver();
        } else if ("Safari".equals(System.getProperty("selenium_test_browser"))) {
            driver = new SafariDriver();
        } else {
            LoggingPreferences logs = new LoggingPreferences();
            logs.enable(LogType.DRIVER, Level.SEVERE);
            logs.enable(LogType.BROWSER, Level.INFO);
            DesiredCapabilities capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);
            driver = new FirefoxDriver(capabilities);
        }
        baseUrl = System.getProperty("selenium_test_url");
        driver.manage().timeouts().implicitlyWait(maxWaitSeconds, TimeUnit.SECONDS);
    }

    // NOTE: this key sequence is for OSX
    private void openConsole() {
        if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
            driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.SHIFT, "j"));
        } else if (driver instanceof org.openqa.selenium.chrome.ChromeDriver) {
            driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.ALT, "j"));
        }
    }

    // useful constant xpaths: update button, save/exit button, save-for-later, remove-submission
    private final String update_btn_xpath      = "//input[@class='ds-button-field ds-update-button']";
    private final String save_exit_btn_xpath   = "//input[@id='aspect_submission_StepTransformer_field_submit_cancel']";
    private final String save_later_btn_xpath  = "//input[@id='aspect_submission_submit_SaveOrRemoveStep_field_submit_save']";
    private final String remove_subs_btn_xpath = "//input[@id='aspect_discovery_DiscoverySubmissions_field_submit_submissions_remove']";

    @Test
    public void testDescribePublicationAJAX() throws Exception {
        driver.get(baseUrl + "/");
        openConsole();
        login();
        initSubmitData();

        // ADD TITLE
        String formTitle = this.getClass().getName() + ": " + Long.toString(System.currentTimeMillis());
        String unfinishedTitle = "Data from: " + formTitle;
        addTitle(formTitle);

        // ADD AUTHORS
        ArrayList<Author> authors = new ArrayList<Author>(3);
        authors.add(new Author("Buck", "Jones", 1));
        authors.add(new Author("Zoe", "Barnes", 2));
        authors.add(new Author("Brian", "Badluck", 3));
        for (Author author : authors) {
            addAuthor(author);
        }
        verifyAuthorNames(authors);

        // EDIT AUTHOR NAME
        authors.get(0).first = "Buck G.";
        authors.get(0).setInterp();
        updateAuthorNames(authors);
        verifyAuthorNames(authors);

        // REORDER AUTHORS
        Author temp = authors.remove(0);
        authors.add(temp);
        for (int i = 0; i < authors.size(); i++) {
            authors.get(i).index = i+1;
        }
        updateAuthorOrder(authors);
        verifyAuthorOrder(authors);

        // REMOVE AUTHOR
        removeAuthor(authors,2);
        verifyAuthorOrder(authors);
        verifyAuthorNames(authors);

        // Save publication description and exit to user page
        saveAndExit();

        // remove unfinished submission entry
        // do not do cleanup if the submission is not on the first page
        try {
            cleanupSubmission(unfinishedTitle);
        } catch(Exception e){}

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

    // utility wrapper for generating page xpaths on a per-author basis
    private class AuthorXpath {
        private Author author;
        public AuthorXpath(Author author) {
            this.author = author;
        }
        private String authpred(String by) throws Exception {
          if (by.equals("i")) return "["    + author.index  + "]";
          if (by.equals("a")) return "[.='" + author.interp + "']";
          throw new Exception();
        }
        public String row(String by) throws Exception {
          String pred = by.equals("i")
                      ? "[" + author.index + "]"
                      : "[" + ".//span[@class='ds-interpreted-field']" + authpred(by) + "]";    // has a span descendant where name = ...
          return "//tr[@class='ds-author-input-row']" + pred;
        }
        public String span_interp(String by) throws Exception {
          return "(//span[@class='ds-interpreted-field'])" + authpred(by);
        }
        public String input_name_last(String by) throws Exception {
            return row(by) + "//input[starts-with(@name,'dc_contributor_author_last_')]";
        }
        public String input_name_first(String by) throws Exception {
            return row(by) + "//input[starts-with(@name,'dc_contributor_author_first_')]";
        }
        public String edit_btn(String by) throws Exception {
            return row(by) + "//input[@class='ds-button-field ds-edit-button']";
        }
        public String order_select(String by) throws Exception {
            return row(by) + "//select[@class='ds-author-order-select']";
        }
        public String remove_btn(String by) throws Exception {
            return row(by) + "//input[@name='submit_dc_contributor_author_delete']";
        }
    }

    // data model for an author on a submission page, which
    // has names and an index
    private class Author {
        public Author(String first, String last, int index) {
            this.first = first;
            this.last = last;
            this.index = index;
            setInterp();
            xpath = new AuthorXpath(this);
        }
        public void setInterp() {
          this.interp = last + ", " + first;
        }
        public int index;     // 1-based
        public String first;
        public String last;
        public String interp;
        public AuthorXpath xpath;
    }

    private void login() {
        driver.get(baseUrl + "/");
        driver.findElement(By.id("sign-up-item")).click();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_email")).sendKeys("seleniumtest@datadryad.org");
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).clear();
        driver.findElement(By.id("aspect_eperson_PasswordLogin_field_login_password")).sendKeys("seleniumtest");
        driver.findElement(By.cssSelector("#aspect_eperson_PasswordLogin_item_loginsubmit-item > div.ds-form-content > #aspect_eperson_PasswordLogin_field_submit")).click();
    }

    private void initSubmitData() {
        driver.findElement(By.linkText("Submit data now")).click();
        new Select(driver.findElement(By.name("country"))).selectByVisibleText("Afghanistan");
        driver.findElement(By.id("xmlui_submit_publication_article_status_in_review")).click();
        driver.findElement(By.xpath("//li[@id='aspect_submission_StepTransformer_item_article_status']/div/div/label[3]")).click();
        new Select(driver.findElement(By.id("aspect_submission_StepTransformer_field_journalIDStatusInReview"))).selectByVisibleText("Dryad Testing Journal");
        driver.findElement(By.name("license_accept")).click();
        driver.findElement(By.id("aspect_submission_StepTransformer_field_submit_next")).click();
    }

    private void waitOnXpathsPresent(ArrayList<String> xpaths) throws InterruptedException {
      // wait for form submission to complete
      for (int second = 0;; second++) {
        if (second >= ajaxWaitSeconds) fail("timeout");
        try {
            boolean done = true;
            for (String xpath : xpaths) {
                done = done && isElementPresent(By.xpath(xpath));
            }
            if (done) break;
        } catch (Exception e) {}
            Thread.sleep(waitSleepInterval);
        }
    }

    private void addAuthor(Author author) throws Exception {
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).clear();
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_last")).sendKeys(author.last);
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).clear();
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_contributor_author_first")).sendKeys(author.first);
        // submit for update then wait for update on page
        driver.findElement(By.name("submit_dc_contributor_author_add")).click();
        ArrayList<String> paths = new ArrayList<String>();
        paths.add(author.xpath.span_interp("a"));
        paths.add(author.xpath.span_interp("i"));
        waitOnXpathsPresent(paths);
    }

    private void verifyAuthorNames(ArrayList<Author> authors) throws Exception {
        for (Author author : authors) {
          assertEquals(author.interp, driver.findElement(By.xpath(author.xpath.span_interp("i"))     ).getText());
          assertEquals(author.last,   driver.findElement(By.xpath(author.xpath.input_name_last("i")) ).getAttribute("value"));
          assertEquals(author.first,  driver.findElement(By.xpath(author.xpath.input_name_first("i"))).getAttribute("value"));
        }
    }

    private void addTitle(String title) {
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).clear();
        driver.findElement(By.id("aspect_submission_StepTransformer_field_dc_title")).sendKeys(title);
    }

    private void updateAuthorName(Author author) throws Exception {
        driver.findElement(By.xpath(author.xpath.edit_btn("i"))).click();
        driver.findElement(By.xpath(author.xpath.input_name_last("i"))).clear();
        driver.findElement(By.xpath(author.xpath.input_name_last("i"))).sendKeys(author.last);
        driver.findElement(By.xpath(author.xpath.input_name_first("i"))).clear();
        driver.findElement(By.xpath(author.xpath.input_name_first("i"))).sendKeys(author.first);

        // update and wait for form submission to complete
        driver.findElement(By.xpath(update_btn_xpath)).click();
        for (int second = 0;; second++) {
            if (second >= ajaxWaitSeconds) fail("timeout");
            try {
                if (author.interp.equals(driver.findElement(By.xpath(author.xpath.span_interp("i"))).getText())) { break;}
            } catch (Exception e) {}
            Thread.sleep(waitSleepInterval);
        }
    }

    private void updateAuthorNames(ArrayList<Author> authors) throws Exception {
        for (Author author : authors) {
            if(!author.interp.equals(driver.findElement(By.xpath(author.xpath.span_interp("i"))).getText())) {
                updateAuthorName(author);
            }
        }
    }

    private void saveAndExit() throws Exception {
        driver.findElement(By.xpath(save_exit_btn_xpath)).click();
        ArrayList<String> paths = new ArrayList<String>();
        paths.add(save_later_btn_xpath);
        waitOnXpathsPresent(paths);
        driver.findElement(By.xpath(save_later_btn_xpath)).click();
    }

    private void cleanupSubmission(String unfinishedTitle) throws Exception {
        String checkbox_xpath = "//tr[.//a[.='" + unfinishedTitle  + "']]//input[@type='checkbox']";
        driver.findElement(By.xpath(checkbox_xpath)).click();
        // click remove button
        driver.findElement(By.xpath(remove_subs_btn_xpath)).click();
    }

    private void updateAuthorOrder(ArrayList<Author> authors) throws Exception {
        ArrayList<String> paths = new ArrayList<String>();
        for (Author author: authors) {
            // update the selected select/option value to match the data
            // checks current index of data against found text at that index
            if (!driver.findElement(By.xpath(author.xpath.span_interp("i"))).getText().equals(author.interp))
            {
                driver.findElement(By.xpath(author.xpath.edit_btn("a"))).click();
                // off-by-one for author.interp index and select/option index
                new Select(driver.findElement(By.xpath(author.xpath.order_select("a")))).selectByIndex(author.index - 1);
            }
            paths.add(author.xpath.span_interp("a"));
        }
        driver.findElement(By.xpath(update_btn_xpath)).click();
        waitOnXpathsPresent(paths);
    }

    private void verifyAuthorOrder(ArrayList<Author> authors) throws Exception {
        for (Author author: authors) {
            assertTrue(!author.xpath.span_interp("i").equals(author.interp));
        }
    }

    private void removeAuthor(ArrayList<Author> authors, int ind) throws Exception {
        // remove given author, then re-set remaining authors' indices
        Author author = authors.remove(ind);
        for (int i = 0; i < authors.size(); i++) {
            authors.get(i).index = i+1;
        }
        ArrayList<String> paths = new ArrayList<String>(authors.size());
        paths.add(author.xpath.remove_btn("a"));
        waitOnXpathsPresent(paths);
        paths.clear();
        // TODO: figure out why this event won't fire with a single click
        // just attempt several clicks
        while (isElementPresent(By.xpath(author.xpath.remove_btn("a")))) {
            try {
                driver.findElement(By.xpath(author.xpath.remove_btn("a"))).click();
            } catch (Exception e) {}
        }
        // action is done once form is updated, so wait for the non-removed
        // authors to be present again after submission
        for (Author a : authors) {
            paths.add(a.xpath.row("a"));
        }
        waitOnXpathsPresent(paths);
        // confirm that requested author was removed
        assertFalse(isElementPresent(By.xpath(author.xpath.span_interp("a"))));
    }
}
