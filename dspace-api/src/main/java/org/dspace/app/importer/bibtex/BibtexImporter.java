package org.dspace.app.importer.bibtex;


import java.io.StringReader;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.dspace.app.importer.AConfigurableImporter;
import org.dspace.app.importer.SingleImportResultBean;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexNode;
import bibtex.dom.BibtexPerson;
import bibtex.dom.BibtexPersonList;
import bibtex.dom.BibtexString;
import bibtex.expansions.MacroReferenceExpander;
import bibtex.expansions.PersonListExpander;
import bibtex.parser.BibtexParser;

/**
 * This plugin cannot be used as singleton i.e reusable = false
 * as it keeps state information
 *
 * @author bollini
 *
 */
public class BibtexImporter extends AConfigurableImporter<BibtexEntry>
{
    // private Pattern[] patterns = new Pattern[]{
    // Pattern.compile("((\\w*),\\s*(\\w*)\\.?){\\s*and\\s*}?")
    // };
    // cognome nome, cognome nome, ...
    // cognome nome and cognome nome and cognome nome and ... -------------> WOS
    // cognome, n. and cognome, n. and cognome, n. and ... -------------> WOS
    // nome cognome and nome cognome and.... ------------> ????
    // cognome, n., cognome, n. --------> scopus
    private List<BibtexEntry> bibtexEntries = null;

    private Context context = null;

    protected int getTotal(String data)
    {
        try
        {
            List<BibtexEntry> entries = getBibtexEntries(data);
            return entries.size();
        }
        catch (Exception e1)
        {
            return -1;
        }
    }

    protected List<SingleImportResultBean> processData(String data,  Community community, Collection collection,
            EPerson eperson)
    {
        List<SingleImportResultBean> results = new LinkedList<SingleImportResultBean>();
        Context context = null;

        List<BibtexEntry> entries = null;
        try
        {
            entries = getBibtexEntries(data);
        }
        catch (Exception e1)
        {
            SingleImportResultBean result = new SingleImportResultBean(
                    SingleImportResultBean.ERROR, -1, e1.getMessage(), "", data);
            results.add(result);
            return results;
        }

        for (BibtexEntry entry : entries)
        {
            try
            {
                context = getContext(eperson);

                Collection targetCollection = getCollection(context, community, collection, entry);
                WorkspaceItem witem = WorkspaceItem.create(context,
                        targetCollection, true);
                fitMetatadata(witem, entry);
                extractMetadata(context, witem, getTargetCollectionFormName(entry));
                removeInvalidMetadata(context, witem, getTargetCollectionFormName(entry));
                witem.update();
                context.commit();
                SingleImportResultBean result = new SingleImportResultBean(
                        SingleImportResultBean.SUCCESS, witem.getID(),
                        "Tipologia assegnata: " + targetCollection.getName(),
                        entry.getEntryKey(), entry.toString());
                results.add(result);

            }
            catch (Exception e)
            {
                SingleImportResultBean result = new SingleImportResultBean(
                        SingleImportResultBean.ERROR, -1, e.getMessage(), entry
                                .getEntryKey(), entry.toString());
                results.add(result);
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
        }
        if (context != null && context.isValid())
        {
            context.abort();
        }
        return results;
    }

    protected String getType(BibtexEntry crossitem)
    {
        String bibtexType = crossitem.getEntryType().toLowerCase();
        if ("book".equalsIgnoreCase(bibtexType) && crossitem.getFieldValue("editor") != null)
        {
            bibtexType = "bookeditor";
        }
        return bibtexType;
    }

    private void fitMetatadata(WorkspaceItem witem, BibtexEntry entry)
    {
        // String[] authorFields = getImporterConfiguration().getProperty(
        // "personalname-field", "author")
        // .split("\\s*,\\s*");
        for (String field : (Set<String>) entry.getFields().keySet())
        {
            BibtexAbstractValue values = entry.getFieldValue(field);
            String bibtexValue = "";
            if (values instanceof BibtexString)
            {
                BibtexString stringValue = (BibtexString) values;
                bibtexValue = stringValue.getContent();

                if (field.equalsIgnoreCase("author")
                        || field.equalsIgnoreCase("editor"))
                {
                    // vuol dire che � stato impossibile parserizzare i singoli
                    // autori
                    field = "all" + field + "s";
                }
                addMetadata(witem.getItem(), field.toLowerCase(),
                        getTargetCollectionFormName(entry), bibtexValue
                                .replaceAll("\\{([^\\}]*[^\\\\])\\}", "$1"));
            }
            else if (values instanceof BibtexPersonList)
            {
                BibtexPersonList personList = (BibtexPersonList) values;
                for (BibtexPerson person : (List<BibtexPerson>) personList
                        .getList())
                {
                    addMetadata(witem.getItem(), field.toLowerCase(),
                            getTargetCollectionFormName(entry), getName(person)
                                    .replaceAll("\\{([^\\}]*[^\\\\])\\}", "$1"));
                }
            }
            else
            {
                throw new RuntimeException("Campo " + field + " sconosciuto");
            }

        }
    }

    protected String getName(BibtexPerson person)
    {
        return (person.getLast() + ", " + person.getFirst());
    }

    private Context getContext(EPerson eperson) throws SQLException
    {
        if (context != null && context.isValid())
        {
            return context;
        }

        context = new Context();
        context.setCurrentUser(eperson);
        int[] specialGroups = AuthenticationManager.getSpecialGroups(context,
                null);
        for (int groupid : specialGroups)
        {
            context.setSpecialGroup(groupid);
        }
        return context;
    }

    private List<BibtexEntry> getBibtexEntries(String data)
    {
        if (bibtexEntries != null)
        {
            return bibtexEntries;
        }

        StringReader reader = new StringReader(data);
        BibtexParser parser = new BibtexParser(false);
        BibtexFile bib = new BibtexFile();
        try
        {
            parser.parse(bib, reader);
            MacroReferenceExpander mre = new MacroReferenceExpander(true, true,
                    false, false);
            mre.expand(bib);

            if (ConfigurationManager.getBooleanProperty("importer.bibtex.expandnames", true))
            {
                PersonListExpander ple = new PersonListExpander(true, true, false);
                ple.expand(bib);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                    "Errore durante la parserizzazione dei dati forniti "
                            + (e.getMessage() != null ? ":" + e.getMessage()
                                    : ""));
        }

        bibtexEntries = new LinkedList<BibtexEntry>();

        if (bib.getEntries() != null)
        {
            for (BibtexNode node : (List<BibtexNode>) bib.getEntries())
            {
                if (node instanceof BibtexEntry)
                {
                    bibtexEntries.add((BibtexEntry) node);
                }
            }
        }
        return bibtexEntries;
    }
}