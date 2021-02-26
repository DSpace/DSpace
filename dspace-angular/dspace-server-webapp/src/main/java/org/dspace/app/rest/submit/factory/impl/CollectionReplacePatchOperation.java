/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.sql.SQLException;

import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" patch operation
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class CollectionReplacePatchOperation extends ReplacePatchOperation<String> {

    @Autowired
    CollectionService collectionService;
    @Autowired
    ItemService itemService;
    @Autowired
    WorkspaceItemService workspaceItemService;

    @Override
    void replace(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws SQLException, DCInputsReaderException {

        if (!(source instanceof WorkspaceItem)) {
            throw new IllegalArgumentException("the replace operation is only supported on workspaceitem");
        }
        WorkspaceItem wsi = (WorkspaceItem) source;
        String uuid = (String) value;
        Collection fromCollection = source.getCollection();
        Collection toCollection = collectionService.find(context, UUIDUtils.fromString(uuid));
        workspaceItemService.move(context, wsi, fromCollection, toCollection);

    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

}
