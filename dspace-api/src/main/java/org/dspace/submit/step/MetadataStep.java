/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.submit.lookup.SubmissionLookupDataLoader;

//FIXME move to the ExtractionStep
/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class MetadataStep extends AbstractProcessingStep {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(MetadataStep.class);

    protected List<MetadataListener> listeners = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                      .getServicesByType(MetadataListener.class);

    protected Map<String, List<MetadataValue>> metadataMap = new HashMap<String, List<MetadataValue>>();
    private Map<String, Set<String>> results = new HashMap<String, Set<String>>();
    private Map<String, String> mappingIdentifier = new HashMap<String, String>();

    @Override
    public void doPreProcessing(Context context, InProgressSubmission wsi) {
        for (MetadataListener listener : listeners) {
            for (String metadata : listener.getMetadata().keySet()) {
                String[] tokenized = Utils.tokenize(metadata);
                List<MetadataValue> mm = itemService.getMetadata(wsi.getItem(), tokenized[0], tokenized[1],
                                                                  tokenized[2], Item.ANY);
                if (mm != null && !mm.isEmpty()) {
                    metadataMap.put(metadata, mm);
                } else {
                    metadataMap.put(metadata, new ArrayList<MetadataValue>());
                }
                mappingIdentifier.put(metadata, listener.getMetadata().get(metadata));
            }
        }
    }

    @Override
    public void doPostProcessing(Context context, InProgressSubmission wsi) {
        external:
        for (String metadata : metadataMap.keySet()) {
            String[] tokenized = Utils.tokenize(metadata);
            List<MetadataValue> currents = itemService.getMetadata(wsi.getItem(), tokenized[0], tokenized[1],
                                                                    tokenized[2], Item.ANY);
            if (currents != null && !currents.isEmpty()) {
                List<MetadataValue> olds = metadataMap.get(metadata);
                if (olds.isEmpty()) {
                    process(context, metadata, currents);
                    continue external;
                }
                internal:
                for (MetadataValue current : currents) {

                    boolean found = false;
                    for (MetadataValue old : olds) {
                        if (old.getValue().equals(current.getValue())) {
                            found = true;
                        }
                    }
                    if (!found) {
                        process(context, metadata, current);
                    }
                }
            }
        }

        if (!results.isEmpty()) {
            for (MetadataListener listener : listeners) {
                for (DataLoader dataLoader : listener.getDataloadersMap().values()) {
                    SubmissionLookupDataLoader submissionLookupDataLoader = (SubmissionLookupDataLoader) dataLoader;
                    try {
                        List<Record> recordSet = submissionLookupDataLoader.getByIdentifier(context, results);
                        List<Record> resultSet = convertFields(recordSet, bteBatchImportService.getOutputMap());
                        enrichItem(context, resultSet, wsi.getItem());
                    } catch (HttpException | IOException | SQLException | AuthorizeException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    protected void enrichItem(Context context, List<Record> rset, Item item) throws SQLException, AuthorizeException {
        for (Record record : rset) {
            for (String field : record.getFields()) {
                if (record.getValues(field) != null) {
                    try {
                        String[] tfield = Utils.tokenize(field);
                        List<MetadataValue> mdvs = itemService
                            .getMetadata(item, tfield[0], tfield[1], tfield[2], Item.ANY);
                        if (mdvs == null || mdvs.isEmpty()) {
                            for (Value value : record.getValues(field)) {

                                itemService.addMetadata(context, item, tfield[0], tfield[1], tfield[2], null,
                                                        value.getAsString());
                            }
                        } else {
                            external:
                            for (Value value : record.getValues(field)) {
                                boolean found = false;
                                for (MetadataValue mdv : mdvs) {
                                    if (mdv.getValue().equals(value.getAsString())) {
                                        found = true;
                                        continue external;
                                    }
                                }
                                if (!found) {
                                    itemService.addMetadata(context, item, tfield[0], tfield[1], tfield[2], null,
                                                            value.getAsString());
                                }
                            }
                        }
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
        itemService.update(context, item);

    }

    private void process(Context context, String metadata, List<MetadataValue> currents) {
        for (MetadataValue current : currents) {
            process(context, metadata, current);
        }
    }

    private void process(Context context, String metadata, MetadataValue current) {
        String key = mappingIdentifier.get(metadata);
        Set<String> identifiers = null;
        if (!results.containsKey(key)) {
            identifiers = new HashSet<String>();
        } else {
            identifiers = results.get(key);
        }
        identifiers.add(current.getValue());
        results.put(key, identifiers);
    }

    public List<Record> convertFields(List<Record> recordSet, Map<String, String> fieldMap) {
        List<Record> result = new ArrayList<Record>();
        if (recordSet != null) {
            for (Record publication : recordSet) {
                for (String fieldName : fieldMap.keySet()) {
                    String md = null;
                    if (fieldMap != null) {
                        md = fieldMap.get(fieldName);
                    }

                    if (StringUtils.isBlank(md)) {
                        continue;
                    } else {
                        md = md.trim();
                    }

                    if (publication.isMutable()) {
                        List<Value> values = publication.getValues(md);
                        publication.makeMutable().removeField(md);
                        publication.makeMutable().addField(fieldName, values);
                    }
                }

                result.add(publication);
            }
        }
        return result;
    }
}
