/*
 */
package org.datadryad.test;

import java.sql.SQLException;
import org.dspace.core.Context;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class ContextUnitTest {
    protected Context context;

    @Before
    public void setUp() {
        try {
            this.context = new Context();
            context.turnOffAuthorisationSystem();
        } catch (SQLException ex) {
            fail("Unable to instantiate context " + ex);
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

}
