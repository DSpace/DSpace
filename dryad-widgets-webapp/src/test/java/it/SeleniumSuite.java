
package it;

import junit.framework.Test;
import junit.framework.TestSuite;

import it.BtnCiteLightbox;
import it.BtnDownload;
import it.BtnShareLightbox;
import it.BtnZoomLightbox;
import it.ButtonsPresent;
import it.SeleniumSuite;
import it.WidgetLoad;

public class SeleniumSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(WidgetLoad.class);
/*
    suite.addTestSuite(ButtonsPresent.class);
    suite.addTestSuite(BtnZoomLightbox.class);
    suite.addTestSuite(BtnShareLightbox.class);
    suite.addTestSuite(BtnDownload.class);
    suite.addTestSuite(BtnCiteLightbox.class);
*/
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
