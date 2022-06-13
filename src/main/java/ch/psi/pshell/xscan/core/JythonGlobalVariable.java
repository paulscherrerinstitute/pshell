package ch.psi.pshell.xscan.core;

public class JythonGlobalVariable {

    private String name;
    private Object value;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    
    public static class DoubleVar extends JythonGlobalVariable{
        public Double getValue() {
            return (Double) super.getValue();
        }

        public void setValue(double value) {
            super.setValue(value);
        }
    }
    
    
    public static class StringVar extends JythonGlobalVariable{
        public String getValue() {
            return (String) super.getValue();
        }

        public void setValue(String value) {
            super.setValue(value);
        }
    }    
}
