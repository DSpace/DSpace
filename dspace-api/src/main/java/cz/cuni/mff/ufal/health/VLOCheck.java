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

import org.dspace.core.ConfigurationManager;
import org.dspace.health.Check;
import org.dspace.health.Core;
import org.dspace.health.ReportInfo;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VLOCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        final HashMap<String, String> info = new HashMap<String, String>();
        final List<String> errors = new ArrayList<String>();
        try {
            String harvesterInfoUrl = ConfigurationManager.getProperty("lr",
                "lr.harvester.info.url");
            if (harvesterInfoUrl == null
                    || harvesterInfoUrl.trim().length() == 0) {
                return "PLEASE configure lr.harvester.info.url";
            }
            final String harvesterInfoAnchorName = ConfigurationManager
                    .getProperty("lr", "lr.harvester.info.anchorName");
            if (harvesterInfoAnchorName == null
                    || harvesterInfoAnchorName.trim().length() == 0) {
                return "PLEASE configure lr.harvester.info.anchorName";
            }
            // Try to download the page
            StringWriter writer = new StringWriter();
            org.apache.commons.io.IOUtils.copy(
                    new URL(harvesterInfoUrl.trim()).openStream(), writer);
            String page = writer.toString();
            // String page =
            // org.apache.commons.io.FileUtils.readFileToString(new
            // File("/tmp/lindat.html"));
            // end download

            Reader reader = new StringReader(page);
            HTMLEditorKit.Parser parser = new ParserDelegator();
            parser.parse(reader, new HTMLEditorKit.ParserCallback() {
                boolean contentDiv = false;
                boolean content_a = false;
                boolean content_p = false;
                String p_text;
                boolean content_p_strong = false;
                boolean content_table_tr = false;
                java.util.LinkedList<String> row = new java.util.LinkedList<String>();
                boolean content_table_td = false;
                String title;

                public void handleStartTag(HTML.Tag tag,
                                           MutableAttributeSet attrSet, int pos) {
                    if (tag.toString().equals(HTML.Tag.DIV.toString())) {
                        if (attrSet.containsAttribute(HTML.Attribute.ID,
                            "content")) {
                            contentDiv = true;
                        }
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.A.toString())) {
                        if (attrSet.containsAttribute(HTML.Attribute.NAME,
                            harvesterInfoAnchorName.trim())) {
                            content_a = true;
                        }
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.P.toString())) {
                        content_p = true;
                    } else if (content_p
                        && tag.toString().equals(HTML.Tag.STRONG.toString())) {
                        content_p_strong = true;
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.TR.toString())) {
                        content_table_tr = true;
                    } else if (content_table_tr
                        && tag.toString().equals(HTML.Tag.TD.toString())) {
                        Object titleAtr = attrSet
                            .getAttribute(HTML.Attribute.TITLE);
                        if (titleAtr != null) {
                            title = titleAtr.toString();
                        }
                        content_table_td = true;
                    }
                }

                public void handleText(char[] data, int pos) {
                    String text = new String(data);
                    if (content_a) {
                        // ret.append(data).append("\n");
                        info.put("vlo_records",
                            text.replaceAll(".*\\((\\d+).*", "$1"));
                    }
                    if (content_p && p_text == null) {
                        p_text = text;
                    }
                    if (content_p_strong
                        && p_text.contains("records were updated")) {
                        info.put("updated", text);
                    }
                    if (content_table_td) {
                        row.push(text);
                        if (row.size() == 1 && title == null) {
                            title = text;
                        }
                    }
                }

                /*
                 * public void handleEndOfLineString(String data) { // This is
                 * invoked after the stream has been parsed, but before flush.
                 * // eol will be one of \n, \r or \r\n, which ever is //
                 * encountered the most in parsing the stream.
                 * System.out.println("End of Line String => " + data); }
                 */

                public void handleEndTag(HTML.Tag tag, int pos) {
                    if (tag.toString().equals(HTML.Tag.DIV.toString()) && contentDiv) {
                        contentDiv = false;
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.A.toString())
                        && content_a) {
                        content_a = false;
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.P.toString())) {
                        content_p = false;
                        p_text = null;
                    } else if (content_p
                        && tag.toString().equals(HTML.Tag.STRONG.toString())) {
                        content_p_strong = false;
                    } else if (contentDiv
                        && tag.toString().equals(HTML.Tag.TR.toString())) {
                        content_table_tr = false;
                        // find errors in row
                        for (String td_text : row) {
                            if (td_text.toLowerCase().contains("error")) {
                                errors.add(title);
                            }
                        }
                        row = new java.util.LinkedList<String>();
                        title = null;
                    } else if (content_table_tr
                        && tag.toString().equals(HTML.Tag.TD.toString())) {
                        content_table_td = false;
                    }
                }

                public void handleError(String err, int pos) {
                    // System.out.println("Error => " + err);
                }
            }, true);
            reader.close();
            info.put("total_records", String.valueOf(Core.getItemsTotalCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String ret = String
            .format("Number of records in vlo is %s (we have %s items in our repository).\nThe records were harvested at %s.\nIt contains %s errors.\n",
                info.get("vlo_records"), info.get("total_records"),
                        info.get("updated"), errors.size());
        if (errors.size() > 0) {
            ret += "Erroneous ids:\n";
            for (String title : errors) {
                ret += title + "\n";
            }
        }
        return ret;
    }

}
