/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.openAire.service.OpenAIREImportMetadataSourceServiceImpl;
import org.dspace.utils.DSpace;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAIREProjectAuthority implements ChoiceAuthority {

    public static final String SPLIT = "::";
    public static final String GENERATE = "will be generated" + SPLIT;
    public static final String OPENAIRE_PROJECT_AUTHORITY_TYPE = "openAireProject";

    private OpenAIREImportMetadataSourceServiceImpl openAIREProjectService = new DSpace().getServiceManager()
                          .getServiceByName("OpenAIREService", OpenAIREImportMetadataSourceServiceImpl.class);

    private static DSpace dspace = new DSpace();

    @Override
    public Choices getBestMatch(String field, String locale) {
        return getMatches(field, 0, 2, locale);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        List<ImportRecord> records;
        try {
            records = (List<ImportRecord>) openAIREProjectService.getRecords(text, start, limit);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        List<Choice> choiceList = new ArrayList<Choice>();
        if (records != null && !records.isEmpty()) {
            for (ImportRecord ir : records) {
                String authority = null;
                String value = null;
                String label = null;
                String code = null;
                List<MetadatumDTO> metadatums = ir.getValueList();
                for (MetadatumDTO metadatum : metadatums) {
                    if (metadatum.getElement().equals("title")) {
                        value = metadatum.getValue();
                    }
                    if (metadatum.getElement().equals("identifier")) {
                        code =  metadatum.getValue();
                    }
                }
                authority = GENERATE + OPENAIRE_PROJECT_AUTHORITY_TYPE + SPLIT + code;
                label = value + "(" + code + ")";
                Map<String, String> extras = getOpenAireExtra(code);
                choiceList.add(new Choice(authority, value, label, extras));
            }
        }
        Choice[] results = new Choice[choiceList.size()];
        results = choiceList.toArray(results);
        return new Choices(results, start, records.size(), Choices.CF_AMBIGUOUS, records.size() > (start + limit), 0);
    }

    private Map<String, String> getOpenAireExtra(String val) {
        Map<String, String> extras = new HashMap<String, String>();
        List<OpenAIREExtraMetadataGenerator> generators = dspace.getServiceManager()
                                                              .getServicesByType(OpenAIREExtraMetadataGenerator.class);
        if (generators != null) {
            for (OpenAIREExtraMetadataGenerator gg : generators) {
                Map<String, String> extrasTmp = gg.build(val);
                extras.putAll(extrasTmp);
            }
        }
        return extras;
    }

    @Override
    public String getPluginInstanceName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPluginInstanceName(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getLabel(String key, String locale) {
        // TODO Auto-generated method stub
        return null;
    }
}