/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package cz.cuni.mff.ufal.health;


import cz.cuni.mff.ufal.checks.ShibUserLogins;
import cz.cuni.mff.ufal.dspace.IOUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

public class ShibbolethCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        StringBuilder ret = new StringBuilder();
        final String input_dir = ConfigurationManager.getProperty("lr",
            "lr.shibboleth.log.path");
        final String default_log = ConfigurationManager.getProperty("lr",
            "lr.shibboleth.log.defaultName");

        String input_file = new File(input_dir, default_log).toString();

        ret.append(String.format("Parsing %s:\n", input_file));
        BufferedReader safe_reader = null;
        try {
            safe_reader = IOUtils.safe_reader(input_file);
            // output warnings
            ShibUserLogins user_logins = new ShibUserLogins(safe_reader);
            if (0 < user_logins.warnings().size()) {
                for (String warning : user_logins.warnings()) {
                    ret.append(warning + "\n");
                }
            } else {
                ret.append("No shibboleth warnings have been found.\n");
            }
        } catch (Exception e) {
            error(e, String.format("Problematic file [%s]", default_log));
            return ret.toString();
        }

        // > WARN from shibd_warn in the last week
        ret.append("\nParsing shibd_warn.*:\n");
        File dir = new File(input_dir);
        String[] files = dir.list(new java.io.FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("shibd_warn");
            }
        });
        Long nowMillis = System.currentTimeMillis();
        String weekAgo = new SimpleDateFormat("yyyy-MM-dd").format(new Date(
                nowMillis - 604800000));
        String[] cmd = new String[] { "awk", "-v", "from=" + weekAgo,
                "BEGIN{FS=\" \"} {if($1>=from) print $0}" };
        String[] cmdWithFiles = new String[cmd.length + files.length];
        System.arraycopy(cmd, 0, cmdWithFiles, 0, cmd.length);
        System.arraycopy(files, 0, cmdWithFiles, cmd.length, files.length);
        try {
            Process child = Runtime.getRuntime().exec(cmdWithFiles, null, dir);
            BufferedReader[] outputs = new BufferedReader[] {
                    new BufferedReader(new InputStreamReader(
                            child.getInputStream())),
                    new BufferedReader(new InputStreamReader(
                            child.getErrorStream())) };
            for (BufferedReader out : outputs) {
                String s = null;
                while ((s = out.readLine()) != null) {
                    ret.append(s);
                    ret.append("\n");
                }
            }
        } catch (java.io.IOException e) {
            error(e);
        }


        try {
            String feedsConfig = ConfigurationManager.getProperty("discojuice", "feeds");
            ret.append(String.format( "Using these static discojuice feeds [%s] as source.\n",
                feedsConfig ));
            // Try to download our feeds file, so the proper action is triggered
            StringWriter writer = new StringWriter();
            org.apache.commons.io.IOUtils.copy(
                new URL(ConfigurationManager.getProperty("dspace.url")
                    + "/discojuice/feeds").openStream(), writer);
            String jsonp = writer.toString();
            // end download
            String json = jsonp.substring(jsonp.indexOf("(") + 1,
                jsonp.lastIndexOf(")")); // strip the dj_md_1()
            Set<String> entities = new HashSet<String>();
            JSONParser parser = new JSONParser();
            JSONArray entityArray = (JSONArray) parser.parse(json);
            Iterator<JSONObject> i = entityArray.iterator();
            int counter = 0;
            while (i.hasNext()) {
                counter++;
                JSONObject entity = i.next();
                String entityID = (String) entity.get("entityID");
                entities.add(entityID);
            }
            int idCount = entities.size();
            ret.append(String.format(
                "Our feeds file contains %d entities out of which %d are unique.\n"
                    + "This number should be around 1200?? (20.11.2014).",
                counter, idCount)
            );
        } catch (Exception e) {
            error(e);
        }

        return ret.toString();
    }
}
