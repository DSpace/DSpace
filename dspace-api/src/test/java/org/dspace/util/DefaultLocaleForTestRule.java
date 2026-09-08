/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.util.Locale;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class DefaultLocaleForTestRule extends TestWatcher {

    private Locale originalDefault;
    private Locale testDefault;

    public DefaultLocaleForTestRule() {
        this(null);
    }

    public DefaultLocaleForTestRule(Locale testDefault) {
        this.testDefault = testDefault;
    }

    @Override
    protected void starting(Description description) {
        originalDefault = Locale.getDefault();

        if (null != testDefault) {
            Locale.setDefault(testDefault);
        }
    }

    @Override
    protected void succeeded(Description description) {
        Locale.setDefault(originalDefault);
    }

    public void setDefault(Locale locale) {
        if (null == locale) {
            locale = originalDefault;
        }

        Locale.setDefault(locale);
    }

    public static DefaultLocaleForTestRule en() {
        return new DefaultLocaleForTestRule(Locale.ENGLISH);
    }
}
