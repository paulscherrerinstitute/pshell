package ch.psi.utils;

import java.util.ArrayList;
import java.util.Map;


public class Filter {
    String filter;
    ArrayList<FilterCondition> filterConditions = new ArrayList<>();
    
    public Filter(){        
    }
    
    public Filter(String filter) throws IllegalArgumentException{        
        set(filter);
    }

    public enum FilterOp {
        equal,
        notEqual,
        less,
        greater,
        greaterOrEqual,
        lessOrEqual
    }


    public static class FilterCondition {
        public final String id;
        public final FilterOp op;
        public final Object value;

        FilterCondition(String str)  throws IllegalArgumentException{
            try {
                String aux = null;
                if (str.contains("==")) {
                    aux = "==";
                    op = FilterOp.equal;
                } else if (str.contains("!=")) {
                    aux = "!=";
                    op = FilterOp.notEqual;
                } else if (str.contains(">=")) {
                    aux = ">=";
                    op = FilterOp.greaterOrEqual;
                } else if (str.contains("<=")) {
                    aux = "<=";
                    op = FilterOp.lessOrEqual;
                } else if (str.contains(">")) {
                    aux = ">";
                    op = FilterOp.greater;
                } else if (str.contains("<")) {
                    aux = "<";
                    op = FilterOp.less;
                } else {
                    op=null;
                }
                String[] tokens = str.split(aux);
                id = tokens[0].trim();
                aux = tokens[1].trim();
                if ((aux.startsWith("\"") && aux.endsWith("\"")) || (aux.startsWith("'") && aux.endsWith("'"))) {
                    value = aux.substring(1, aux.length() - 1);
                } else if (aux.equalsIgnoreCase("false")) {
                    value = Boolean.FALSE;
                } else if (aux.equalsIgnoreCase("true")) {
                    value = Boolean.TRUE;
                } else {
                    value = Double.valueOf(aux);
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException(str);
            }
        }

        public boolean check(Comparable c) {
            if (c instanceof Number) {
                c = (Double) (((Number) c).doubleValue());
            }
            switch (op) {
                case equal:
                    return c.compareTo(value) == 0;
                case notEqual:
                    return c.compareTo(value) != 0;
                case greater:
                    return c.compareTo(value) > 0;
                case less:
                    return c.compareTo(value) < 0;
                case greaterOrEqual:
                    return c.compareTo(value) >= 0;
                case lessOrEqual:
                    return c.compareTo(value) <= 0;
            }
            return false;
        }        
    }
    
    public final void set(String filter) throws IllegalArgumentException{
        this.filter = null;
        filterConditions.clear();
        if (filter != null) {
            try {
                for (String token : filter.split(" AND ")) {
                    filterConditions.add(new FilterCondition(token));
                }
            } catch (IllegalArgumentException ex) {
                filterConditions.clear();
                throw ex;
            }
            this.filter = filter;
        }
    }

    public String get() {
        return filter;
    }
    
    
    public ArrayList<FilterCondition> getConditions(){
        return (ArrayList<FilterCondition>)filterConditions.clone();
    }
    

    public boolean check(Map<String, ? extends Object> data) {
        if (filter != null) {
            try {
                for (FilterCondition filterCondition : filterConditions) {
                    Comparable val = (Comparable)data.get(filterCondition.id);
                    if (!filterCondition.check(val)) {
                        return false;
                    }
                }
            } catch (Exception ex) {
                return false;
            }
        }
        return true;
    }        
            
    
}