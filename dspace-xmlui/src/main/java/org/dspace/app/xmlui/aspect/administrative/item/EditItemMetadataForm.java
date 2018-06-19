/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

/**
 * Display a list of all metadata available for this item and allow the user to
 * add, remove, or update it.
 *
 * @author Jay Paz
 * @author Scott Phillips
 */

public class EditItemMetadataForm extends AbstractDSpaceTransformer {
        
        /** Language strings */
        private static final Message T_dspace_home = message("xmlui.general.dspace_home");
        private static final Message T_submit_update = message("xmlui.general.update");
        private static final Message T_submit_return = message("xmlui.general.return");
        private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
        private static final Message T_template_head = message("xmlui.administrative.item.general.template_head");
        private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
        private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
        private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
        private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
        private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");
        private static final Message T_option_curate = message("xmlui.administrative.item.general.option_curate");

        private static final Message T_title = message("xmlui.administrative.item.EditItemMetadataForm.title");
        private static final Message T_trail = message("xmlui.administrative.item.EditItemMetadataForm.trail");
        private static final Message T_head1 = message("xmlui.administrative.item.EditItemMetadataForm.head1");
        private static final Message T_name_label = message("xmlui.administrative.item.EditItemMetadataForm.name_label");
        private static final Message T_value_label = message("xmlui.administrative.item.EditItemMetadataForm.value_label");
        private static final Message T_lang_label = message("xmlui.administrative.item.EditItemMetadataForm.lang_label");
        private static final Message T_submit_add = message("xmlui.administrative.item.EditItemMetadataForm.submit_add");
        private static final Message T_para1 = message("xmlui.administrative.item.EditItemMetadataForm.para1");

        
        private static final Message T_head2 = message("xmlui.administrative.item.EditItemMetadataForm.head2");
        private static final Message T_column1 = message("xmlui.administrative.item.EditItemMetadataForm.column1");
        private static final Message T_column2 = message("xmlui.administrative.item.EditItemMetadataForm.column2");
        private static final Message T_column3 = message("xmlui.administrative.item.EditItemMetadataForm.column3");
        private static final Message T_column4 = message("xmlui.administrative.item.EditItemMetadataForm.column4");
        private static final Message T_unlock = message("xmlui.authority.confidence.unlock.help");

        protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        protected ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        protected MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

        public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
            Item item = itemService.find(context, UUID.fromString(parameters.getParameter("itemID", null)));
            Collection owner = item.getOwningCollection();
            UUID collectionID = (owner == null) ? null : owner.getID();

            pageMeta.addMetadata("choice", "collection").addContent(String.valueOf(collectionID));
            pageMeta.addMetadata("title").addContent(T_title);


            pageMeta.addMetadata("stylesheet", "screen", "datatables", true).addContent("../../static/Datatables/DataTables-1.8.0/media/css/datatables.css");
            pageMeta.addMetadata("javascript", "static", "datatables", true).addContent("static/Datatables/DataTables-1.8.0/media/js/jquery.dataTables.min.js");
            pageMeta.addMetadata("stylesheet", "screen", "person-lookup", true).addContent("../../static/css/authority/person-lookup.css");
            pageMeta.addMetadata("javascript", null, "person-lookup", true).addContent("../../static/js/person-lookup.js");


            pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
            pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
            pageMeta.addTrail().addContent(T_trail);
        }

        /**
         * Add either the simple Ajax response document or the full
         * document with header and full edit item form based on the
         * mode parameter.  Mode parameter values are set in the Flowscipt
         * and can be either 'ajax' or 'normal'.
         */
        @SuppressWarnings("unchecked") // the cast is correct
        public void addBody(Body body) throws SQLException, WingException
        {
                // Get our parameters and state
                UUID itemID = UUID.fromString(parameters.getParameter("itemID", null));
                Item item = itemService.find(context, itemID);
                java.util.List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                Collections.sort(values, new DCValueComparator());
                String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();

                Request request = ObjectModelHelper.getRequest(objectModel);
                String previousFieldID = request.getParameter("field");

        // Metadata editing is the only type of editing available for a template item.
        boolean editingTemplateItem = false;
        String templateCollectionID = parameters.getParameter("templateCollectionID", null);
        Collection templateCollection = StringUtils.isBlank(templateCollectionID) ? null : collectionService.find(context, UUID.fromString(templateCollectionID));
        if (templateCollection != null)
        {
            Item templateItem = templateCollection.getTemplateItem();
            if (templateItem != null && templateItem.getID().equals(itemID))
            {
                editingTemplateItem = true;
            }
        }

        // DIVISION: main
        Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
        if (templateCollection != null && editingTemplateItem)
        {
            main.setHead(T_template_head.parameterize(templateCollection.getName()));
        }
        else
        {
            main.setHead(T_option_head);
        }

        Collection owner = item.getOwningCollection();

        // LIST: options
        if (!editingTemplateItem)
        {
          List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
          options.addItem().addXref(baseURL+"&submit_status",T_option_status);
          options.addItem().addXref(baseURL+"&submit_bitstreams",T_option_bitstreams);
          options.addItem().addHighlight("bold").addXref(baseURL+"&submit_metadata",T_option_metadata);
          options.addItem().addXref(baseURL + "&view_item", T_option_view);
          options.addItem().addXref(baseURL + "&submit_curate", T_option_curate);
        }

        // LIST: add new metadata
        List addForm = main.addList("addItemMetadata",List.TYPE_FORM);
        addForm.setHead(T_head1);

                Select addName = addForm.addItem().addSelect("field");
                addName.setLabel(T_name_label);
                java.util.List<MetadataField> fields = metadataFieldService.findAll(context);
                for (MetadataField field : fields)
                {
                        int fieldID = field.getID();
                        MetadataSchema schema = field.getMetadataSchema();
                        String name = schema.getName() +"."+field.getElement();
                        if (field.getQualifier() != null)
                        {
                            name += "." + field.getQualifier();
                        }

                        addName.addOption(fieldID, name);
                }
                if (previousFieldID != null)
                {
                    addName.setOptionSelected(previousFieldID);
                }


                Composite addComposite = addForm.addItem().addComposite("value");
                addComposite.setLabel(T_value_label);
                TextArea addValue = addComposite.addTextArea("value");
                Text addLang = addComposite.addText("language");

                addValue.setSize(4, 35);
                addLang.setLabel(T_lang_label);
                addLang.setSize(6);

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
                header.addCell().addContent(T_column4);

                for(MetadataValue value : values)
                {
                        String name = value.getMetadataField().toString('_');

                        Row row = table.addRow(name,Row.ROLE_DATA,"metadata-value");

                        CheckBox remove = row.addCell().addCheckBox("remove_"+index);
                        remove.setLabel("remove");
                        remove.addOption(index);

                        Cell cell = row.addCell();
                        cell.addContent(name.replaceAll("_", ". "));
                        cell.addHidden("name_"+index).setValue(name);

                        // value entry cell:
                        Cell mdCell = row.addCell();
                        String fieldKey = metadataAuthorityService.makeFieldKey(value.getMetadataField().getMetadataSchema().getName(), value.getMetadataField().getElement(), value.getMetadataField().getQualifier());

                        // put up just a selector when preferred choice presentation is select:
                        if (choiceAuthorityService.isChoicesConfigured(fieldKey) &&
                            Params.PRESENTATION_SELECT.equals(choiceAuthorityService.getPresentation(fieldKey)))
                        {
                            Select mdSelect = mdCell.addSelect("value_"+index);
                            mdSelect.setSize(1);
                            Choices cs = choiceAuthorityService.getMatches(fieldKey, value.getValue(), owner, 0, 0, null);
                            if (cs.defaultSelected < 0)
                            {
                                mdSelect.addOption(true, value.getValue(), value.getValue());
                            }
                            for (int i = 0; i < cs.values.length; ++i)
                            {
                                mdSelect.addOption(i == cs.defaultSelected, cs.values[i].value, cs.values[i].label);
                            }
                        }
                        else
                        {
                            TextArea mdValue = mdCell.addTextArea("value_"+index);
                        mdValue.setSize(4,35);
                        mdValue.setValue(value.getValue());
                            boolean isAuth = metadataAuthorityService.isAuthorityControlled(fieldKey);
                            if (isAuth)
                            {
                                mdValue.setAuthorityControlled();
                                mdValue.setAuthorityRequired(metadataAuthorityService.isAuthorityRequired(fieldKey));
                                Value authValue =  mdValue.setAuthorityValue((value.getAuthority() == null)?"":value.getAuthority(), Choices.getConfidenceText(value.getConfidence()));
                                // add the "unlock" button to auth field
                                Button unlock = authValue.addButton("authority_unlock_"+index,"ds-authority-lock");
                                unlock.setHelp(T_unlock);
                            }
                            if (choiceAuthorityService.isChoicesConfigured(fieldKey))
                            {
                                mdValue.setChoices(fieldKey);
                                if(Params.PRESENTATION_AUTHORLOOKUP.equals(choiceAuthorityService.getPresentation(fieldKey))){
                                    mdValue.setChoicesPresentation(Params.PRESENTATION_AUTHORLOOKUP);
                                }else{
                                    mdValue.setChoicesPresentation(Params.PRESENTATION_LOOKUP);
                                }
                                mdValue.setChoicesClosed(choiceAuthorityService.isClosed(fieldKey));
                            }
                        }
                        Text mdLang = row.addCell().addText("language_"+index);
                        mdLang.setSize(6);
                        mdLang.setValue(value.getLanguage());

                        // Tick the index counter;
                        index++;
                }

                
                
                
                // PARA: actions
                actions = main.addPara(null,"edit-metadata-actions bottom" );
                actions.addButton("submit_update").setValue(T_submit_update);
                actions.addButton("submit_return").setValue(T_submit_return);


                main.addHidden("administrative-continue").setValue(knot.getId());
        }



        /**
         * Compare two metadata element's name so that they may be sorted.
         */
        static class DCValueComparator implements Comparator, Serializable {
            public int compare(Object arg0, Object arg1) {
          			final MetadataValue o1 = (MetadataValue)arg0;
          			final MetadataValue o2 = (MetadataValue)arg1;
          			MetadataField o1Field = o1.getMetadataField();
          			MetadataField o2Field = o2.getMetadataField();
          			final String s1 = o1Field.getMetadataSchema().getName() + o1Field.getElement() + (o1Field.getQualifier()==null?"":("." + o1Field.getQualifier()));
          			final  String s2 = o2Field.getMetadataSchema().getName() + o2Field.getElement() + (o2Field.getQualifier()==null?"":("." + o2Field.getQualifier()));
          			return s1.compareTo(s2);
          		}
        }

}
