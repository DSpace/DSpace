/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager.plugins.adapter.decorators;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.objectmanager.AbstractAdapter;
import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by Bavo Van Geit
 * Date: 25/11/14
 * Time: 13:59
 */
@Component
public class AdapterDecoratorManager {

    public static final String FILE = "file";
    public static final String FILE_SEC = "fileSec";

    private static Logger log = Logger.getLogger(AdapterDecoratorManager.class);

    private Map<Integer,Map<String,List<AttributeDecorator>>> attributeDecoratorsMap;

    private Map<Integer,Map<String,List<ElementDecorator>>> elementDecoratorsMap;

    public static AdapterDecoratorManager getInstance(){
        List<AdapterDecoratorManager> servicesByType = new DSpace().getServiceManager().getServicesByType(AdapterDecoratorManager.class);
        if(servicesByType.size()>0){
            return servicesByType.get(0);
        }else{
            log.error("AdapterDecoratorManager service not found.");
        }
        return null;
    }

    @Autowired(required=false)
    public void setAttributeDecorators(List<AttributeDecorator> attributeDecorators) {
        Map<Integer,Map<String,List<AttributeDecorator>>> dsoTypeSectionDecoratorMap = new HashMap<Integer, Map<String, List<AttributeDecorator>>>();
        for (AttributeDecorator decorator : attributeDecorators){

            for (Integer dsoType: decorator.getApplicableTypes()){
                Map<String, List<AttributeDecorator>> sectionDecoratorMap = dsoTypeSectionDecoratorMap.get(dsoType);
                if(sectionDecoratorMap==null){
                    sectionDecoratorMap = new HashMap<String, List<AttributeDecorator>>();
                }
                for (String section: decorator.getApplicableSections()){

                    List<AttributeDecorator> decoratorList = sectionDecoratorMap.get(section);
                    if(decoratorList==null){
                        decoratorList = new ArrayList<AttributeDecorator>();
                    }
                    decoratorList.add(decorator);
                    sectionDecoratorMap.put(section,decoratorList);
                }
                dsoTypeSectionDecoratorMap.put(dsoType,sectionDecoratorMap);
            }
        }
        this.attributeDecoratorsMap = dsoTypeSectionDecoratorMap;
    }

    @Autowired(required=false)
    public void setElementDecorators(List<ElementDecorator> elementDecorators) {
        Map<Integer,Map<String,List<ElementDecorator>>> dsoTypeSectionDecoratorMap = new HashMap<Integer, Map<String, List<ElementDecorator>>>();
        for (ElementDecorator decorator : elementDecorators){

            for (Integer dsoType: decorator.getApplicableTypes()){
                Map<String, List<ElementDecorator>> sectionDecoratorMap = dsoTypeSectionDecoratorMap.get(dsoType);
                if(sectionDecoratorMap==null){
                    sectionDecoratorMap = new HashMap<String, List<ElementDecorator>>();
                }
                for (String section: decorator.getApplicableSections()){

                    List<ElementDecorator> decoratorList = sectionDecoratorMap.get(section);
                    if(decoratorList==null){
                        decoratorList = new ArrayList<ElementDecorator>();
                    }
                    decoratorList.add(decorator);
                    sectionDecoratorMap.put(section,decoratorList);
                }
                dsoTypeSectionDecoratorMap.put(dsoType, sectionDecoratorMap);
            }
        }
        this.elementDecoratorsMap = dsoTypeSectionDecoratorMap;
    }

    public List<AttributeDecorator> getAttributeDecorators() {

        Set<AttributeDecorator> decoratorsSet = new HashSet<AttributeDecorator>();

        for (Map.Entry<Integer,Map<String,List<AttributeDecorator>>> entry : attributeDecoratorsMap.entrySet()) {
            for (Map.Entry<String,List<AttributeDecorator>> decorators : entry.getValue().entrySet()) {
                decoratorsSet.addAll(decorators.getValue());
            }
        }

        return new ArrayList<AttributeDecorator>(decoratorsSet);
    }

    public List<ElementDecorator> getElementDecorators() {

        Set<ElementDecorator> decoratorsSet = new HashSet<ElementDecorator>();

        for (Map.Entry<Integer,Map<String,List<ElementDecorator>>> entry : elementDecoratorsMap.entrySet()) {
            for (Map.Entry<String,List<ElementDecorator>> decorators : entry.getValue().entrySet()) {
                decoratorsSet.addAll(decorators.getValue());
            }
        }

        return new ArrayList<ElementDecorator>(decoratorsSet);
    }

    public void decorateAttribute(Context context, int dsoType, String section, AttributeMap attributes, Item item, Bitstream bitstream, String fileID, String groupID, String admID) {
       if(attributeDecoratorsMap!=null && attributeDecoratorsMap.containsKey(dsoType)) {
           Map<String, List<AttributeDecorator>> sectionMap = attributeDecoratorsMap.get(dsoType);
           if (sectionMap != null) {
               List<AttributeDecorator> decorators = sectionMap.get(section);
               if (decorators != null) {
                   for (AttributeDecorator d : decorators) {
                       try {
                           d.decorate(context, attributes, item, bitstream, fileID, groupID, admID, section, dsoType);
                       } catch (SQLException e) {
                           e.printStackTrace();
                           log.error(e);
                       }
                   }
               }
           }
       }
    }

    public void decorateElement(Context context, int dsoType, String section, AbstractAdapter adapter) {
        if(elementDecoratorsMap!=null && elementDecoratorsMap.containsKey(dsoType)) {
            Map<String, List<ElementDecorator>> sectionMap = elementDecoratorsMap.get(dsoType);
            if (sectionMap != null) {
                List<ElementDecorator> decorators = sectionMap.get(section);
                if (decorators != null) {
                    for (ElementDecorator d : decorators) {
                        try {
                            d.decorate(context, adapter, section, dsoType);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            log.error(e);
                        } catch (SAXException e) {
                            e.printStackTrace();
                            log.error(e);
                        }
                    }
                }
            }
        }
    }



}