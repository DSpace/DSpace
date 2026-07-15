/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.rest.model.DynamicLayoutBoxRest;
import org.dspace.app.rest.model.DynamicLayoutCellRest;
import org.dspace.app.rest.model.DynamicLayoutRowRest;
import org.dspace.app.rest.model.DynamicLayoutTabRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.DynamicLayoutTabRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBox2SecurityGroup;
import org.dspace.layout.DynamicLayoutCell;
import org.dspace.layout.DynamicLayoutRow;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.DynamicLayoutTab2SecurityGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.DynamicLayoutBoxService;
import org.dspace.layout.service.DynamicLayoutTabService;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity DynamicLayoutTab to the REST data model
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class DynamicLayoutTabConverter implements DSpaceConverter<DynamicLayoutTab, DynamicLayoutTabRest> {

    @Autowired
    private EntityTypeService eService;

    @Autowired
    private DynamicLayoutBoxService dynamicLayoutBoxService;

    @Autowired
    private DynamicLayoutBoxConverter boxConverter;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DynamicLayoutTabService dynamicLayoutTabService;

    @Override
    public DynamicLayoutTabRest convert(DynamicLayoutTab model, Projection projection) {
        Item item = getScopeItem();

        if (item == null || hasAccess(getScopeItem(), model)) {
            return convertTab(model, projection);
        }

        return Optional.ofNullable(findAlternativeTab(model))
                       .map(tab -> convertTab(tab, projection))
                       .orElseGet(DynamicLayoutTabRest::new);
    }

    private boolean hasAccess(Item item, DynamicLayoutTab tab) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return dynamicLayoutTabService.hasAccess(context, tab, item);
    }

    private DynamicLayoutTab findAlternativeTab(DynamicLayoutTab tab) {
        return tab.getTab2SecurityGroups()
                  .stream()
                  .map(DynamicLayoutTab2SecurityGroup::getAlternativeTab)
                  .filter(Objects::nonNull)
                  .findFirst()
                  .orElse(null);
    }

    private DynamicLayoutTabRest convertTab(DynamicLayoutTab tab, Projection projection) {
        DynamicLayoutTabRest rest = new DynamicLayoutTabRest();
        rest.setId(tab.getID());
        rest.setEntityType(tab.getEntity().getLabel());
        rest.setCustomFilter(tab.getCustomFilter());
        rest.setShortname(tab.getShortName());
        rest.setHeader(tab.getHeader());
        rest.setPriority(tab.getPriority());
        rest.setSecurity(tab.getSecurity());
        rest.setRows(convertRows(getScopeItem(), tab.getRows(), projection));
        rest.setLeading(tab.isLeading());
        return rest;
    }

    @Override
    public Class<DynamicLayoutTab> getModelClass() {
        return DynamicLayoutTab.class;
    }

    public DynamicLayoutTab toModel(Context context, DynamicLayoutTabRest rest) {
        DynamicLayoutTab tab = new DynamicLayoutTab();
        tab.setHeader(rest.getHeader());
        tab.setPriority(rest.getPriority());
        tab.setSecurity(LayoutSecurity.valueOf(rest.getSecurity()));
        tab.setShortName(rest.getShortname());
        tab.setEntity(findEntityType(context, rest));
        tab.setCustomFilter(rest.getCustomFilter());
        tab.setLeading(rest.isLeading());
        rest.getRows().forEach(row -> tab.addRow(toRowModel(context, row)));
        return tab;
    }

    private List<DynamicLayoutRowRest> convertRows(Item item, List<DynamicLayoutRow> rows, Projection projection) {
        return rows.stream()
            .map(row -> convertRow(item, row, projection))
            .filter(row -> CollectionUtils.isNotEmpty(row.getCells()))
            .collect(Collectors.toList());
    }

    private DynamicLayoutRowRest convertRow(Item item, DynamicLayoutRow row, Projection projection) {
        DynamicLayoutRowRest rest = new DynamicLayoutRowRest();
        rest.setStyle(row.getStyle());
        rest.setCells(convertCells(item, row.getCells(), projection));
        return rest;
    }

    private List<DynamicLayoutCellRest> convertCells(Item item, List<DynamicLayoutCell> cells, Projection projection) {
        return cells.stream()
            .map(cell -> convertCell(item, cell, projection))
            .filter(cell -> CollectionUtils.isNotEmpty(cell.getBoxes()))
            .collect(Collectors.toList());
    }

    private DynamicLayoutCellRest convertCell(Item item, DynamicLayoutCell cell, Projection projection) {
        DynamicLayoutCellRest rest = new DynamicLayoutCellRest();
        rest.setStyle(cell.getStyle());
        rest.setBoxes(convertBoxes(item, cell.getBoxes(), projection));
        return rest;
    }

    private List<DynamicLayoutBoxRest> convertBoxes(Item item, List<DynamicLayoutBox> boxes, Projection projection) {
        return boxes.stream()
                    .map(box -> getDynamicLayoutBox(item, box))
                    .filter(Objects::nonNull)
                    .map(box -> boxConverter.convert(box, projection))
                    .collect(Collectors.toList());
    }

    private DynamicLayoutBox getDynamicLayoutBox(Item item, DynamicLayoutBox box) {

        if (item == null) {
            return box;
        }

        return Optional.of(box)
                       .filter(b -> hasAccess(item, b) && hasContent(item, b))
                       .orElseGet(() ->
                           Optional.ofNullable(findAlternativeBox(box))
                                   .filter(altBox -> hasContent(item, altBox))
                                   .orElse(null));
    }

    private boolean hasAccess(Item item, DynamicLayoutBox box) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return dynamicLayoutBoxService.hasAccess(context, box, item);
    }

    private boolean hasContent(Item item, DynamicLayoutBox box) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return dynamicLayoutBoxService.hasContent(context, box, item);
    }

    private DynamicLayoutBox findAlternativeBox(DynamicLayoutBox box) {
        return box.getBox2SecurityGroups()
                  .stream()
                  .map(DynamicLayoutBox2SecurityGroup::getAlternativeBox)
                  .filter(Objects::nonNull)
                  .findFirst()
                  .orElse(null);
    }

    private DynamicLayoutRow toRowModel(Context context, DynamicLayoutRowRest rowRest) {
        DynamicLayoutRow row = new DynamicLayoutRow();
        row.setStyle(rowRest.getStyle());
        rowRest.getCells().forEach(cell -> row.addCell(toCellModel(context, cell)));
        return row;
    }

    private DynamicLayoutCell toCellModel(Context context, DynamicLayoutCellRest cellRest) {
        DynamicLayoutCell cell = new DynamicLayoutCell();
        cell.setStyle(cellRest.getStyle());
        cellRest.getBoxes().forEach(box -> cell.addBox(boxConverter.toModel(context, box)));
        return cell;
    }

    private EntityType findEntityType(Context context, DynamicLayoutTabRest rest) {
        try {
            return eService.findByEntityType(context, rest.getEntityType());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e.getMessage(), e);
        }
    }

    private Item getScopeItem() {
        return Optional.ofNullable(requestService.getCurrentRequest())
            .map(rq -> (String) rq.getAttribute(DynamicLayoutTabRestRepository.SCOPE_ITEM_ATTRIBUTE))
            .map(itemId -> findItem(itemId))
            .orElse(null);
    }

    private Item findItem(String uuid) {
        try {
            return itemService.find(ContextUtil.obtainCurrentRequestContext(), UUIDUtils.fromString(uuid));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}
