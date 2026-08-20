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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutCell;
import org.dspace.layout.DynamicLayoutRow;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.DynamicLayoutTab2SecurityGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.DynamicLayoutTabService;

public class DynamicLayoutTabBuilder extends AbstractBuilder<DynamicLayoutTab, DynamicLayoutTabService> {

    private static Logger log = LogManager.getLogger(DynamicLayoutTabBuilder.class);

    private DynamicLayoutTab tab;

    private DynamicLayoutTabBuilder(Context context) {
        super(context);
    }

    @Override
    protected DynamicLayoutTabService getService() {
        return dynamicLayoutTabService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(tab);
    }

    public static DynamicLayoutTabBuilder createTab(Context context, EntityType eType, Integer priority) {
        DynamicLayoutTabBuilder builder = new DynamicLayoutTabBuilder(context);
        return builder.create(context, eType, priority);
    }

    private DynamicLayoutTabBuilder create(Context context, EntityType eType, Integer priority) {
        try {
            this.context = context;
            this.tab = getService().create(context, eType, priority);
        } catch (Exception e) {
            log.error("Error in DynamicLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public DynamicLayoutTab build() {
        try {
            getService().update(context, tab);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in DynamicLayoutTabBuilder.build(), error: ", e);
        }
        return tab;
    }

    @Override
    public void delete(Context c, DynamicLayoutTab tab) throws Exception {
        if (tab != null) {
            getService().delete(c, tab);
        }
    }

    public void delete(DynamicLayoutTab tab) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            DynamicLayoutTab attachedTab = c.reloadEntity(tab);
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
            DynamicLayoutTab tab = dynamicLayoutTabService.find(c, id);
            if (tab != null) {
                try {
                    dynamicLayoutTabService.delete(c, tab);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

    public DynamicLayoutTabBuilder withShortName(String shortName) {
        this.tab.setShortName(shortName);
        return this;
    }

    public DynamicLayoutTabBuilder withCustomFilter(String customFilter) {
        this.tab.setCustomFilter(customFilter);
        return this;
    }

    public DynamicLayoutTabBuilder withHeader(String header) {
        this.tab.setHeader(header);
        return this;
    }

    public DynamicLayoutTabBuilder withSecurity(LayoutSecurity security) {
        this.tab.setSecurity(security);
        return this;
    }

    public DynamicLayoutTabBuilder withLeading(boolean leading) {
        this.tab.setLeading(leading);
        return this;
    }

    public DynamicLayoutTabBuilder addBoxIntoNewRow(DynamicLayoutBox box) {
        return addBoxIntoNewRow(box, null, null);
    }

    public DynamicLayoutTabBuilder addBoxIntoNewRow(DynamicLayoutBox box, String rowStyle, String cellStyle) {

        DynamicLayoutRow newRow = new DynamicLayoutRow();
        newRow.setStyle(rowStyle);

        DynamicLayoutCell newCell = new DynamicLayoutCell();
        newCell.setStyle(cellStyle);

        newCell.addBox(box);
        newRow.addCell(newCell);
        this.tab.addRow(newRow);

        return this;
    }

    public DynamicLayoutTabBuilder addBoxIntoLastRow(DynamicLayoutBox box) {
        return addBoxIntoLastRow(box, null);
    }

    public DynamicLayoutTabBuilder addBoxIntoLastRow(DynamicLayoutBox box, String cellStyle) {
        List<DynamicLayoutRow> rows = this.tab.getRows();
        if (rows.isEmpty()) {
            return addBoxIntoNewRow(box);
        }

        DynamicLayoutRow row = rows.get(rows.size() - 1);
        DynamicLayoutCell newCell = new DynamicLayoutCell();
        newCell.setStyle(cellStyle);

        newCell.addBox(box);
        row.addCell(newCell);

        return this;
    }

    public DynamicLayoutTabBuilder addBoxIntoLastCell(DynamicLayoutBox box) {
        List<DynamicLayoutRow> rows = this.tab.getRows();
        if (rows.isEmpty()) {
            return addBoxIntoNewRow(box);
        }

        DynamicLayoutRow row = rows.get(rows.size() - 1);
        List<DynamicLayoutCell> cells = row.getCells();
        if (cells.isEmpty()) {
            DynamicLayoutCell cell = new DynamicLayoutCell();
            row.addCell(cell);
            cell.addBox(box);
        } else {
            cells.get(cells.size() - 1).addBox(box);
        }

        return this;
    }

    public DynamicLayoutTabBuilder withMetadatasecurity(Set<MetadataField> metadataFields) {
        this.tab.setMetadataSecurityFields(metadataFields);
        return this;
    }

    public DynamicLayoutTabBuilder addMetadatasecurity(MetadataField metadataField) {
        if (this.tab.getMetadataSecurityFields() == null) {
            this.tab.setMetadataSecurityFields(new HashSet<>());
        }
        this.tab.getMetadataSecurityFields().add(metadataField);
        return this;
    }

    public DynamicLayoutTabBuilder addTab2SecurityGroups(Group group, DynamicLayoutTab alternativeTab) {
        if (this.tab.getTab2SecurityGroups() == null) {
            this.tab.setTab2SecurityGroups(new HashSet<>());
        }
        this.tab.getTab2SecurityGroups().add(
            new DynamicLayoutTab2SecurityGroup(
                new DynamicLayoutTab2SecurityGroup.DynamicLayoutTab2SecurityGroupId(tab, group),
                tab, group, alternativeTab)
        );
        return this;
    }
}
