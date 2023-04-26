/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
package org.dspace.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *  Class is copied from the LINDAT/CLARIAH-CZ (This class is taken from UFAL-clarin.
 *  https://github.com/ufal/clarin-dspace/blob
 *  * /si-master-origin/dspace-oai/src/main/java/cz/cuni/mff/ufal/utils/LicenseUtil.java) and modified by
 *
 *  @author Marian Berger (marian.berger at dataquest.sk)
 *  @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class LicenseUtil {

    private LicenseUtil() {
    }

    /** log4j logger */
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(LicenseUtil.class);

    /*
     * values:
     *                  <xs:enumeration value="CC-BY"/>
                        <xs:enumeration value="CC-BY-NC"/>
                        <xs:enumeration value="CC-BY-NC-ND"/>
                        <xs:enumeration value="CC-BY-NC-SA"/>
                        <xs:enumeration value="CC-BY-ND"/>
                        <xs:enumeration value="CC-BY-SA"/>
                        <xs:enumeration value="CC-ZERO"/>
                        <xs:enumeration value="MS-C-NoReD"/>
                        <xs:enumeration value="MS-C-NoReD-FF"/>
                        <xs:enumeration value="MS-C-NoReD-ND"/>
                        <xs:enumeration value="MS-C-NoReD-ND-FF"/>
                        <xs:enumeration value="MS-NC-NoReD"/>
                        <xs:enumeration value="MS-NC-NoReD-FF"/>
                        <xs:enumeration value="MS-NC-NoReD-ND"/>
                        <xs:enumeration value="MS-NC-NoReD-ND-FF"/>
                        <xs:enumeration value="MSCommons-BY"/>
                        <xs:enumeration value="MSCommons-BY-NC"/>
                        <xs:enumeration value="MSCommons-BY-NC-ND"/>
                        <xs:enumeration value="MSCommons-BY-NC-SA"/>
                        <xs:enumeration value="MSCommons-BY-ND"/>
                        <xs:enumeration value="MSCommons-BY-SA"/>
                        <xs:enumeration value="CLARIN_ACA"/>
                        <xs:enumeration value="CLARIN_ACA-NC"/>
                        <xs:enumeration value="CLARIN_PUB"/>
                        <xs:enumeration value="CLARIN_RES"/>
                        <xs:enumeration value="ELRA_END_USER"/>
                        <xs:enumeration value="ELRA_EVALUATION"/>
                        <xs:enumeration value="ELRA_VAR"/>
                        <xs:enumeration value="AGPL"/>
                        <xs:enumeration value="ApacheLicence_2.0"/>
                        <xs:enumeration value="BSD"/>
                        <xs:enumeration value="BSD-style"/>
                        <xs:enumeration value="GFDL"/>
                        <xs:enumeration value="GPL"/>
                        <xs:enumeration value="LGPL"/>
                        <xs:enumeration value="Princeton_Wordnet"/>
                        <xs:enumeration value="proprietary"/>
                        <xs:enumeration value="underNegotiation"/>
                        <xs:enumeration value="other"/>

     */
    private static final String[] _uri2metashareDefs = {
        "http://opensource.org/licenses/GPL-3.0,GPL",
        "http://www.gnu.org/licenses/gpl-2.0.html,GPL",
        "http://opensource.org/licenses/BSD-2-Clause,BSD",
        "http://opensource.org/licenses/BSD-3-Clause,BSD",
        "http://www.apache.org/licenses/LICENSE-2.0,ApacheLicence_2.0",
        "http://creativecommons.org/licenses/by-nc/3.0/,CC-BY-NC",
        "http://creativecommons.org/licenses/by-nc-sa/3.0/,CC-BY-NC-SA",
        "http://creativecommons.org/licenses/by-nd/3.0/,CC-BY-ND",
        "http://creativecommons.org/licenses/by-sa/3.0/,CC-BY-SA",
        "http://creativecommons.org/licenses/by/3.0/,CC-BY",
        "http://creativecommons.org/licenses/by-nc-nd/3.0/,CC-BY-NC-ND",
        "http://creativecommons.org/choose/zero/,CC-ZERO"
    };

    /*
     * values:
     *                  <xs:enumeration value="informLicensor"/>
                        <xs:enumeration value="redeposit"/>
                        <xs:enumeration value="onlyMSmembers"/>
                        <xs:enumeration value="academic-nonCommercialUse"/>
                        <xs:enumeration value="evaluationUse"/>
                        <xs:enumeration value="commercialUse"/>
                        <xs:enumeration value="attribution"/>
                        <xs:enumeration value="shareAlike"/>
                        <xs:enumeration value="noDerivatives"/>
                        <xs:enumeration value="noRedistribution"/>
                        <xs:enumeration value="other"/>

     */
    private static final String[] _restrictionDefs = {
            ccToRes("http://creativecommons.org/licenses/by-nc/3.0/"),
            ccToRes("http://creativecommons.org/licenses/by-nc-sa/3.0/"),
            ccToRes("http://creativecommons.org/licenses/by-nd/3.0/"),
            ccToRes("http://creativecommons.org/licenses/by-sa/3.0/"),
            ccToRes("http://creativecommons.org/licenses/by/3.0/"),
            ccToRes("http://creativecommons.org/licenses/by-nc-nd/3.0/")
    };

    private static String ccToRes(String ccuri) {
        String ld = ccuri.replaceFirst("http://creativecommons.org/licenses/", "").replaceFirst("/3.0/", "");
        String ret = ccuri + "©";
        for (String tag : ld.split("-")) {
            if (tag.equals("by")) {
                ret += "attribution,";
            } else if (tag.equals("nc")) {
                ret += "academic-nonCommercialUse,";
            } else if (tag.equals("sa")) {
                ret += "shareAlike,";
            } else if (tag.equals("nd")) {
                ret += "noDerivatives,";
            }
        }
        return ret;
    }

    private static final Set<String> unrestricted = Collections.unmodifiableSet(new HashSet<String>());

    private static final Map<String, String> _uri2metashare = uri2metashare();
    private static final Map<String, String> _uri2restrictions = uri2restrictions();

    private static Map<String, String> uri2metashare() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String def : _uri2metashareDefs) {
            String[] defn = def.split(",", 2);
            if (defn.length < 2) {
                log.warn("Bad string " + def + ", should have at least two parts " +
                        "delimited by ,");
                map.put("", "");
            } else {
                map.put(defn[0], defn[1]);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> uri2restrictions() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String def : _restrictionDefs) {
            String[] defn = def.split("©", 2);
            if (defn.length < 2) {
                log.warn("Bad string " + def + ", should have at least two parts " +
                        "delimited by ,");
                map.put("", "");
            } else {
                map.put(defn[0], defn[1]);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     *
     * @param uri uri
     * @return returns string mapped to uri. If not mapped, returns "other". I don't know why.
     */
    public static String uriToMetashare(String uri) {
        String mapped = _uri2metashare.get(uri);
        if (Objects.nonNull(mapped)) {
            return mapped;
        } else {

            return "other";
        }

    }

    /**
     *
     * @param uri
     * @return available-restrictedUse if not present in unrestricted
     */
    public static String uriToAvailability(String uri) {
        if (unrestricted.contains(uri)) {
            return "available-unrestrictedUse";
        }
        return "available-restrictedUse";
    }

    public static org.w3c.dom.NodeList uriToRestrictions(String uri)
            throws ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder;
        builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element root = doc.createElement("restrictions");

        String restrictions = _uri2restrictions.get(uri);
        if (Objects.isNull(restrictions)) {
            restrictions = "other";
        }

        for (String restriction : restrictions.split(",")) {
            Element res = doc.createElement("restriction");
            res.appendChild(doc.createTextNode(restriction));
            root.appendChild(res);
        }

        return root.getElementsByTagName("restriction");
    }

    public static void main(String[] args) throws Exception {
        System.out.println(uriToMetashare("http://creativecommons.org/licenses/by-nc/3.0/"));
        System.out.println(uriToAvailability("http://creativecommons.org/licenses/by-nc/3.0/"));
    }

}
