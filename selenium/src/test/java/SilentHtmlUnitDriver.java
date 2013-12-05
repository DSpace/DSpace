package test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
   A class that wraps the HtmlUnitDriver, but ignores CSS warnings during testing.
 **/

public class SilentHtmlUnitDriver extends HtmlUnitDriver {
    SilentHtmlUnitDriver() {
	super();
	this.getWebClient().setCssErrorHandler(new SilentCssErrorHandler());
    }
}
