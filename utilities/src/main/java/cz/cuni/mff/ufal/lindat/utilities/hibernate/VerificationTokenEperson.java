package cz.cuni.mff.ufal.lindat.utilities.hibernate;

public class VerificationTokenEperson extends GenericEntity implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int eid;
	private String token;
	private String email;
	
	public VerificationTokenEperson(){
		
	}
	
	public VerificationTokenEperson(String token, int eid, String email){
		this.eid = eid;
		this.token = token;
		this.email = email;
	}
	
	public String getToken(){
		return this.token;
	}
	
	public int getEid(){
		return this.eid;
	}
	
	public String getEmail(){
		return this.email;
	}
	
	public void setToken(String token){
		this.token = token;
	}
	
	public void setEid(int eid){
		this.eid = eid;
	}
	
	public void setEmail(String email){
		this.email = email;
	}

	@Override
	public int getID() {
		return eid;
	}

}
