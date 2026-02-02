/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.suggestion.MockSuggestionExternalDataSource;
import org.dspace.app.suggestion.SolrSuggestionStorageService;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Builder to construct Item objects
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionTargetBuilder extends AbstractBuilder<SuggestionTarget, SolrSuggestionStorageService> {
    public final static String EVIDENCE_MOCK_NAME = "MockEvidence";
    public final static String EVIDENCE_MOCK_NOTE = "Generated for testing purpose...";
    private Item item;
    private SuggestionTarget target;
    private List<Suggestion> suggestions;
    private String source;
    private int total;

    protected SuggestionTargetBuilder(Context context) {
        super(context);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Collection col, final String name) {
        return createTarget(context, col, name, null);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Collection col, final String name,
            final EPerson eperson) {
        SuggestionTargetBuilder builder = new SuggestionTargetBuilder(context);
        return builder.create(context, col, name, eperson);
    }

    public static SuggestionTargetBuilder createTarget(final Context context, final Item item) {
        SuggestionTargetBuilder builder = new SuggestionTargetBuilder(context);
        return builder.create(context, item);
    }

    private SuggestionTargetBuilder create(final Context context, final Collection col, final String name) {
        return create(context, col, name, null);
    }

    private SuggestionTargetBuilder create(final Context context, final Collection col, final String name,
            final EPerson eperson) {
        this.context = context;

        try {
            ItemBuilder itemBuilder = ItemBuilder.createItem(context, col).withTitle(name);
            if (eperson != null) {
                itemBuilder = itemBuilder.withDSpaceObjectOwner(eperson.getFullName(), eperson.getID().toString());
            }
            item = itemBuilder.build();
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    private SuggestionTargetBuilder create(final Context context, final Item item) {
        this.context = context;
        this.item = item;
        return this;
    }

    public SuggestionTargetBuilder withSuggestionCount(final String source, final int total) {
        this.source = source;
        this.total = total;
        return this;
    }

    @Override
    public SuggestionTarget build() {
        target = new SuggestionTarget(item);
        target.setTotal(total);
        target.setSource(source);
        suggestions = generateAllSuggestion();
        try {
            for (Suggestion s : suggestions) {
                solrSuggestionService.addSuggestion(s, false, false);
            }
            solrSuggestionService.commit();
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return target;
    }

    @Override
    public void cleanup() throws Exception {
        solrSuggestionService.deleteTarget(target);
    }

    @Override
    protected SolrSuggestionStorageService getService() {
        return solrSuggestionService;
    }

    @Override
    public void delete(Context c, SuggestionTarget dso) throws Exception {
        solrSuggestionService.deleteTarget(dso);
    }

    private List<Suggestion> generateAllSuggestion() {
        List<Suggestion> allSuggestions = new ArrayList<Suggestion>();
        for (int idx = 0; idx < target.getTotal(); idx++) {
            String idPartStr = String.valueOf(idx + 1);
            Suggestion sug = new Suggestion(source, item, idPartStr);
            sug.setDisplay("Suggestion " + source + " " + idPartStr);
            MetadataValueDTO mTitle = new MetadataValueDTO();
            mTitle.setSchema("dc");
            mTitle.setElement("title");
            mTitle.setValue("Title Suggestion " + idPartStr);

            MetadataValueDTO mSource1 = new MetadataValueDTO();
            mSource1.setSchema("dc");
            mSource1.setElement("source");
            mSource1.setValue("Source 1");

            MetadataValueDTO mSource2 = new MetadataValueDTO();
            mSource2.setSchema("dc");
            mSource2.setElement("source");
            mSource2.setValue("Source 2");

            sug.getMetadata().add(mTitle);
            sug.getMetadata().add(mSource1);
            sug.getMetadata().add(mSource2);

            sug.setExternalSourceUri(
                    "http://localhost/api/integration/externalsources/" + MockSuggestionExternalDataSource.NAME
                            + "/entryValues/" + idPartStr);
            sug.getEvidences().add(new SuggestionEvidence(EVIDENCE_MOCK_NAME,
                    idx % 2 == 0 ? 100 - idx : (double) idx / 2, EVIDENCE_MOCK_NOTE));
            allSuggestions.add(sug);
        }
        return allSuggestions;
    }

}
