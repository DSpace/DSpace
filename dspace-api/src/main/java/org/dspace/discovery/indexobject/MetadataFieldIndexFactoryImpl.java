/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.factory.MetadataFieldIndexFactory;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving {@link org.dspace.content.MetadataField} items in the search core
 *
 * @author Maria Verdonck (Atmire) on 14/07/2020
 */
public class MetadataFieldIndexFactoryImpl extends IndexFactoryImpl<IndexableMetadataField, MetadataField>
    implements MetadataFieldIndexFactory {

    @Autowired
    protected GroupService groupService;

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableMetadataField indexableObject) throws SQLException,
        IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final MetadataField metadataField = indexableObject.getIndexedObject();
        // add schema, element, qualifier and full fieldName
        addFacetIndex(doc, "schema", metadataField.getMetadataSchema().getName(),
            metadataField.getMetadataSchema().getName());
        addFacetIndex(doc, "element", metadataField.getElement(), metadataField.getElement());
        if (StringUtils.isNotBlank(metadataField.getQualifier())) {
            addFacetIndex(doc, "qualifier", metadataField.getQualifier(), metadataField.getQualifier());
        }
        String fieldName = metadataField.toString().replace('_', '.');
        addFacetIndex(doc, "fieldName", fieldName, fieldName);
        addNamedResourceTypeIndex(doc, indexableObject.getTypeText());
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        // add read permission on doc for anonymous group
        doc.addField("read", "g" + anonymousGroup.getID());
        return doc;
    }

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public Iterator<IndexableMetadataField> findAll(Context context) throws SQLException {
        final Iterator<MetadataField> metadataFields = metadataFieldService.findAll(context).iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return metadataFields.hasNext();
            }

            @Override
            public IndexableMetadataField next() {
                return new IndexableMetadataField(metadataFields.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableMetadataField.TYPE;
    }

    @Override
    public Optional<IndexableMetadataField> findIndexableObject(Context context, String id) throws SQLException {
        final MetadataField metadataField = metadataFieldService.find(context, Integer.parseInt(id));
        return metadataField == null ? Optional.empty() : Optional.of(new IndexableMetadataField(metadataField));
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof MetadataField;
    }

    @Override
    public List getIndexableObjects(Context context, MetadataField object) {
        return Arrays.asList(new IndexableMetadataField(object));
    }
}
