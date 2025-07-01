package ch.psi.pshell.xscan;

import ch.psi.pshell.utils.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Message holding data
 */
public class DataMessage extends Message{
	
	private static final long serialVersionUID = 1L;
	
	private final List<Object> data;
	private List<Metadata> metadata;
	
//	public DataMessage(){
//		this.data = new ArrayList<Object>();
//		this.metadata = new ArrayList<>();
//	}
//	
	public DataMessage(List<Metadata> metadata){
		this.data = new ArrayList<Object>();
		this.metadata = metadata;
	}
	
	public List<Object> getData(){
		return(data);
	}
	public List<Metadata> getMetadata(){
		return metadata;
	}
	public void setMetadata(List<Metadata> metadata){
		this.metadata = metadata;
	}
	
	// Utility functions
	
	@SuppressWarnings("unchecked")
	public <T> T getData(String id){
		int i=0;
		for(Metadata m: metadata){
			if(m.getId().equals(id)){
				return (T) data.get(i);
			}
			i++;
		}
		throw new IllegalArgumentException("No data found for id: "+id);
	}
	
	public Metadata getMetadata(String id){
		for(Metadata m: metadata){
			if(m.getId().equals(id)){
				return m;
			}
		}
		throw new IllegalArgumentException("No data found for id: "+id);
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("Message [ ");
		for (Object o : data) {
			if (o.getClass().isArray()) {
				// If the array object is of type double[] display its content
				if (o instanceof double[] oa) {
					b.append("[ ");
					for (double o1 : oa) {
						b.append(o1);
						b.append(" ");
					}
					b.append("]");
				} else {
					b.append(o.toString());
				}
			} else {
				b.append(o);
			}
			
			b.append(" ");
		}
		b.append("]");
		return b.toString();
	}
}
