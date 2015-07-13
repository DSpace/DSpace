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

import cz.cuni.mff.ufal.Info;
import cz.cuni.mff.ufal.dspace.IOUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.io.File;
import java.text.SimpleDateFormat;

public class AdditionalInfoCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        String url = ConfigurationManager.getProperty("dspace.url");

        ret += String.format(
            "Dspace built:  %s\n", Info.get_ufal_build_time());
        ret += String.format(
            "Server uptime: %s\n", Info.get_proc_uptime());

        ret += "\n\n";

        ret += String.format(
            "Statistics:       %s/statistics?date=%s\n", url,
            new SimpleDateFormat("yyyy-M").format(ri.till()));
        ret +=
            "Project info:     http://svn.ms.mff.cuni.cz/redmine/projects/dspace-modifications\n";
        ret +=
            "QA monitoring:    http://ufal-point-dev.ms.mff.cuni.cz/nagios3/\n";
        ret +=
            "QA testing:       http://ufal-point-dev.ms.mff.cuni.cz:8083/\n";
        ret += String.format(
            "GA stats:         %s/statistics-google\n", url);
        ret += String.format(
            "CP info:          %s/admin/panel?java\n", url);
        ret += String.format(
            "CP conf:          %s/admin/panel?dspace\n", url);
        ret += String.format(
            "Clarin harvest:   %s\n", ConfigurationManager.getProperty("lr", "lr.harvester.info.url"));
        ret +=
            "Clarin VLO:       http://www.clarin.eu/vlo\n";
        ret +=
            "Clarin centers:   https://centerregistry-clarin.esc.rzg.mpg.de/centers/\n";
        ret +=
            "INL harvest:      https://portal.clarin.inl.nl/imdiportal/BC?virtpath=/TST-LRs/External%20Resources/OLAC%20Metadata%20Providers/Providers/Charles_University_Prague\n";

        ret += IOUtils.run(
            new File(ConfigurationManager.getProperty("dspace.dir")),
            new String[]{"sudo", "cat", "/proc/user_beancounters"});

        return ret;
    }

}
