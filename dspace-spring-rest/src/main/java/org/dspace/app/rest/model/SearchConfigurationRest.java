/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to store the information that'll be shown on the /search endpoint.
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

    @Override
    public boolean equals(Object object){
        return (object instanceof SearchConfigurationRest &&
                new EqualsBuilder().append(this.getCategory(), ((SearchConfigurationRest) object).getCategory())
                        .append(this.getType(), ((SearchConfigurationRest) object).getType())
                        .append(this.getController(), ((SearchConfigurationRest) object).getController())
                        .append(this.getScope(), ((SearchConfigurationRest) object).getScope())
                        .append(this.getConfigurationName(), ((SearchConfigurationRest) object).getConfigurationName())
                        .append(this.getFilters(), ((SearchConfigurationRest) object).getFilters())
                        .append(this.getSortOptions(), ((SearchConfigurationRest) object).getSortOptions())
                        .isEquals());
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getCategory())
                .append(this.getType())
                .append(this.getController())
                .append(this.getScope())
                .append(this.getConfigurationName())
                .append(this.getFilters())
                .append(this.getSortOptions())
                .toHashCode();
    }


    public static class Filter{
        private String filter;
        private List<Operator> operators = new LinkedList<>();

        public static final String OPERATOR_EQUALS = "equals";
        public static final String OPERATOR_NOTEQUALS = "notequals";
        public static final String OPERATOR_AUTHORITY = "authority";
        public static final String OPERATOR_NOTAUTHORITY = "notauthority";
        public static final String OPERATOR_CONTAINS = "contains";
        public static final String OPERATOR_NOTCONTAINS = "notcontains";


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
        @Override
        public boolean equals(Object object){
            return (object instanceof SearchConfigurationRest.Filter &&
                    new EqualsBuilder().append(this.filter, ((Filter) object).filter)
                            .append(this.getOperators(), ((Filter) object).getOperators())
                            .isEquals());
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(filter)
                    .append(operators)
                    .toHashCode();
        }
        public static class Operator{
            private String operator;
            public Operator(String operator){
                this.operator = operator;
            }
            public String getOperator(){
                return operator;
            }
            @Override
            public boolean equals(Object object){
                return (object instanceof SearchConfigurationRest.Filter.Operator &&
                        new EqualsBuilder().append(this.getOperator(), ((Operator) object).getOperator()).isEquals());
            }
            @Override
            public int hashCode() {
                return new HashCodeBuilder(17, 37)
                        .append(operator)
                        .toHashCode();
            }
        }
    }

    public static class SortOption{

        //TODO Remove this ignore when the proper actualName gets added through the bean ID
        @JsonIgnore
        private String actualName;
        private String name;

        public void setActualName(String name){
            this.actualName = name;
        }
        public String getActualName(){
            return actualName;
        }
        public void setName(String metadata){
            this.name = metadata;
        }
        public String getName(){
            return name;
        }
        @Override
        public boolean equals(Object object){
            return (object instanceof SearchConfigurationRest.SortOption &&
                    new EqualsBuilder().append(this.getName(), ((SortOption) object).getName())
                            .append(this.getActualName(), ((SortOption) object).getActualName())
                            .isEquals());
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(actualName)
                    .append(name)
                    .toHashCode();
        }
    }
}
