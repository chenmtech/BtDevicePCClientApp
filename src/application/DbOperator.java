package application;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;
import com.cmtech.web.dbUtil.DbUtil;
import com.cmtech.web.dbUtil.RecordWebUtil;
import com.cmtech.web.btdevice.Account;

import javafx.application.Platform;

/**
 * 数据库操作器类
 * 用于提供应用层所需的数据库操作
 * @author gdmc
 *
 */
public class DbOperator {
	// 缺省的数据库服务器地址
	private static final String DEFAULT_DB_ADDRESS = "203.195.137.198:3306";
	
	// 数据库操作回调接口对象，为应用层提供数据库操作的回调处理
	private final IDbOperationCallback callback;
	
	public DbOperator(IDbOperationCallback callback) {
		this(callback, DEFAULT_DB_ADDRESS);
	}
	
	public DbOperator(IDbOperationCallback callback, String dbAddress) {
		this.callback = callback;
		DbUtil.setDbAddress(dbAddress);
	}
	
	/**
	 * 测试用户名和密码是否能建立连接
	 * @param userName：用户名
	 * @param password：密码
	 */
	public void testConnect(String userName, String password) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				DbUtil.setDbUser(userName, password);
				Connection conn = DbUtil.connect();
				if(conn != null) {
					Platform.runLater(()->callback.onLogin(true));
					DbUtil.disconnect(conn);
				} else {
					Platform.runLater(()->callback.onLogin(false));
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
	
	/**
	 * 下载符合条件的BasicRecords
	 * @param type：记录类型
	 * @param creatorId：记录创建者ID
	 * @param fromTime：起始时间
	 * @param filterStr：过滤字符串
	 * @param num：记录数
	 */
	public void downloadBasicRecords(RecordType type, int creatorId, long fromTime, String filterStr, int num) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				RecordType[] types;
				if(type == RecordType.ALL) {
					types = RecordType.values();
				} else {
					types = new RecordType[] {type};
				}
				JSONArray basicRecords = RecordWebUtil.downloadBasicRecords(types, creatorId, fromTime, filterStr, num);
				Platform.runLater(()->callback.onBasicRecordsDownloaded(basicRecords));
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
	
	/**
	 * 下载指定的记录
	 * @param type：记录类型
	 * @param createTime：起始时间
	 * @param devAddress：设备地址
	 */
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

	/**
	 * 获取账户信息
	 * @param creatorId：账户ID
	 */
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
