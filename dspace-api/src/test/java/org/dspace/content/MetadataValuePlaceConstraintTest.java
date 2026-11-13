/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.dspace.AbstractUnitTest;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.junit.Test;

/**
 * Regression test for DSpace issue #8517.
 * Verifies that metadata positions cannot be NULL.
 */
public class MetadataValuePlaceConstraintTest extends AbstractUnitTest {

    @Test
    public void testPlaceColumnIsNotNullable() throws Exception {

        DataSource dataSource = DSpaceServicesFactory.getInstance()
            .getServiceManager()
            .getServiceByName("dataSource", DataSource.class);

        assertNotNull("DSpace DataSource must exist", dataSource);

        try (Connection connection = dataSource.getConnection()) {

            DatabaseMetaData metaData = connection.getMetaData();

            String tableName = DatabaseUtils.canonicalize(
                connection, "metadatavalue"
            );

            String columnName = DatabaseUtils.canonicalize(
                connection, "place"
            );

            try (ResultSet columns = metaData.getColumns(
                    connection.getCatalog(),
                    connection.getSchema(),
                    tableName,
                    columnName)) {

                assertTrue(
                    "metadatavalue.place column must exist",
                    columns.next()
                );

                assertEquals(
                    "metadatavalue.place must have NOT NULL constraint",
                    DatabaseMetaData.columnNoNulls,
                    columns.getInt("NULLABLE")
                );
            }
        }
    }
}
