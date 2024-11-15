/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import static org.dspace.app.suggestion.SuggestionUtils.getAllEntriesByMetadatum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@see org.dspace.app.suggestion.oaire.EvidenceScorer} which evaluate ImportRecords
 * based on Author's name.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class AuthorNamesScorer implements EvidenceScorer {

    private List<String> contributorMetadata;

    private List<String> names;

    @Autowired
    private ItemService itemService;

    /**
     * returns the metadata key of the Item which to base the filter on
     * @return metadata key
     */
    public List<String> getContributorMetadata() {
        return contributorMetadata;
    }

    /**
     * set the metadata key of the Item which to base the filter on
     */
    public void setContributorMetadata(List<String> contributorMetadata) {
        this.contributorMetadata = contributorMetadata;
    }

    /**
     * return the metadata key of ImportRecord which to base the filter on
     * @return
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * set the metadata key of ImportRecord which to base the filter on
     */
    public void setNames(List<String> names) {
        this.names = names;
    }

    /**
     * Method which is responsible to evaluate ImportRecord based on authors name.
     * This method extract the researcher name from Item using contributorMetadata fields
     * and try to match them with values extract from ImportRecord using metadata keys defined
     * in names.
     * ImportRecords which don't match will be discarded.
     * 
     * @param importRecord the import record to check
     * @param researcher DSpace item
     * @return the generated evidence or null if the record must be discarded
     */
    @Override
    public SuggestionEvidence computeEvidence(Item researcher, ExternalDataObject importRecord) {
        List<String[]> names = searchMetadataValues(researcher);
        int maxNameLenght = names.stream().mapToInt(n -> n[0].length()).max().orElse(1);
        List<String> metadataAuthors = new ArrayList<>();
        for (String contributorMetadatum : contributorMetadata) {
            metadataAuthors.addAll(getAllEntriesByMetadatum(importRecord, contributorMetadatum));
        }
        List<String> normalizedMetadataAuthors = metadataAuthors.stream().map(x -> normalize(x))
                .collect(Collectors.toList());
        int idx = 0;
        for (String nMetadataAuthor : normalizedMetadataAuthors) {
            Optional<String[]> found = names.stream()
                    .filter(a -> StringUtils.equalsIgnoreCase(a[0], nMetadataAuthor)).findFirst();
            if (found.isPresent()) {
                return new SuggestionEvidence(this.getClass().getSimpleName(),
                        100 * ((double) nMetadataAuthor.length() / (double) maxNameLenght),
                        "The author " + metadataAuthors.get(idx) + " at position " + (idx + 1)
                                + " in the authors list matches the name " + found.get()[1]
                                + " in the researcher profile");
            }
            idx++;
        }
        return null;
    }

    /**
     * Return list of Item metadata values starting from metadata keys defined in class level variable names.
     * 
     * @param researcher DSpace item
     * @return list of metadata values
     */
    private List<String[]> searchMetadataValues(Item researcher) {
        List<String[]> authors = new ArrayList<String[]>();
        for (String name : names) {
            List<MetadataValue> values = itemService.getMetadataByMetadataString(researcher, name);
            if (values != null) {
                for (MetadataValue v : values) {
                    authors.add(new String[] {normalize(v.getValue()), v.getValue()});
                }
            }
        }
        return authors;
    }

    /**
     * cleans up undesired characters
     * @param value the string to clean up
     * @return cleaned up string
     * */
    private String normalize(String value) {
        String norm = Normalizer.normalize(value, Normalizer.NFD);
        CharsetDetector cd = new CharsetDetector();
        cd.setText(value.getBytes());
        CharsetMatch detect = cd.detect();
        if (detect != null && detect.getLanguage() != null) {
            norm = norm.replaceAll("[^\\p{L}]", " ").toLowerCase(new Locale(detect.getLanguage()));
        } else {
            norm = norm.replaceAll("[^\\p{L}]", " ").toLowerCase();
        }
        return Arrays.asList(norm.split("\\s+")).stream().sorted().collect(Collectors.joining());
    }

}
