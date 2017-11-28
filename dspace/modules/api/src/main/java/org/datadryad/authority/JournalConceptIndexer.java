package org.datadryad.authority;

import org.datadryad.api.DryadJournalConcept;
import org.datadryad.api.DryadOrganizationConcept;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.content.*;
import org.dspace.core.Context;
import java.util.*;
import org.dspace.JournalUtils;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 1, 2011
 * Time: 2:42:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class JournalConceptIndexer implements AuthorityIndexerInterface {

    protected AuthorityValue nextValue;

    protected LinkedList<AuthorityValue> authorities = null;
    private static Boolean journals_cached = false;

    public void init() {
        if (authorities == null) {
            authorities = new LinkedList<AuthorityValue>();
        }
        if (!journals_cached) {
            DryadJournalConcept[] dryadJournalConcepts = JournalUtils.getAllJournalConcepts();
            for (DryadJournalConcept concept : dryadJournalConcepts) {
                if (concept.isAccepted()) {
                    authorities.addAll(createAuthorityValues(concept));
                }
            }
            journals_cached = true;
        }
    }

    @Override
    public void init(Context context, Item item) {
        init();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void init(Context context) {
        init();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AuthorityValue nextValue() {
        return nextValue;
    }

    @Override
    public boolean hasMore() {
        nextValue = authorities.poll();
        return nextValue != null;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<AuthorityValue> createAuthorityValues(DryadOrganizationConcept concept) {
        ArrayList<AuthorityValue> authorityValues = new ArrayList<AuthorityValue>();
        ArrayList<String> names = new ArrayList<String>();
        names.add(concept.getFullName());
//        for (String name : concept.getAlternateNames()) {
//            names.add(name);
//        }
        for (String name : names) {
            AuthorityValue authorityValue = new AuthorityValue();
            authorityValue.setId(String.valueOf(concept.getConceptID()));
            authorityValue.setSource(getSource());
            authorityValue.setField(getField());
            authorityValue.setValue(concept.getFullName());
            // full-text field is for searching, so index it with no spaces.
            authorityValue.setFullText(name.replaceAll("\\s", ""));
            authorityValue.setCreationDate(new Date());
            authorityValue.setLastModified(new Date());
            authorityValues.add(authorityValue);
        }
        return authorityValues;
    }

    public String indexerName() {
        return this.getClass().getName();
    }

    public String getSource() {
        return "JOURNALCONCEPTS";
    }

    public String getField() {
        return "prism_publicationName";
    }
}



