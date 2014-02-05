/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;

import com.lyncode.xoai.builders.dataprovider.ElementBuilder;
import com.lyncode.xoai.builders.dataprovider.MetadataBuilder;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;

public class ItemRepositoryBuilder {
    private SolrServer solrServer;

    public ItemRepositoryBuilder(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    public ItemRepositoryBuilder withItem (DSpaceItemBuilder builder) {
        try {
            solrServer.add(index(builder));
            solrServer.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    private SolrInputDocument index(DSpaceItemBuilder item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        SolrInputDocument doc = new SolrInputDocument();

        doc.addField("item.id", item.getId());
        doc.addField("item.public", item.isPublic());
        doc.addField("item.lastmodified", item.getLastModifiedDate());
        doc.addField("item.submitter", item.getSubmitter());
        doc.addField("item.handle", item.getHandle());
        doc.addField("item.deleted", item.isDeleted());

        for (String col : item.getCollections())
            doc.addField("item.collections", col);

        for (String col : item.getCommunities())
            doc.addField("item.communities", col);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext context = XmlOutputContext.emptyContext(out, Second);
        item.getMetadata().write(context);
        context.getWriter().flush();
        context.getWriter().close();
        doc.addField("item.compile", out.toString());

        return doc;
    }

    public static class DSpaceItemBuilder {
        private List<String> collections = new ArrayList<String>();
        private List<String> communities = new ArrayList<String>();
        private MetadataBuilder metadataBuilder = new MetadataBuilder();
        private String handle;
        private int id;
        private String submitter;
        private Date lastModifiedDate;
        private boolean deleted;
        private boolean aPublic = true;


        public DSpaceItemBuilder withLastModifiedDate (Date lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public DSpaceItemBuilder withCollection (String colName) {
            collections.add(colName);
            return this;
        }
        public DSpaceItemBuilder withCommunity (String comName) {
            communities.add(comName);
            return this;
        }
        public DSpaceItemBuilder whichSsPublic () {
            aPublic = true;
            return this;
        }
        public DSpaceItemBuilder whichSsPrivate () {
            aPublic = false;
            return this;
        }

        public DSpaceItemBuilder whichIsDeleted () {
            this.deleted = true;
            return this;
        }
        public DSpaceItemBuilder whichIsNotDeleted () {
            this.deleted = false;
            return this;
        }

        public DSpaceItemBuilder withMetadata (String schema, String element, String value) {
            metadataBuilder.withElement(new ElementBuilder().withName(schema).withField(element, value).build());
            return this;
        }

        public String getHandle() {
            return handle;
        }

        public DSpaceItemBuilder withHandle (String handle) {
            this.handle = handle;
            return this;
        }

        public DSpaceItemBuilder withSubmitter (String submitter) {
            this.submitter = submitter;
            return this;
        }

        public DSpaceItemBuilder withId (int id) {
            this.id = id;
            return this;
        }

        public int getId() {
            return id;
        }

        public String getSubmitter() {
            return submitter;
        }

        public Date getLastModifiedDate() {
            return lastModifiedDate;
        }

        public List<String> getCollections() {
            return collections;
        }

        public List<String> getCommunities() {
            return communities;
        }

        public Metadata getMetadata() {
            return metadataBuilder.build();
        }

        public boolean isDeleted() {
            return deleted;
        }

        public boolean isPublic() {
            return aPublic;
        }
    }
}
