/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * QAReinstateRequestAction is an implementation of the QualityAssuranceAction interface.
 * It is responsible for applying a correction to reinstate a specified item.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class QAReinstateRequestAction implements QualityAssuranceAction {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ItemService itemService;

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, QAMessageDTO message) {
        try {
            itemService.reinstate(context, item);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
    }

}