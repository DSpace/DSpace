/*
 * MetadataIntegrationTest.java
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content;

import java.io.IOException;
import org.databene.contiperf.Required;
import org.databene.contiperf.PerfTest;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This is an integration test to validate the metadata classes
 * @author pvillega
 */
public class MetadataIntegrationTest  extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataIntegrationTest.class);


    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Tests the creation of a new metadata schema with some values
     */
    @Test
    @PerfTest(invocations = 50, threads = 1)
    @Required(percentile95 = 500, average= 200)
    public void testCreateSchema() throws SQLException, AuthorizeException, NonUniqueMetadataException, IOException
    {
        String schemaName = "integration";

        //we create the structure
        context.turnOffAuthorisationSystem();
        Item it = Item.create(context);

        MetadataSchema schema = new MetadataSchema("htpp://test/schema/", schemaName);
        schema.create(context);

        MetadataField field1 = new MetadataField(schema, "elem1", "qual1", "note 1");
        field1.create(context);

        MetadataField field2 = new MetadataField(schema, "elem2", "qual2", "note 2");
        field2.create(context);

        MetadataValue value1 = new MetadataValue(field1);
        value1.setItemId(it.getID());
        value1.setValue("value1");
        value1.create(context);

        MetadataValue value2 = new MetadataValue(field2);
        value2.setItemId(it.getID());
        value2.setValue("value2");
        value2.create(context);
        
        context.commit();

        //verify it works as expected
        assertThat("testCreateSchema 0", schema.getName(), equalTo(schemaName));
        assertThat("testCreateSchema 1", field1.getSchemaID(), equalTo(schema.getSchemaID()));
        assertThat("testCreateSchema 2", field2.getSchemaID(), equalTo(schema.getSchemaID()));

        MetadataField[] fields = MetadataField.findAllInSchema(context, schema.getSchemaID());
        assertTrue("testCreateSchema 3", fields.length == 2);
        boolean exist = true;
        for(MetadataField f : fields)
        {
           if(!f.equals(field1) && !f.equals(field2))
           {
               exist = false;
           }
        }
        assertTrue("testCreateSchema 4", exist);

        java.util.Collection col1 = MetadataValue.findByField(context, field1.getFieldID());
        assertTrue("testCreateSchema 5", col1.contains(value1));

        java.util.Collection col2 = MetadataValue.findByField(context, field2.getFieldID());
        assertTrue("testCreateSchema 6", col2.contains(value2));

        //clean database
        value1.delete(context);
        col1 = MetadataValue.findByField(context, field1.getFieldID());
        assertFalse("testCreateSchema 7", col1.contains(value1));

        value2.delete(context);
        field1.delete(context);
        field2.delete(context);
        schema.delete(context);

        context.restoreAuthSystemState();
        context.commit();
    }

}

