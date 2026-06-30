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
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox2SecurityGroup;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;

public class CrisLayoutBoxBuilder extends AbstractBuilder<CrisLayoutBox, CrisLayoutBoxService> {

    private static Logger log = LogManager.getLogger(CrisLayoutBoxBuilder.class);

    private CrisLayoutBox box;

    protected CrisLayoutBoxBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        delete(box);
    }

    public static CrisLayoutBoxBuilder createBuilder(Context context, EntityType eType, boolean collapsed,
            boolean minor) {
        return createBuilder(context, eType, CrisLayoutBoxTypes.METADATA.name(), collapsed, minor);
    }

    public static CrisLayoutBoxBuilder createBuilder(Context context, EntityType eType, String boxType,
            boolean collapsed, boolean minor) {
        CrisLayoutBoxBuilder builder = new CrisLayoutBoxBuilder(context);
        return builder.create(context, eType, boxType, collapsed, minor);
    }

    private CrisLayoutBoxBuilder create(Context context, EntityType eType, String boxType, boolean collapsed,
            boolean minor) {
        try {
            this.context = context;
            this.box = getService().create(context, eType, boxType, collapsed, minor);
        } catch (Exception e) {
            log.error("Error in CrisLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public CrisLayoutBox build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, box);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in CrisLayoutBoxBuilder.build(), error: ", e);
        }
        return box;
    }

    @Override
    public void delete(Context c, CrisLayoutBox dso) throws Exception {
        if ( dso != null ) {
            getService().delete(c, dso);
        }
    }

    public void delete(CrisLayoutBox dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisLayoutBox attachedBox = c.reloadEntity(box);
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
            CrisLayoutBox box = crisLayoutBoxService.find(c, id);
            if (box != null) {
                try {
                    crisLayoutBoxService.delete(c, box);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

    @Override
    protected CrisLayoutBoxService getService() {
        return crisLayoutBoxService;
    }

    public CrisLayoutBoxBuilder withShortname(String shortname) {
        this.box.setShortname(shortname);
        return this;
    }

    public CrisLayoutBoxBuilder withHeader(String header) {
        this.box.setHeader(header);
        return this;
    }

    public CrisLayoutBoxBuilder withSecurity(LayoutSecurity security) {
        this.box.setSecurity(security);
        return this;
    }

    public CrisLayoutBoxBuilder withStyle(String style) {
        this.box.setStyle(style);
        return this;
    }

    public CrisLayoutBoxBuilder addField(CrisLayoutField field) {
        this.box.addLayoutField(field);
        return this;
    }

    public CrisLayoutBoxBuilder withMetadataSecurityField(Set<MetadataField> fields) {
        this.box.setMetadataSecurityFields(fields);
        return this;
    }

    public CrisLayoutBoxBuilder withMaxColumns(Integer maxColumns) {
        this.box.setMaxColumns(maxColumns);
        return this;
    }
    public CrisLayoutBoxBuilder withType(String type) {
        this.box.setType(type);
        return this;
    }
    public CrisLayoutBoxBuilder addMetadataSecurityField(MetadataField field) {
        if (this.box.getMetadataSecurityFields() == null) {
            this.box.setMetadataSecurityFields(new HashSet<>());
        }
        this.box.getMetadataSecurityFields().add(field);
        return this;
    }

    public CrisLayoutBoxBuilder addBox2SecurityGroups(Group group, CrisLayoutBox alternativeBox) throws SQLException {
        if (this.box.getBox2SecurityGroups() == null) {
            this.box.setBox2SecurityGroups(new HashSet<>());
        }
        this.box.getBox2SecurityGroups().add(
            new CrisLayoutBox2SecurityGroup(new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box, group),
                box, group, alternativeBox)
        );
        return this;
    }

    public CrisLayoutBoxBuilder withContainer(boolean container) {
        this.box.setContainer(container);
        return this;
    }
}
