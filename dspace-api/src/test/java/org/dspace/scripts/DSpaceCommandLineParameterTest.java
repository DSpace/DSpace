/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.junit.Test;

public class DSpaceCommandLineParameterTest extends AbstractUnitTest {

    @Test
    public void constructorTest() {
        String key = "-c";
        String value = "test";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);

        assertThat("constructorTest 0", dSpaceCommandLineParameter.getName(), equalTo(key));
        assertThat("constructorTest 1", dSpaceCommandLineParameter.getValue(), equalTo(value));
    }

    @Test
    public void constructorTestNullValue() {
        String key = "-c";
        String value = null;

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);

        assertThat("constructorTest 0", dSpaceCommandLineParameter.getName(), equalTo(key));
        assertThat("constructorTest 1", dSpaceCommandLineParameter.getValue(), equalTo(value));
    }

    @Test
    public void singleParameterConstructorTest() {
        String parameter = "-c test";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(parameter);

        assertThat("singleParameterConstructorTest 0", dSpaceCommandLineParameter.getName(), equalTo("-c"));
        assertThat("singleParameterConstructorTest 1", dSpaceCommandLineParameter.getValue(), equalTo("test"));
    }

    @Test
    public void singleParameterConstructorTestNoValue() {
        String parameter = "-c";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(parameter);

        assertThat("singleParameterConstructorTest 0", dSpaceCommandLineParameter.getName(), equalTo("-c"));
        assertThat("singleParameterConstructorTest 1", dSpaceCommandLineParameter.getValue(), equalTo(null));
    }

    @Test
    public void toStringTest() {
        String key = "-c";
        String value = "test";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);

        assertThat("toStringTest 0", dSpaceCommandLineParameter.getName(), equalTo(key));
        assertThat("toStringTest 1", dSpaceCommandLineParameter.getValue(), equalTo(value));

        assertThat("toStringTest 2", dSpaceCommandLineParameter.toString(), equalTo("-c test"));
    }

    @Test
    public void toStringTestNullValue() {
        String key = "-c";
        String value = null;

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);

        assertThat("toStringTest 0", dSpaceCommandLineParameter.getName(), equalTo(key));
        assertThat("toStringTest 1", dSpaceCommandLineParameter.getValue(), equalTo(value));

        assertThat("toStringTest 2", dSpaceCommandLineParameter.toString(), equalTo("-c"));
    }

    @Test
    public void equalsTest() {
        String key = "-c";
        String value = "test";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);
        DSpaceCommandLineParameter dSpaceCommandLineParameter1 = new DSpaceCommandLineParameter(key, value);

        assertThat("toStringTest 0", dSpaceCommandLineParameter.getName(), equalTo(key));
        assertThat("toStringTest 1", dSpaceCommandLineParameter.getValue(), equalTo(value));


        assertThat("toStringTest 0", dSpaceCommandLineParameter1.getName(), equalTo(key));
        assertThat("toStringTest 1", dSpaceCommandLineParameter1.getValue(), equalTo(value));

        assertTrue(dSpaceCommandLineParameter.equals(dSpaceCommandLineParameter1));
    }

    @Test
    public void concatenateTest() {
        String key = "-c";
        String value = "test";

        DSpaceCommandLineParameter dSpaceCommandLineParameter = new DSpaceCommandLineParameter(key, value);
        DSpaceCommandLineParameter dSpaceCommandLineParameter1 = new DSpaceCommandLineParameter(key, value);

        String key2 = "-r";
        String value2 = "testing";
        DSpaceCommandLineParameter dSpaceCommandLineParameter2 = new DSpaceCommandLineParameter(key2, value2);

        String key3 = "-t";
        String value3 = null;
        DSpaceCommandLineParameter dSpaceCommandLineParameter3 = new DSpaceCommandLineParameter(key3, value3);

        List<DSpaceCommandLineParameter> dSpaceCommandLineParameterList = new ArrayList<>();
        dSpaceCommandLineParameterList.add(dSpaceCommandLineParameter);
        dSpaceCommandLineParameterList.add(dSpaceCommandLineParameter1);
        dSpaceCommandLineParameterList.add(dSpaceCommandLineParameter2);
        dSpaceCommandLineParameterList.add(dSpaceCommandLineParameter3);
        String concatenedString = DSpaceCommandLineParameter.concatenate(dSpaceCommandLineParameterList);

        assertThat("concatenateTest", concatenedString, equalTo(
            dSpaceCommandLineParameter.toString() + DSpaceCommandLineParameter.SEPARATOR + dSpaceCommandLineParameter1
                .toString() + DSpaceCommandLineParameter.SEPARATOR + dSpaceCommandLineParameter2
                .toString() + DSpaceCommandLineParameter.SEPARATOR + dSpaceCommandLineParameter3.toString()));
    }
}
