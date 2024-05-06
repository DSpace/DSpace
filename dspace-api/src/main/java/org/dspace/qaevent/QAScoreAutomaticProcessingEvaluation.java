/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A configurable implementation of {@link QAEventAutomaticProcessingEvaluation} allowing to define thresholds for
 * automatic acceptance, rejection or ignore of {@link QAEvent} matching a specific, optional, item filter
 * {@link LogicalStatement}. If the item filter is not defined only the score threshold will be used.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class QAScoreAutomaticProcessingEvaluation implements QAEventAutomaticProcessingEvaluation {
    /**
     * The minimum score of QAEvent to be considered for automatic approval (trust must be greater or equals to that)
     */
    private double scoreToApprove;

    /**
     * The threshold under which QAEvent are considered for automatic ignore (trust must be less or equals to that)
     */
    private double scoreToIgnore;

    /**
     * The threshold under which QAEvent are considered for automatic rejection (trust must be less or equals to that)
     */
    private double scoreToReject;

    /**
     * The optional logical statement that must pass for item target of a QAEvent to be considered for automatic
     * approval
     */
    private LogicalStatement itemFilterToApprove;

    /**
     * The optional logical statement that must pass for item target of a QAEvent to be considered for automatic
     * ignore
     */
    private LogicalStatement itemFilterToIgnore;

    /**
     * The optional logical statement that must pass for item target of a QAEvent to be considered for automatic
     * rejection
     */
    private LogicalStatement itemFilterToReject;

    @Autowired
    private ItemService itemService;

    @Override
    public AutomaticProcessingAction evaluateAutomaticProcessing(Context context, QAEvent qaEvent) {
        Item item = findItem(context, qaEvent.getTarget());

        if (shouldReject(context, qaEvent.getTrust(), item)) {
            return AutomaticProcessingAction.REJECT;
        } else if (shouldIgnore(context, qaEvent.getTrust(), item)) {
            return AutomaticProcessingAction.IGNORE;
        } else if (shouldApprove(context, qaEvent.getTrust(), item)) {
            return AutomaticProcessingAction.ACCEPT;
        } else {
            return null;
        }

    }

    private Item findItem(Context context, String uuid) {
        try {
            return itemService.find(context, UUID.fromString(uuid));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean shouldReject(Context context, double trust, Item item) {
        return trust <= scoreToReject &&
            (itemFilterToReject == null || itemFilterToReject.getResult(context, item));
    }

    private boolean shouldIgnore(Context context, double trust, Item item) {
        return trust <= scoreToIgnore &&
            (itemFilterToIgnore == null || itemFilterToIgnore.getResult(context, item));
    }

    private boolean shouldApprove(Context context, double trust, Item item) {
        return trust >= scoreToApprove &&
            (itemFilterToApprove == null || itemFilterToApprove.getResult(context, item));
    }

    public double getScoreToApprove() {
        return scoreToApprove;
    }

    public void setScoreToApprove(double scoreToApprove) {
        this.scoreToApprove = scoreToApprove;
    }

    public double getScoreToIgnore() {
        return scoreToIgnore;
    }

    public void setScoreToIgnore(double scoreToIgnore) {
        this.scoreToIgnore = scoreToIgnore;
    }

    public double getScoreToReject() {
        return scoreToReject;
    }

    public void setScoreToReject(double scoreToReject) {
        this.scoreToReject = scoreToReject;
    }

    public LogicalStatement getItemFilterToApprove() {
        return itemFilterToApprove;
    }

    public void setItemFilterToApprove(LogicalStatement itemFilterToApprove) {
        this.itemFilterToApprove = itemFilterToApprove;
    }

    public LogicalStatement getItemFilterToIgnore() {
        return itemFilterToIgnore;
    }

    public void setItemFilterToIgnore(LogicalStatement itemFilterToIgnore) {
        this.itemFilterToIgnore = itemFilterToIgnore;
    }

    public LogicalStatement getItemFilterToReject() {
        return itemFilterToReject;
    }

    public void setItemFilterToReject(LogicalStatement itemFilterToReject) {
        this.itemFilterToReject = itemFilterToReject;
    }
}

