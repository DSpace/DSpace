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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.factory.MetadataFieldIndexFactory;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving {@link org.dspace.content.MetadataField} items in the search core
 *
 * @author Maria Verdonck (Atmire) on 14/07/2020
 */
public class MetadataFieldIndexFactoryImpl extends IndexFactoryImpl<IndexableMetadataField, MetadataField>
    implements MetadataFieldIndexFactory {

    public static final String SCHEMA_FIELD_NAME = "schema";
    public static final String ELEMENT_FIELD_NAME = "element";
    public static final String QUALIFIER_FIELD_NAME = "qualifier";
    public static final String FIELD_NAME_VARIATIONS = "fieldName";

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableMetadataField indexableObject) throws SQLException,
        IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final MetadataField metadataField = indexableObject.getIndexedObject();
        // add schema, element, qualifier and full fieldName
        addFacetIndex(doc, SCHEMA_FIELD_NAME, metadataField.getMetadataSchema().getName(),
            metadataField.getMetadataSchema().getName());
        addFacetIndex(doc, ELEMENT_FIELD_NAME, metadataField.getElement(), metadataField.getElement());
        String fieldName = metadataField.toString().replace('_', '.');
        addFacetIndex(doc, FIELD_NAME_VARIATIONS, fieldName, fieldName);
        if (StringUtils.isNotBlank(metadataField.getQualifier())) {
            addFacetIndex(doc, QUALIFIER_FIELD_NAME, metadataField.getQualifier(), metadataField.getQualifier());
            addFacetIndex(doc, FIELD_NAME_VARIATIONS, fieldName,
                metadataField.getElement() + "." + metadataField.getQualifier());
            addFacetIndex(doc, FIELD_NAME_VARIATIONS, metadataField.getQualifier(), metadataField.getQualifier());
        } else {
            addFacetIndex(doc, FIELD_NAME_VARIATIONS, metadataField.getElement(), metadataField.getElement());
        }
        addNamedResourceTypeIndex(doc, indexableObject.getTypeText());
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        // add read permission on doc for anonymous group
        doc.addField("read", "g" + anonymousGroup.getID());
        doc.addField(FIELD_NAME_VARIATIONS + "_sort", fieldName);
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
