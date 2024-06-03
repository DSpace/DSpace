/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.orcid.xml;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.xml.sax.SAXException;

/**
 * Convert the XML response from the ORCID API to a list of Results
 * The conversion here is sort of a layer between the Choice class and the ORCID classes
 */
public class ExpandedSearchConverter extends Converter<ExpandedSearchConverter.Results> {

    public static final ExpandedSearchConverter.Results ERROR =
            new ExpandedSearchConverter.Results(new ArrayList<>(), 0L, false);

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ExpandedSearchConverter.class);

    @Override
    public ExpandedSearchConverter.Results convert(InputStream inputStream) {
        try {
            ExpandedSearch search = (ExpandedSearch) unmarshall(inputStream, ExpandedSearch.class);
            long numFound = search.getNumFound();
            return new Results(search.getResults().stream()
                    .filter(Objects::nonNull)
                    .filter(result -> isNotBlank(result.getOrcidId()))
                    .map(ExpandedSearchConverter.Result::new)
                    .collect(Collectors.toList()), numFound);
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return ERROR;
    }


    /**
     * Keeps the results and their total number
     */
    public static final class Results {
        private final List<Result> results;
        private final Long numFound;

        private final boolean ok;

        Results(List<Result> results, Long numFound) {
            this(results, numFound, true);
        }

        Results(List<Result> results, Long numFound, boolean ok) {
            this.results = results;
            this.numFound = numFound;
            this.ok = ok;
        }


        /**
         * The results
         * @return the results as List
         */
        public List<Result> results() {
            return results;
        }

        /**
         * The total number of results
         * @return the number of results
         */
        public Long numFound() {
            return numFound;
        }

        /**
         * Whether there were any issues
         * @return false if there were issues
         */
        public boolean isOk() {
            return ok;
        }

        @Override
        public String toString() {
            return "Results[" +
                    "results=" + results + ", " +
                    "numFound=" + numFound + ']';
        }

    }

    /**
     * Represents a single result
     * Taking care of potential null/empty values
     */
    public static final class Result {
        private final String authority;
        private final String value;
        private final String label;
        private final String creditName;
        private final String[] otherNames;
        private final String[] institutionNames;

        Result(ExpandedResult result) {
            if (isBlank(result.getOrcidId())) {
                throw new IllegalArgumentException("OrcidId is required");
            }
            final String last = isNotBlank(result.getFamilyNames()) ? result.getFamilyNames() : "";
            final String first = isNotBlank(result.getGivenNames()) ? result.getGivenNames() : "";
            final String maybeComma = isNotBlank(last) && isNotBlank(first) ? ", " : "";
            String displayName = String.format("%s%s%s", last, maybeComma, first);
            displayName = isNotBlank(displayName) ? displayName : result.getOrcidId();

            this.authority = result.getOrcidId();
            this.value = displayName;
            this.label = displayName;

            this.creditName = result.getCreditName();
            this.otherNames = result.getOtherNames();
            this.institutionNames = result.getInstitutionNames();
        }

        /**
         * The authority value
         * @return orcid
         */
        public String authority() {
            return authority;
        }

        /**
         * The value to store
         * @return the value
         */
        public String value() {
            return value;
        }

        /**
         * The label to display
         * @return the label
         */
        public String label() {
            return label;
        }

        /**
         * Optional extra info - credit name
         * @return the credit name
         */
        public Optional<String> creditName() {
            return Optional.ofNullable(creditName);
        }

        /**
         * Optional extra info - other names
         * @return other names
         */
        public Optional<String> otherNames() {
            return Optional.ofNullable(otherNames).map(names -> String.join(" | ", names));
        }

        /**
         * Optional extra info - institution names
         * @return institution names
         */
        public Optional<String> institutionNames() {
            //joining with newline doesn't seem to matter for ui
            return Optional.ofNullable(institutionNames) .map(names -> String.join(" | ", names));
        }

        @Override
        public String toString() {
            return "Result[" +
                    "authority=" + authority + ", " +
                    "value=" + value + ", " +
                    "label=" + label + ", " +
                    "creditNames=" + creditName + ", " +
                    "otherNames=" + Arrays.toString(otherNames) + ", " +
                    "institutionNames=" + Arrays.toString(institutionNames) + ']';
        }
    }
}