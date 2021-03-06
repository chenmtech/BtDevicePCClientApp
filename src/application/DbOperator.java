package application;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;
import com.cmtech.web.dbUtil.DbUtil;
import com.cmtech.web.dbUtil.RecordWebUtil;
import com.cmtech.web.btdevice.Account;

import javafx.application.Platform;

public class DbOperator {
	public static final String DEFAULT_DB_ADDRESS = "203.195.137.198:3306";
	private final IDbOperationCallback callback;
	
	public DbOperator(IDbOperationCallback callback) {
		this(callback, DEFAULT_DB_ADDRESS);
	}
	
	public DbOperator(IDbOperationCallback callback, String dbAddress) {
		this.callback = callback;
		DbUtil.setDbAddress(dbAddress);
	}
	
	public void testConnect(String userName, String password) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				DbUtil.setDbUser(userName, password);
				Connection conn = DbUtil.connect();
				if(conn != null) {
					Platform.runLater(()->callback.onLoginUpdated(true));
					DbUtil.disconnect(conn);
				} else {
					Platform.runLater(()->callback.onLoginUpdated(false));
				}				
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void queryRecord(RecordType type, int creatorId, long fromTime, String noteSearchStr, int num) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				RecordType[] types;
				if(type == RecordType.ALL) {
					types = RecordType.values();
				} else {
					types = new RecordType[] {type};
				}
				JSONArray basicInfoJsons = RecordWebUtil.downloadList(types, creatorId, fromTime, noteSearchStr, num);
				Platform.runLater(()->callback.onRecordBasicInfoListUpdated(basicInfoJsons));
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void downloadRecord(RecordType type, long createTime, String devAddress) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject json = RecordWebUtil.download(type, createTime, devAddress);
				Platform.runLater(()->callback.onRecordDownloaded(json));
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getAccountInfo(int creatorId) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Account account = new Account(creatorId);
				account.retrieve();
				Platform.runLater(()->callback.onAccountInfoDownloaded(account.toJson()));
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
