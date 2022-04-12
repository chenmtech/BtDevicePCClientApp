package application;

/**
 * 账户类
 * @author gdmc
 *
 */
public class Account {
	// 账户名
	private String name;
	
	// 密码
	private String password;
	
	// 是否已登录
	private boolean login;
	
	public Account() {
		
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isLogin() {
		return login;
	}
	public void setLogin(boolean login) {
		this.login = login;
	}
}
