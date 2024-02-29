/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gadgets for debugging database activity.
 *
 * @author mwood
 */
public class DatabaseDebug {
    private DatabaseDebug() { }

    /**
     * Dump the entire content of a database table.  Long values are truncated
     * to avoid seeing 2GB underlines and the like.
     *
     * @param cnx
     * @param name the name of the table to be dumped.
     * @throws SQLException if the DBMS returns badness.
     */
    public static void dumpATable(Connection cnx, String name)
            throws SQLException {
        Statement stmt = cnx.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + name);
        ResultSetMetaData rsm = rs.getMetaData();
        int columns = rsm.getColumnCount();
        int[] widths = new int[columns];
        // Print column names
        for (int column = 0; column < columns; column++) {
            String colName = rsm.getColumnName(column + 1);
            if (column > 0) {
                System.out.print("  ");
            }
            System.out.print(colName);
            int width = rsm.getPrecision(column + 1);
            widths[column] = width > 32 ? 32 : width;
        }
        System.out.println();
        // Underline names
        for (int column = 0; column < columns; column++) {
            if (column > 0) {
                System.out.print("  ");
            }
            for (int pos = 0; pos < widths[column]; pos++) {
                System.out.print('-');
            }
        }
        System.out.println();
        // Print column data for each row
        while (rs.next()) {
            for (int column = 0; column < columns; column++) {
                if (column > 0) {
                    System.out.print("  ");
                }
                Object cell = rs.getObject(column + 1);
                System.out.format("%" + widths[column] + "s",
                        cell == null ? "null" : cell.toString());
            }
            System.out.println();
        }
    }
}
