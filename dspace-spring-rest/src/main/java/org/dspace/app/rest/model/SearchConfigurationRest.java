package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.DiscoveryRestController;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by raf on 22/09/2017.
 */
public class SearchConfigurationRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;
    @JsonIgnore
    private String scope;
    @JsonIgnore
    private String configurationName;

    private List<Filter> filters = new LinkedList<>();
    private List<SortOption> sortOptions = new LinkedList<>();

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getScope() {
        return scope;
    }
    public void setScope(String scope){
        this.scope = scope;
    }

    public String getConfigurationName() {
        return configurationName;
    }
    public void setConfigurationName(String configurationName){
        this.configurationName = configurationName;
    }

    public void addFilter(Filter filter){
        filters.add(filter);
    }
    public List<Filter> getFilters(){
        return filters;
    }

    public void addSortOption(SortOption sortOption){
        sortOptions.add(sortOption);
    }
    public List<SortOption> getSortOptions(){
        return sortOptions;
    }
    public static class Filter{
        private String filter;
        private List<Operator> operators = new LinkedList<>();

        private static final String OPERATOR_EQUALS = "equals";
        private static final String OPERATOR_NOTEQUALS = "notequals";
        private static final String OPERATOR_AUTHORITY = "authority";
        private static final String OPERATOR_NOTAUTHORITY = "notauthority";
        private static final String OPERATOR_CONTAINS = "contains";
        private static final String OPERATOR_NOTCONTAINS = "notcontains";


        public void setFilter(String filter){
            this.filter = filter;
        }
        public String getFilter(){
            return filter;
        }

        public void addOperator(Operator operator){
            operators.add(operator);
        }
        public List<Operator> getOperators() {
            return operators;
        }
        public void addDefaultOperatorsToList(){
            operators.add(new Operator(OPERATOR_EQUALS));
            operators.add(new Operator(OPERATOR_NOTEQUALS));
            operators.add(new Operator(OPERATOR_AUTHORITY));
            operators.add(new Operator(OPERATOR_NOTAUTHORITY));
            operators.add(new Operator(OPERATOR_CONTAINS));
            operators.add(new Operator(OPERATOR_NOTCONTAINS));
        }
        public static class Operator{
            private String operator;
            public Operator(String operator){
                this.operator = operator;
            }
            public String getOperator(){
                return operator;
            }
        }
    }

    public static class SortOption{

        //TODO Remove this ignore when the proper name gets added through the bean ID
        @JsonIgnore
        private String name;
        private String metadata;

        public void setName(String name){
            this.name = name;
        }
        public String getName(){
            return name;
        }
        public void setMetadata(String metadata){
            this.metadata = metadata;
        }
        public String getMetadata(){
            return metadata;
        }
    }
}
