/*
 * OpenAirePiwikTrackerUnreportedDB.java
 *
 * Version: 0.1
 * Date: 2018-05-20
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
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
package com.openaire.piwik.tracker;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.dspace.core.ConfigurationManager;

/**
 * User: dpie (dpierrakos at gmail.com) Date: Time:
**/
public class OpenAirePiwikTrackerUnreportedDB
{

    public static String getMatomoDatabaseUrl()
    {
        String dspaceDir = ConfigurationManager.getProperty("dspace.dir");

        String matomoDbLocation = ConfigurationManager.getProperty("oapiwik","piwik.matomoDbLocation");

        String connectionStringMatomoDB = "jdbc:sqlite:" + dspaceDir + "/"
                + matomoDbLocation;

        return connectionStringMatomoDB;
    }

    public static void main(String[] args) throws Exception
    {

        String argument = args[0];
        if (!("-create".equals(argument)) && !("-retry".equals(argument))
                && !("-delete".equals(argument)))
        {
            System.out.println("Usage: dspace.dir/bin/dspace -create or -retry or -delete");
            return;
        }

        try (Connection conn = DriverManager
                .getConnection(getMatomoDatabaseUrl());
                Statement stmt = conn.createStatement();)
        {

            System.out.println("Connected to Matomo DB...");
            if (argument.equals("-create"))
            {

                String sqlCreate = "CREATE TABLE IF NOT EXISTS MissingInformation (time_req TEXT PRIMARY KEY, url TEXT NOT NULL, isSend INTEGER NOT NULL);";
                stmt.executeUpdate(sqlCreate);
            }
            else if (argument.equals("-retry"))
            {

                try (ResultSet rs = stmt.executeQuery(
                        "SELECT time_req, url FROM MissingInformation where isSend=0;");)
                {
                    String urlUnreported = "";
                    String time_req = "";

                    HashMap<String, String> timestampURLMap = new HashMap<String, String>();

                    if (!rs.isBeforeFirst())
                        System.out.println("No pending missing requests");

                    else
                    {
                        while (rs.next())
                        {
                            time_req = rs.getString("time_req");
                            urlUnreported = rs.getString("url");
                            timestampURLMap.put(time_req, urlUnreported);
                        }
                    }

                    for (Map.Entry<String, String> entry : timestampURLMap
                            .entrySet())
                    {
                        String timeStamp = entry.getKey();
                        String urlForReport = entry.getValue();

                        URL obj = new URL(urlForReport + "&cdt="
                                + URLEncoder.encode(timeStamp,
                                        StandardCharsets.UTF_8.name()));
                        HttpURLConnection con = (HttpURLConnection) obj
                                .openConnection();
                        int responseCode = con.getResponseCode();
                        if (responseCode == 200)
                        {
                            String sqlUpdate = "";

                            sqlUpdate = "UPDATE MissingInformation set isSend=1 where time_req='"
                                    + timeStamp + "';";
                            stmt.executeUpdate(sqlUpdate);
                            System.out.println("Unreported Event sent");
                        }
                    }
                }
                catch (Exception e)
                {
                    System.out.println(e);
                }
            }
            else if (argument.equals("-delete"))
            {

                String sqlPurge = "DELETE FROM MissingInformation where isSend=1;";
                int deletedRowCount = stmt.executeUpdate(sqlPurge);
                System.out.println(deletedRowCount
                       +  " missing requests have already been sent to Matomo and deleted from local DB");

            }
        }
        catch (Exception e)
        {
            System.out.println("Error in creating table " + e.getMessage());
        }
    }
}