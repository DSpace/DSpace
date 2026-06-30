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
import java.util.List;
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
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2SecurityGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutTabService;

public class CrisLayoutTabBuilder extends AbstractBuilder<CrisLayoutTab, CrisLayoutTabService> {

    private static Logger log = LogManager.getLogger(CrisLayoutTabBuilder.class);

    private CrisLayoutTab tab;

    private CrisLayoutTabBuilder(Context context) {
        super(context);
    }

    @Override
    protected CrisLayoutTabService getService() {
        return crisLayoutTabService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(tab);
    }

    public static CrisLayoutTabBuilder createTab(Context context, EntityType eType, Integer priority) {
        CrisLayoutTabBuilder builder = new CrisLayoutTabBuilder(context);
        return builder.create(context, eType, priority);
    }

    private CrisLayoutTabBuilder create(Context context, EntityType eType, Integer priority) {
        try {
            this.context = context;
            this.tab = getService().create(context, eType, priority);
        } catch (Exception e) {
            log.error("Error in CrisLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public CrisLayoutTab build() {
        try {
            getService().update(context, tab);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in CrisLayoutTabBuilder.build(), error: ", e);
        }
        return tab;
    }

    @Override
    public void delete(Context c, CrisLayoutTab tab) throws Exception {
        if (tab != null) {
            getService().delete(c, tab);
        }
    }

    public void delete(CrisLayoutTab tab) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisLayoutTab attachedTab = c.reloadEntity(tab);
            if (attachedTab != null) {
                getService().delete(c, attachedTab);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static void delete(Integer id)
            throws SQLException, IOException, SearchServiceException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisLayoutTab tab = crisLayoutTabService.find(c, id);
            if (tab != null) {
                try {
                    crisLayoutTabService.delete(c, tab);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

    public CrisLayoutTabBuilder withShortName(String shortName) {
        this.tab.setShortName(shortName);
        return this;
    }

    public CrisLayoutTabBuilder withCustomFilter(String customFilter) {
        this.tab.setCustomFilter(customFilter);
        return this;
    }

    public CrisLayoutTabBuilder withHeader(String header) {
        this.tab.setHeader(header);
        return this;
    }

    public CrisLayoutTabBuilder withSecurity(LayoutSecurity security) {
        this.tab.setSecurity(security);
        return this;
    }

    public CrisLayoutTabBuilder withLeading(boolean leading) {
        this.tab.setLeading(leading);
        return this;
    }

    public CrisLayoutTabBuilder addBoxIntoNewRow(CrisLayoutBox box) {
        return addBoxIntoNewRow(box, null, null);
    }

    public CrisLayoutTabBuilder addBoxIntoNewRow(CrisLayoutBox box, String rowStyle, String cellStyle) {

        CrisLayoutRow newRow = new CrisLayoutRow();
        newRow.setStyle(rowStyle);

        CrisLayoutCell newCell = new CrisLayoutCell();
        newCell.setStyle(cellStyle);

        newCell.addBox(box);
        newRow.addCell(newCell);
        this.tab.addRow(newRow);

        return this;
    }

    public CrisLayoutTabBuilder addBoxIntoLastRow(CrisLayoutBox box) {
        return addBoxIntoLastRow(box, null);
    }

    public CrisLayoutTabBuilder addBoxIntoLastRow(CrisLayoutBox box, String cellStyle) {
        List<CrisLayoutRow> rows = this.tab.getRows();
        if (rows.isEmpty()) {
            return addBoxIntoNewRow(box);
        }

        CrisLayoutRow row = rows.get(rows.size() - 1);
        CrisLayoutCell newCell = new CrisLayoutCell();
        newCell.setStyle(cellStyle);

        newCell.addBox(box);
        row.addCell(newCell);

        return this;
    }

    public CrisLayoutTabBuilder addBoxIntoLastCell(CrisLayoutBox box) {
        List<CrisLayoutRow> rows = this.tab.getRows();
        if (rows.isEmpty()) {
            return addBoxIntoNewRow(box);
        }

        CrisLayoutRow row = rows.get(rows.size() - 1);
        List<CrisLayoutCell> cells = row.getCells();
        if (cells.isEmpty()) {
            CrisLayoutCell cell = new CrisLayoutCell();
            row.addCell(cell);
            cell.addBox(box);
        } else {
            cells.get(cells.size() - 1).addBox(box);
        }

        return this;
    }

    public CrisLayoutTabBuilder withMetadatasecurity(Set<MetadataField> metadataFields) {
        this.tab.setMetadataSecurityFields(metadataFields);
        return this;
    }

    public CrisLayoutTabBuilder addMetadatasecurity(MetadataField metadataField) {
        if (this.tab.getMetadataSecurityFields() == null) {
            this.tab.setMetadataSecurityFields(new HashSet<>());
        }
        this.tab.getMetadataSecurityFields().add(metadataField);
        return this;
    }

    public CrisLayoutTabBuilder addTab2SecurityGroups(Group group, CrisLayoutTab alternativeTab) {
        if (this.tab.getTab2SecurityGroups() == null) {
            this.tab.setTab2SecurityGroups(new HashSet<>());
        }
        this.tab.getTab2SecurityGroups().add(
            new CrisLayoutTab2SecurityGroup(new CrisLayoutTab2SecurityGroup.CrisLayoutTab2SecurityGroupId(tab, group),
                tab, group, alternativeTab)
        );
        return this;
    }
}
