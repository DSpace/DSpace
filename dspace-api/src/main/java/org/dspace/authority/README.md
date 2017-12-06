# How to reuse this functionality for other metadata fields.
Let's say dc.relation.ispartofseries is a *onebox* input field labeled 'Journals' in the submission and needs to have an authority look-up just like dc.contributor.author.

Additionally the journal document should contain:
* journal title: mandatory, not repeatable
* ISSN: optional, repeatable
* Publisher: optional, not repeatable
* an internal ID

## Add the authority controlled metadata field to dspace.cfg
```
choices.plugin.dc.relation.ispartofseries = SolrAuthorAuthority
choices.presentation.dc.relation.ispartofseries = lookup
authority.controlled.dc.relation.ispartofseries = true

authority.author.indexer.field.1=dc.contributor.author
authority.author.indexer.field.2=dc.relation.ispartofseries
```

## Add the desired properties of the new authority type in the solr schema
solr/authority/conf/schema.xml
```
     <!-- journal-->
     <field name="issn" type="string" multiValued="true" indexed="true" stored="true" required="false"/>
     <field name="publisher" type="string" multiValued="false" indexed="true" stored="true" required="false"/>
 </fields>
```
The title and the internal ID find their places in the already existing "value" and "id" fields.

## Extend org.dspace.authority.AuthorityValue, add the fields and implement the methods
```
public class JournalAuthorityValue extends AuthorityValue {

    protected String publisher;
    protected List<String> ISSN = new ArrayList<String>();
```
Since the journal title is to be stored as the record's value no specific instance variable is needed, **AuthorityValue** already provides this. The internal ID is also taken care of in the superclass.

Override **getSolrInputDocument()** and **setValues(SolrDocument document)** to control what is stored in the solr document.

```
    @Override
    public SolrInputDocument getSolrInputDocument() {
        SolrInputDocument doc = super.getSolrInputDocument();
        doc.addField("publisher", getPublisher());
        for (String issn : ISSN) {
            doc.addField("ISSN", issn);
        }
        return doc;
    }

    @Override
    public void setValues(SolrDocument document) {
        super.setValues(document);
        Object publisher = document.getFieldValue("publisher");
        if (publisher != null) {
            setPublisher(publisher.toString());
        }
        Collection<Object> issns = document.getFieldValues("ISSN");
        if (issns != null) {
            for (Object issn : issns) {
                addISSN(issn.toString());
            }
        }
    }
```

Override **choiceSelectMap()** to control what will be displayed in the lookup UI
```
    @Override
    public Map<String, String> choiceSelectMap() {
        Map<String, String> map = super.choiceSelectMap();
        if (StringUtils.isNotBlank(getValue())) {
            map.put("Title", getValue());
        }
        String issn = "";
        for (String s : ISSN) {
            if (StringUtils.isNotBlank(s)) {
                issn += s + " ";
            }
        }
        if (StringUtils.isNotBlank(issn)) {
            map.put("issn", issn.trim());
        }
        if (StringUtils.isBlank(publisher)) {
            map.put("publisher", publisher);
        }
        return map;
    }
```
Override **getAuthorityType()**, **generateString()** and **newInstance(String info)** and make sure they are consistent.

* **getAuthorityType()** The authority type is an implicit field in the solr document and is necessary to cast the solr document into the correct java class.
* **generateString()** is a temporary value for the metadata's authority that will be handed to the authority consumer. This is only used when an external authority is chosen that has not yet been added to the solr cache. It needs to contain enough information to make an inambiguous external lookup, e.g. some sort of id.
* **newInstance(String info)** will use the information to make the actual lookup and fill in all the information. In case there is no external source, this is of no matter.
You may always look at the OrcidAuthorityValue class for an example!

```
    @Override
    public String getAuthorityType() {
        return "journal";
    }

    @Override
    public String generateString() {
        return AuthorityValueGenerator.GENERATE // The trigger for the authority consumer to generate a new entry in the solr cache.
                + getAuthorityType() // So this class will be used to create a new instance
                + AuthorityValueGenerator.SPLIT
                + getValue(); // This will be the value of the "info" parameter in public AuthorityValue newInstance(String info)
    }

    @Override
    public AuthorityValue newInstance(String info) {
        JournalAuthorityValue authorityValue = JournalAuthorityValue.create();
        authorityValue.setValue(info);
        // no external retrieval of information for this authority
        return authorityValue;
    }
```

Override **hasTheSameInformationAs(Object o)** and include only the sensible fields. The use case for this method is an update from the external information source. When comparing a value before and after the update and returning false, the last-modified-date will be updated.

## Add the new class to the spring configuration
config/spring/api/authority-services.xml

```
    <bean name="AuthorityTypes" class="org.dspace.authority.AuthorityTypes">
        <property name="types">
            <list>
                <bean class="org.dspace.authority.journal.JournalAuthorityValue"/>
                <bean class="org.dspace.authority.orcid.OrcidAuthorityValue"/>
                <bean class="org.dspace.authority.PersonAuthorityValue"/>
            </list>
        </property>
        <property name="fieldDefaults">
            <map>
                <entry key="dc_contributor_author">
                    <bean class="org.dspace.authority.PersonAuthorityValue"/>
                </entry>
                <entry key="dc_relation_ispartofseries">
                    <bean class="org.dspace.authority.journal.JournalAuthorityValue"/>
                </entry>
            </map>
        </property>
    </bean>
```
The **types** property contains all active authority types. When casting a solr document into the correct java class, only the classes in this list will be candidates. Only when none of the listed classes have a matching **getAuthorityType()** the superclass AuthorityValue will be used.

The **fieldDefaults** indicate which authority type to create when adding a new metadata value that is not brought forth by the external lookup.
