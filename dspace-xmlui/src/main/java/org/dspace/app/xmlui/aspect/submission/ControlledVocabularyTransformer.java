/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;

/**
 * Renders the main controlled vocabulary division which contains a filter box.
 * This transformer is called by javascript to render a box in which the controlled vocabulary list is loaded.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ControlledVocabularyTransformer extends AbstractDSpaceTransformer{

    public static final Message T_title = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.title");
    public static final Message T_filter_label = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.filter.label");
    public static final Message T_filter_button = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.filter.button");
    public static final Message T_vocabulary_loading = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.loading");
    public static final Message T_vocabulary_error = message("xmlui.Submission.submit.DescribeStep.controlledvocabulary.error");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException {
        pageMeta.addMetadata("framing","popup").addContent(Boolean.TRUE.toString());
        pageMeta.addMetadata("title").addContent(T_title);
    }

    @Override
    public void addBody(Body body) throws WingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String vocabularyIdentifier = request.getParameter("vocabularyIdentifier");
        String metadataFieldName = request.getParameter("metadataFieldName");

        Division vocabularyDialog = body.addDivision("vocabulary_dialog_" + vocabularyIdentifier, "vocabulary-container");
        vocabularyDialog.addHidden("metadataFieldName").setValue(metadataFieldName);

        List filterList = vocabularyDialog.addList("filter-list", List.TYPE_FORM);
        filterList.addLabel(T_filter_label);
        Item actions = filterList.addItem();
        actions.addText("filter");
        actions.addButton("filter_button").setValue(T_filter_button);

        filterList.addItem("vocabulary-loading", "").addContent(T_vocabulary_loading);
        filterList.addItem("vocabulary-error", "error hidden").addContent(T_vocabulary_error);
    }
}
