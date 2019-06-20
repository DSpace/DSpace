/*
 * OpenAirePiwikTrackerUnreported.java
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * User: dpie (dpierrakos at gmail.com) Date: Time:
 */
public class OpenAirePiwikTrackerUnreported
{

    private static Logger log = Logger
            .getLogger(OpenAirePiwikTrackerUnreported.class);

    public OpenAirePiwikTrackerUnreported()
    {
        try
        {
            // The JDBC driver loading doesn't work for webapps without this.
            // http://tomcat.apache.org/tomcat-8.0-doc/jndi-datasource-examples-howto.html#DriverManager,_the_service_provider_mechanism_and_memory_leaks
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            log.error("Could not load sqlite JDBC driver");
        }
    }

    public void storeRequest(String requestLost) throws Exception
    {

        try (Connection conn = DriverManager.getConnection(
                OpenAirePiwikTrackerUnreportedDB.getMatomoDatabaseUrl());
                Statement stmt = conn.createStatement();)
        {

            log.info("Connected to Matomo DB...");

            Timestamp currentTimestamp = new java.sql.Timestamp(
                    Calendar.getInstance().getTime().getTime());
            String sqlInsertMissingRequestsToDB = "INSERT INTO MissingInformation (time_req,url,isSend) "
                    + "VALUES ('" + currentTimestamp + "','" + requestLost
                    + "',0);";

            stmt.execute(sqlInsertMissingRequestsToDB);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }
    }
}