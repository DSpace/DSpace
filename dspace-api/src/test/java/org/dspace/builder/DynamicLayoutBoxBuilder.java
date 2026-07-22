/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.Group;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBox2SecurityGroup;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.DynamicLayoutBoxService;

public class DynamicLayoutBoxBuilder extends AbstractBuilder<DynamicLayoutBox, DynamicLayoutBoxService> {

    private static Logger log = LogManager.getLogger(DynamicLayoutBoxBuilder.class);

    private DynamicLayoutBox box;

    protected DynamicLayoutBoxBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        delete(box);
    }

    public static DynamicLayoutBoxBuilder createBuilder(Context context, EntityType eType, boolean collapsed,
            boolean minor) {
        return createBuilder(context, eType, DynamicLayoutBoxTypes.METADATA.name(), collapsed, minor);
    }

    public static DynamicLayoutBoxBuilder createBuilder(Context context, EntityType eType, String boxType,
            boolean collapsed, boolean minor) {
        DynamicLayoutBoxBuilder builder = new DynamicLayoutBoxBuilder(context);
        return builder.create(context, eType, boxType, collapsed, minor);
    }

    private DynamicLayoutBoxBuilder create(Context context, EntityType eType, String boxType, boolean collapsed,
            boolean minor) {
        try {
            this.context = context;
            this.box = getService().create(context, eType, boxType, collapsed, minor);
        } catch (Exception e) {
            log.error("Error in DynamicLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public DynamicLayoutBox build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, box);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in DynamicLayoutBoxBuilder.build(), error: ", e);
        }
        return box;
    }

    @Override
    public void delete(Context c, DynamicLayoutBox dso) throws Exception {
        if ( dso != null ) {
            getService().delete(c, dso);
        }
    }

    public void delete(DynamicLayoutBox dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            DynamicLayoutBox attachedBox = c.reloadEntity(box);
            if (attachedBox != null) {
                getService().delete(c, attachedBox);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static void delete(Integer id)
            throws SQLException, IOException, SearchServiceException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            DynamicLayoutBox box = dynamicLayoutBoxService.find(c, id);
            if (box != null) {
                try {
                    dynamicLayoutBoxService.delete(c, box);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

    @Override
    protected DynamicLayoutBoxService getService() {
        return dynamicLayoutBoxService;
    }

    public DynamicLayoutBoxBuilder withShortname(String shortname) {
        this.box.setShortname(shortname);
        return this;
    }

    public DynamicLayoutBoxBuilder withHeader(String header) {
        this.box.setHeader(header);
        return this;
    }

    public DynamicLayoutBoxBuilder withSecurity(LayoutSecurity security) {
        this.box.setSecurity(security);
        return this;
    }

    public DynamicLayoutBoxBuilder withStyle(String style) {
        this.box.setStyle(style);
        return this;
    }

    public DynamicLayoutBoxBuilder addField(DynamicLayoutField field) {
        this.box.addLayoutField(field);
        return this;
    }

    public DynamicLayoutBoxBuilder withMetadataSecurityField(Set<MetadataField> fields) {
        this.box.setMetadataSecurityFields(fields);
        return this;
    }

    public DynamicLayoutBoxBuilder withMaxColumns(Integer maxColumns) {
        this.box.setMaxColumns(maxColumns);
        return this;
    }
    public DynamicLayoutBoxBuilder withType(String type) {
        this.box.setType(type);
        return this;
    }
    public DynamicLayoutBoxBuilder addMetadataSecurityField(MetadataField field) {
        if (this.box.getMetadataSecurityFields() == null) {
            this.box.setMetadataSecurityFields(new HashSet<>());
        }
        this.box.getMetadataSecurityFields().add(field);
        return this;
    }

    public DynamicLayoutBoxBuilder addBox2SecurityGroups(Group group, DynamicLayoutBox alternativeBox)
            throws SQLException {
        if (this.box.getBox2SecurityGroups() == null) {
            this.box.setBox2SecurityGroups(new HashSet<>());
        }
        this.box.getBox2SecurityGroups().add(
            new DynamicLayoutBox2SecurityGroup(
                new DynamicLayoutBox2SecurityGroup.DynamicLayoutBox2SecurityGroupId(box, group),
                box, group, alternativeBox)
        );
        return this;
    }

    public DynamicLayoutBoxBuilder withContainer(boolean container) {
        this.box.setContainer(container);
        return this;
    }
}
