package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Constants;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 22-okt-2010
 * Time: 15:10:38
 *
 * An edit form that will allow the curator to edit the submission
 */
public class CuratorEditMetadataForm extends AbstractDSpaceTransformer {

    private static final Message T_submission_title = message("xmlui.Submission.general.submission.title");
    private static final Message T_main_head = message("xmlui.Submission.Submissions.curator.edit.item");

    private static final Message T_submit_update = message("xmlui.general.update");
    private static final Message T_submit_return = message("xmlui.general.return");

    private static final Message T_head1 = message("xmlui.administrative.item.EditItemMetadataForm.head1");
    private static final Message T_name_label = message("xmlui.administrative.item.EditItemMetadataForm.name_label");
    private static final Message T_value_label = message("xmlui.administrative.item.EditItemMetadataForm.value_label");
    private static final Message T_submit_add = message("xmlui.administrative.item.EditItemMetadataForm.submit_add");
    private static final Message T_para1 = message("xmlui.administrative.item.EditItemMetadataForm.para1");

    private static final Message T_head2 = message("xmlui.administrative.item.EditItemMetadataForm.head2");
    private static final Message T_column1 = message("xmlui.administrative.item.EditItemMetadataForm.column1");
    private static final Message T_column2 = message("xmlui.administrative.item.EditItemMetadataForm.column2");
    private static final Message T_column3 = message("xmlui.administrative.item.EditItemMetadataForm.column3");
    private static final Message T_column4 = message("xmlui.administrative.item.EditItemMetadataForm.column4");
    private static final Message T_unlock = message("xmlui.authority.confidence.unlock.help");


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        pageMeta.addMetadata("title").addContent(T_submission_title);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        int wfItemID = parameters.getParameterAsInteger("wfItemID",-1);
        WorkflowItem wfItem = WorkflowItem.find(context, wfItemID);
        Item item = wfItem.getItem();

        AuthorizeManager.authorizeAction(context, item, Constants.WRITE);

        int collectionID=  wfItem.getCollection().getID();

        DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        Arrays.sort(values, new DCValueComparator());
        String actionURL = contextPath + "/submit-edit-metadata/" + knot.getId() + ".continue";
        Request request = ObjectModelHelper.getRequest(objectModel);
        String previousFieldID = request.getParameter("field");
        Map<Integer, String> confOptions = Choices.getConfidenceOptions();

        Division main = body.addInteractiveDivision("edit-item-status", actionURL, Division.METHOD_POST,"primary administrative item");
        main.setHead(T_main_head);

// LIST: add new metadata
        List addForm = main.addList("addItemMetadata",List.TYPE_FORM);
        addForm.setHead(T_head1);

        Select addName = addForm.addItem().addSelect("field");
        addName.setLabel(T_name_label);
        MetadataField[] fields = MetadataField.findAll(context);
        for (MetadataField field : fields)
        {
                int fieldID = field.getFieldID();
                MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
                String name = schema.getName() +"."+field.getElement();
                if (field.getQualifier() != null)
                        name += "."+field.getQualifier();

                addName.addOption(fieldID, name);
        }
        if (previousFieldID != null)
                addName.setOptionSelected(previousFieldID);


        Composite addComposite = addForm.addItem().addComposite("value");
        addComposite.setLabel(T_value_label);

        TextArea addValue = addComposite.addTextArea("value");
        addValue.setSize(4, 35);
        addForm.addItem().addButton("submit_add").setValue(T_submit_add);




        // PARA: Disclaimer
        main.addPara(T_para1);


        Para actions = main.addPara(null,"edit-metadata-actions top" );
        actions.addButton("submit_update").setValue(T_submit_update);
        actions.addButton("submit_return").setValue(T_submit_return);



        // TABLE: Metadata
        main.addHidden("scope").setValue("*");
        int index = 1;
        Table table = main.addTable("editItemMetadata",1,1);
        table.setHead(T_head2);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);

        ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
        for (DCValue value : values) {
            String name = value.schema + "_" + value.element;
            if (value.qualifier != null)
                name += "_" + value.qualifier;

            Row row = table.addRow(name, Row.ROLE_DATA, "metadata-value");

            CheckBox remove = row.addCell().addCheckBox("remove_" + index);
            remove.setLabel("remove");
            remove.addOption(index);

            Cell cell = row.addCell();
            cell.addContent(name.replaceAll("_", ". "));
            cell.addHidden("name_" + index).setValue(name);

            // value entry cell:
            Cell mdCell = row.addCell();
            String fieldKey = MetadataAuthorityManager.makeFieldKey(value.schema, value.element, value.qualifier);

            // put up just a selector when preferred choice presentation is select:
            if (cmgr.isChoicesConfigured(fieldKey) &&
                    Params.PRESENTATION_SELECT.equals(cmgr.getPresentation(fieldKey))) {
                Select mdSelect = mdCell.addSelect("value_" + index);
                mdSelect.setSize(1);
                Choices cs = cmgr.getMatches(fieldKey, value.value, collectionID, 0, 0, null);
                if (cs.defaultSelected < 0)
                    mdSelect.addOption(true, value.value, value.value);
                for (int i = 0; i < cs.values.length; ++i) {
                    mdSelect.addOption(i == cs.defaultSelected, cs.values[i].value, cs.values[i].label);
                }
            } else {
                TextArea mdValue = mdCell.addTextArea("value_" + index);
                mdValue.setSize(4, 35);
                mdValue.setValue(value.value);
                boolean isAuth = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);
                if (isAuth) {
                    Cell authCell = row.addCell();

                    TextArea authArea = authCell.addTextArea("value_"+index+"_authority");
                    authArea.setSize(2, 20);
                    authArea.setValue(value.authority);

                    Select confSelect = authCell.addSelect("value_"+index+"_confidence");

                    // add in descending order:
                    SortedSet<Integer> confKeys = new TreeSet<Integer>(confOptions.keySet());
                    while (confKeys.size() > 0) {
                        Integer currKey = confKeys.last();
                        confKeys.remove(currKey);
                        confSelect.addOption(confOptions.get(currKey),confOptions.get(currKey));
                    }
                    confSelect.setOptionSelected(confOptions.get(value.confidence));
                }
                if (ChoiceAuthorityManager.getManager().isChoicesConfigured(fieldKey)) {
                    mdValue.setChoices(fieldKey);
                    mdValue.setChoicesPresentation(Params.PRESENTATION_LOOKUP);
                    mdValue.setChoicesClosed(ChoiceAuthorityManager.getManager().isClosed(fieldKey));
                }
            }

            // Tick the index counter;
            index++;
        }


        // PARA: actions
        actions = main.addPara(null,"edit-metadata-actions bottom" );
        actions.addButton("submit_update").setValue(T_submit_update);
        actions.addButton("submit_return").setValue(T_submit_return);

        // Handle submit by clicking "Propagate" button
        String propagateMetadataFieldName = request.getParameter("propagate_md_field_name");
        if(propagateMetadataFieldName != null) {
            // User clicked "Propagate" button and not "Update" button
            main.addHidden("propagate_show_popup").setValue("1");
            main.addHidden("propagate_md_field").setValue(propagateMetadataFieldName);
            // Client-side javascript will take care of opening the window
        }
    }


    /**
     * Compare two metadata element's name so that they may be sorted.
     */
    class DCValueComparator implements Comparator {
            public int compare(Object arg0, Object arg1) {
                    final DCValue o1 = (DCValue)arg0;
                    final DCValue o2 = (DCValue)arg1;
                    final String s1 = o1.schema + o1.element + (o1.qualifier==null?"":("." + o1.qualifier));
                    final  String s2 = o2.schema + o2.element + (o2.qualifier==null?"":("." + o2.qualifier));
                    return s1.compareTo(s2);
            }
    }
}
