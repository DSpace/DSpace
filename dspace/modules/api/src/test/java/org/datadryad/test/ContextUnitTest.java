/*
 */
package org.datadryad.test;

import java.sql.SQLException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class ContextUnitTest {
    protected Context context;
    private static DSpaceKernelImpl kernel;

    @Before
    public void setUp() {
        try {
            this.context = new Context();
            context.turnOffAuthorisationSystem();
        } catch (SQLException ex) {
            fail("Unable to instantiate context " + ex);
        }
    }

    @BeforeClass
    public static void setupClass() {
        kernel = DSpaceKernelInit.getKernel(null);
        if(!kernel.isRunning()) {
            kernel.start(ConfigurationManager.getProperty("dspace.dir"));
        }
    }

    @After
    public void tearDown() {
        try {
            this.context.complete();
        } catch (SQLException ex) {
            fail("Unable to tear down context " + ex);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        kernel.destroy();
    }
}
