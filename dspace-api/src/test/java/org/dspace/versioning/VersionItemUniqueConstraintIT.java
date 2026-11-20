/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Integration tests for the unique (versionhistory_id, version_number) constraint
 * on {@code versionitem}, and for the H2 renumber cleanup used before that constraint
 * is applied (see Flyway migration
 * {@code V11.0_2026.10.28__versionitem_unique_version_number_per_history.sql}).
 */
public class VersionItemUniqueConstraintIT extends AbstractIntegrationTestWithDatabase {

    private static final String CONSTRAINT_NAME = "versionitem_versionhistory_version_number_unique";

    @Test
    public void duplicateVersionNumberInsertIsRejected() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Unique constraint item")
                               .build();
        Version version = VersionBuilder.createVersion(context, item, "v2").build();
        context.restoreAuthSystemState();
        context.commit();

        int historyId = version.getVersionHistory().getID();

        try {
            insertVersionItemRow(historyId, 1, "duplicate-number");
            fail("Expected SQLException when inserting duplicate version_number for the same history");
        } catch (SQLException e) {
            // H2 reports unique constraint violations with this name / SQLState 23505
            assertTrue("Unexpected SQLException: " + e.getMessage(),
                       e.getMessage().contains(CONSTRAINT_NAME)
                           || "23505".equals(e.getSQLState())
                           || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")));
        }
    }

    @Test
    public void renumberSqlFixesDuplicateVersionNumbers() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Renumber cleanup item")
                               .build();
        Version version = VersionBuilder.createVersion(context, item, "v2").build();
        context.restoreAuthSystemState();
        context.commit();

        int historyId = version.getVersionHistory().getID();

        DataSource dataSource = DSpaceServicesFactory.getInstance()
                                                     .getServiceManager()
                                                     .getServiceByName("dataSource", DataSource.class);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                // Simulate a pre-migration DB that already has duplicates.
                statement.execute("ALTER TABLE versionitem DROP CONSTRAINT " + CONSTRAINT_NAME);
                statement.executeUpdate(
                    "UPDATE versionitem SET version_number = 1 WHERE versionhistory_id = " + historyId);

                List<Integer> before = loadVersionNumbers(connection, historyId);
                assertEquals(2, before.size());
                assertEquals(Integer.valueOf(1), before.get(0));
                assertEquals(Integer.valueOf(1), before.get(1));

                // Same cleanup steps as the H2 Flyway migration (before re-adding the constraint).
                runH2RenumberSql(statement);

                List<Integer> after = loadVersionNumbers(connection, historyId);
                assertEquals(2, after.size());
                assertEquals(Integer.valueOf(1), after.get(0));
                assertEquals(Integer.valueOf(2), after.get(1));
                assertEquals(after.size(), new HashSet<>(after).size());

                statement.execute(
                    "ALTER TABLE versionitem ADD CONSTRAINT " + CONSTRAINT_NAME
                        + " UNIQUE (versionhistory_id, version_number)");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                // Always try to restore the constraint so later tests are not affected.
                try (Statement restore = connection.createStatement()) {
                    restore.execute(
                        "ALTER TABLE versionitem ADD CONSTRAINT " + CONSTRAINT_NAME
                            + " UNIQUE (versionhistory_id, version_number)");
                    connection.commit();
                } catch (SQLException ignored) {
                    // Best-effort restore (constraint may already exist)
                }
                throw e;
            }
        }

        // Constraint must reject duplicates again after restore.
        try {
            insertVersionItemRow(historyId, 1, "duplicate-after-renumber");
            fail("Expected SQLException after constraint was restored");
        } catch (SQLException e) {
            assertTrue("Unexpected SQLException: " + e.getMessage(),
                       e.getMessage().contains(CONSTRAINT_NAME)
                           || "23505".equals(e.getSQLState())
                           || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")));
        }
    }

    /**
     * Mirrors the renumber steps in
     * {@code h2/V11.0_2026.10.28__versionitem_unique_version_number_per_history.sql}.
     */
    private void runH2RenumberSql(Statement statement) throws SQLException {
        statement.execute("""
            CREATE TABLE versionitem_renumber_tmp AS
            SELECT versionitem_id,
                   ROW_NUMBER() OVER (
                       PARTITION BY versionhistory_id
                       ORDER BY version_date ASC NULLS LAST, versionitem_id ASC
                   ) AS new_version_number
            FROM versionitem
            WHERE versionhistory_id IN (
                SELECT versionhistory_id
                FROM versionitem
                GROUP BY versionhistory_id, version_number
                HAVING COUNT(*) > 1
            )
            """);
        statement.executeUpdate("""
            UPDATE versionitem
            SET version_number = (
                SELECT new_version_number
                FROM versionitem_renumber_tmp
                WHERE versionitem_renumber_tmp.versionitem_id = versionitem.versionitem_id
            )
            WHERE versionitem_id IN (SELECT versionitem_id FROM versionitem_renumber_tmp)
            """);
        statement.execute("DROP TABLE versionitem_renumber_tmp");
    }

    private void insertVersionItemRow(int historyId, int versionNumber, String summary) throws SQLException {
        DataSource dataSource = DSpaceServicesFactory.getInstance()
                                                     .getServiceManager()
                                                     .getServiceByName("dataSource", DataSource.class);
        try (Connection connection = dataSource.getConnection();
             Statement idStatement = connection.createStatement();
             ResultSet rs = idStatement.executeQuery(
                 "SELECT COALESCE(MAX(versionitem_id), 0) + 1 FROM versionitem");
             PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO versionitem (versionitem_id, item_id, version_number, version_summary, versionhistory_id)"
                     + " VALUES (?, NULL, ?, ?, ?)")) {
            assertTrue(rs.next());
            int nextId = rs.getInt(1);
            insert.setInt(1, nextId);
            insert.setInt(2, versionNumber);
            insert.setString(3, summary);
            insert.setInt(4, historyId);
            insert.executeUpdate();
        }
    }

    private List<Integer> loadVersionNumbers(Connection connection, int historyId) throws SQLException {
        List<Integer> numbers = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT version_number FROM versionitem WHERE versionhistory_id = ? ORDER BY versionitem_id ASC")) {
            ps.setInt(1, historyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    numbers.add(rs.getInt(1));
                }
            }
        }
        return numbers;
    }
}
