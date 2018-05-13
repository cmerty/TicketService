package tickets.api.dto;

public class BanRequest {
	private String email;
	private boolean isBanned;
	public BanRequest(String email, boolean isBanned) {
		this.email = email;
		this.isBanned = isBanned;
	}
	public BanRequest() {
	}
	public String getEmail() {
		return email;
	}
	public boolean getIsBanned() {
		return isBanned;
	}
	
	

}
