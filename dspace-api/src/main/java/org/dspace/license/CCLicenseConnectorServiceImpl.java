/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for the Creative Commons license connector service.
 *
 * <p>This service builds Creative Commons license definitions from a local CSV
 * configuration and resolves license URIs based on user-selected field values.</p>
 *
 * <p>It does not perform any outbound HTTP calls. All license metadata is derived
 * from local configuration files:</p>
 * <ul>
 *   <li>{@code dspace.cc-license.csv} — license definitions and URL mappings</li>
 *   <li>{@code dspace.cc-license.rdf} — RDF metadata for license names and titles</li>
 * </ul>
 *
 * <p>The service also applies business rules for mapping user selections
 * (commercial use, derivatives, jurisdiction) into CC license units.</p>
 */
public class CCLicenseConnectorServiceImpl implements CCLicenseConnectorService, InitializingBean {

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(CCLicenseConnectorServiceImpl.class);

    @Autowired
    private CCLicenseCSVRepository csvRepo;

    // Answer map keys
    private static final String FIELD_COMMERCIAL  = "commercial";
    private static final String FIELD_DERIVATIVES = "derivatives";
    private static final String FIELD_JURISDICTION = "jurisdiction";

    // RDF namespaces
    private static final Namespace NS_RDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace NS_CC = Namespace.getNamespace("cc", "http://creativecommons.org/ns#");
    private static final Namespace NS_DCTERMS = Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace NS_OWL = Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#");

    protected SAXBuilder parser = new SAXBuilder();

    /** Lazily-parsed RDF document, shared across calls. */
    private Document rdfDocument;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void afterPropertiesSet() throws Exception {
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }

    /**
     * Builds the list of available license options for the UI.
     *
     * <p>Licenses are grouped based on the CSV {@code ENTRY_POINT} column:</p>
     * <ul>
     *   <li>{@code CC_GROUP} — grouped Creative Commons license families (e.g. CC 3.0, 4.0)</li>
     *   <li>{@code DIRECT} — standalone licenses (e.g. public domain tools)</li>
     * </ul>
     *
     * <p>CC_GROUP entries share a common license structure but differ in version and optional jurisdiction support.</p>
     *
     * @param language the current language in the ui
     *
     * @return A map of licenseId's to CCLicense
     */
    @Override
    public Map<String, CCLicense> retrieveLicenses(String language) {
        Map<String, CCLicense> licenses = new LinkedHashMap<>();

        for (CCLicenseCSVRow row : csvRepo.getEntryPoints()) {

            if ("CC_GROUP".equals(row.getEntryPoint())) {
                String id = "CC " + row.getVersion();
                boolean includeJurisdiction = "3.0".equals(row.getVersion());

                if (!licenses.containsKey(id)) {
                    licenses.put(id,
                            buildCCLicense(
                                    id,
                                    "Creative Commons License (" + row.getVersion() + ")",
                                    includeJurisdiction
                            )
                    );
                }
            } else if ("DIRECT".equals(row.getEntryPoint())) {
                licenses.put(
                        row.getIdentifier(),
                        new CCLicense(
                                row.getIdentifier(),
                                row.getTitle(),
                                Collections.emptyList()
                        )
                );
            }
        }

        return licenses;
    }

    /**
     * Builds a CC license with the standard commercial/derivatives fields,
     * and optionally a jurisdiction field for 3.0.
     *
     * @param id The ID of the CC License
     * @param label The Label to user for the License
     * @param includeJurisdiction   If Jurisdiction information should be included only used for 3.0
     *
     * @return Created CCLicense
     */
    private CCLicense buildCCLicense(String id, String label, boolean includeJurisdiction) {
        CCLicenseField commercial = new CCLicenseField(
                FIELD_COMMERCIAL, "Allow commercial uses of your work?", "",
                List.of(new CCLicenseFieldEnum("y", "Yes (visitors may copy, distribute, display and perform your " +
                                        "Submission for any purpose, including commercial purposes)", ""),
                        new CCLicenseFieldEnum("n", "No (visitors may copy, distribute, display and perform your " +
                                        "Submission, but only for non-commercial purposes)", "")
                )
        );

        CCLicenseField derivatives = new CCLicenseField(FIELD_DERIVATIVES, "Allow modifications of your work?", "",
                List.of(
                        new CCLicenseFieldEnum("y", "Yes", ""),
                        new CCLicenseFieldEnum("sa", "ShareAlike (visitors are permitted to copy, display," +
                                " perform and modify your Submission, as long as they distribute the modified" +
                                " version on similar use terms)", ""),
                        new CCLicenseFieldEnum("n", "No (visitors are only permitted to copy and distribute" +
                                " unaltered versions of your Submission)", "")
                )
        );

        if (!includeJurisdiction) {
            return new CCLicense(id, label, List.of(commercial, derivatives));
        }

        // Build jurisdiction enum from the CSV — all distinct non-blank jurisdiction
        // codes that appear under version 3.0 in the CSV.
        CCLicenseField jurisdiction = new CCLicenseField(
                FIELD_JURISDICTION,
                "Jurisdiction",
                "",
                buildJurisdictionEnums()
        );


        return new CCLicense(id, label, List.of(commercial, derivatives, jurisdiction));
    }

    /**
     * Builds a list of valid JurisdictionsEnums for the given liceneses in configured csv.
     *
     * @return Valid CCLicenseFieldEnum's
     */
    private List<CCLicenseFieldEnum> buildJurisdictionEnums() {

        // 1. Check config override
        String configured = configurationService.getProperty("dspace.cc-license.jurisdiction");

        Map<String, CCLicenseFieldEnum> seen = new LinkedHashMap<>();

        // Always include generic/unported first
        seen.put("", new CCLicenseFieldEnum("", "Generic (Unported)", ""));

        if (configured != null && !configured.isBlank()) {
            String code = configured.trim();
            seen.put(code, new CCLicenseFieldEnum(code, code.toUpperCase(), ""));
            return List.copyOf(seen.values());
        }

        // 2. Otherwise derive from repository (NO CSV IO)
        for (String code : csvRepo.getJurisdictionsByVersion("3.0")) {
            if (!seen.containsKey(code)) {
                seen.put(code, new CCLicenseFieldEnum(code, code.toUpperCase(), ""));
            }
        }

        return List.copyOf(seen.values());
    }

    /**
     * Resolves the canonical URL for a license selection.
     *
     * For CC0, PDM, and PDUS the URL is looked up directly by IDENTIFIER in the CSV.
     *
     * For CC 4.0 and CC 3.0, the {@code commercial} and {@code derivatives}
     * answers are mapped to a CC unit string (e.g. {@code by-nc-sa}), then the
     * CSV is searched for a row matching version + unit + jurisdiction.
     *
     * @param licenseId The id of the given license
     * @param language The current language
     * @param answerMap The current answers
     *
     * @return The canonical URL for a license selection
     */
    @Override
    public String retrieveRightsByQuestion(String licenseId, String language,
                                           Map<String, String> answerMap) {

        CCLicenseCSVRow entry = csvRepo.getByIdentifier(licenseId);

        if (entry == null) {
            log.warn("Unknown licenseId: {}", licenseId);
            return null;
        }

        // Direct lookup licenses (CC0, PDM, PDUS, etc.)
        if (isDirectLicense(entry)) {
            return entry.getUrl();
        }

        // CC licenses (3.0 / 4.0)
        if (isCreativeCommons(entry)) {
            String version = entry.getVersion();
            String commercial = answerMap.get(FIELD_COMMERCIAL);
            String derivatives = answerMap.get(FIELD_DERIVATIVES);

            String unit = resolveUnit(commercial, derivatives);

            String jurisdiction = "";
            if ("3.0".equals(version)) {
                jurisdiction = answerMap.get(FIELD_JURISDICTION);
            }

            return csvRepo.findUri(version, unit, jurisdiction);
        }

        log.warn("Unhandled license type: {}", licenseId);
        return null;
    }

    /**
     * Maps user answers for commercial use and derivative permissions into a
     * Creative Commons license unit identifier.
     *
     * <p>This method encodes the Creative Commons license decision matrix and
     * determines the correct license component string (e.g. {@code by-nc-sa}).</p>
     *
     * <p>Valid combinations correspond to standard CC license variants.</p>
     * @param commercial the commercial license
     * @param derivatives the derivative answers of that license
     * @return The license usage string
     */
    private String resolveUnit(String commercial, String derivatives) {
        boolean allowCommercial = "y".equals(commercial);
        boolean allowDerivatives = "y".equals(derivatives);
        boolean shareAlike = "sa".equals(derivatives);

        if (allowCommercial && allowDerivatives) {
            return "by";
        }
        if (allowCommercial && shareAlike) {
            return "by-sa";
        }
        if (allowCommercial) {
            return "by-nd";
        }
        if (allowDerivatives) {
            return "by-nc";
        }
        if (shareAlike) {
            return "by-nc-sa";
        }
        return "by-nc-nd";
    }

    /**
     * Locates the {@code cc:License} element in the local {@code index.rdf} whose
     * {@code rdf:about} or {@code owl:sameAs} attribute matches {@code licenseURI}
     * and returns it wrapped in a single-element Document.
     *
     * @param licenseURI
     * @return The valid document for the given licenseURI
     */
    @Override
    public Document retrieveLicenseRDFDoc(String licenseURI) throws IOException {
        try {
            Document index = getOrParseRdfDocument();
            if (index == null) {
                return null;
            }

            String normalisedURI = normaliseUri(licenseURI);

            for (Element license : index.getRootElement().getChildren("License", NS_CC)) {
                String about = license.getAttributeValue("about", NS_RDF);

                Element sameAsEl = license.getChild("sameAs", NS_OWL);
                String sameAs = (sameAsEl != null)
                        ? sameAsEl.getAttributeValue("resource", NS_RDF)
                        : null;

                if (normalisedURI.equals(normaliseUri(about))
                        || normalisedURI.equals(normaliseUri(sameAs))) {
                    Element rdf = new Element("RDF", NS_RDF);
                    rdf.addContent(license.clone());

                    Element rdfWrapper = new Element("rdf", NS_RDF);
                    rdfWrapper.addContent(rdf);

                    Element result = new Element("result");
                    result.addContent(rdfWrapper);

                    return new Document(result);
                }
            }

            log.warn("No cc:License found in RDF for URI: " + licenseURI);
        } catch (Exception e) {
            log.error("Error retrieving RDF license document for URI: " + licenseURI, e);
        }
        return null;
    }

    /**
     * Extracts the English {@code dcterms:title} from a {@code cc:License} element
     * document returned by {@link #retrieveLicenseRDFDoc}.
     * Falls back to the first available title if no English title is present.
     *
     * @param doc The RDF index document for this license
     *
     * @return the license name
     */
    @Override
    public String retrieveLicenseName(final Document doc) {
        if (doc == null || doc.getRootElement() == null) {
            return null;
        }

        Element root = doc.getRootElement();
        String fallback = null;

        List<Element> titles = XPathFactory.instance()
                .compile(".//dcterms:title", Filters.element(), null, NS_DCTERMS)
                .evaluate(root);

        if (!titles.isEmpty()) {
            for (Element title : titles) {
                String lang = title.getAttributeValue("lang", Namespace.XML_NAMESPACE);
                String value = title.getTextTrim();

                if ("en".equals(lang)) {
                    return value;
                }
            }

            // fallback if no English match
            return titles.get(0).getTextTrim();
        }
        return fallback;
    }

    private Document getOrParseRdfDocument() {
        if (rdfDocument != null) {
            return rdfDocument;
        }
        String rdfPath = configurationService.getProperty("dspace.cc-license.rdf");
        if (StringUtils.isBlank(rdfPath)) {
            log.error("Configuration property 'dspace.cc-license.rdf' is not set.");
            return null;
        }
        try (InputStream is = Files.newInputStream(Paths.get(rdfPath))) {
            rdfDocument = parser.build(is);
        } catch (Exception e) {
            log.error("Failed to parse CC license RDF from: " + rdfPath, e);
        }
        return rdfDocument;
    }

    /**
     * Normalizes a license URI for comparison purposes by converting HTTPS
     * schemes to HTTP to match legacy RDF values.
     */
    private static String normaliseUri(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.startsWith("https://") ? "http://" + uri.substring(8) : uri;
    }

    private boolean isDirectLicense(CCLicenseCSVRow row) {
        return "publicdomain".equalsIgnoreCase(row.getCategory());
    }

    private boolean isCreativeCommons(CCLicenseCSVRow row) {
        return "licenses".equalsIgnoreCase(row.getCategory());
    }
}
