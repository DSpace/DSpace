package org.dspace.rest.filter;

public enum OP {
    equals("==",true,"="),
    not_equals("!=",false,"="),
    like("like",true," like "),
    not_like("not like",false," like "),
    contains("like",true," like ") {
        public String prepareVal(String val) {
            return "%" + val + "%";
        }
    },
    doesnt_contain("not like",false," like "){
        public String prepareVal(String val) {
            return "%" + val + "%";
        }
    },
    exists(true),
    doesnt_exist(false),
    matches("~",true, "~"),
    doesnt_match("!~",false,"~");
    String disp;
    boolean bexists;
    String valop;
    
    /**
     * Construct an operation
     * @param disp
     *     Shorthand representation of operation
     * @param exists
     *     If true, construct exists query.  Otherwise, construct not exists query
     * @param valop
     *     Operator for exists/not exists query string
     */
    private OP(String disp, boolean exists, String valop) {
        this.disp = disp;
        this.bexists = exists;
        this.valop = valop;
    }
    
    /**
     * Construct an exists/not exists query
     */
    private OP(boolean exists) {
        this.disp = name();
        this.bexists = exists;
        this.valop = null;
    }
    public String prepareVal(String val) {
        return val;
    }
    
    public String getDisplay() {
    	return disp;
    }
    
    public String getValueOperation() {
    	return valop;
    }
    
    public boolean existsQuery() {
    	return bexists;
    }
}
