package application;

import java.sql.Connection;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;
import com.cmtech.web.dbUtil.DbUtil;
import com.cmtech.web.dbUtil.RecordDbUtil;

import javafx.application.Platform;

public class DbOperator {
	public static final String DEFAULT_DB_ADDRESS = "203.195.137.198:3306";
	private final Main main;
	
	public DbOperator(Main main) {
		this(main, DEFAULT_DB_ADDRESS);
	}
	
	public DbOperator(Main main, String dbAddress) {
		this.main = main;
		DbUtil.setDbAddress(dbAddress);
	}
	
	public void testConnect() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Connection conn = DbUtil.connect();
				if(conn != null) {
					Platform.runLater(()->main.updateStatusInfo("数据库连接正常"));
					DbUtil.disconnect(conn);
				} else {
					Platform.runLater(()->main.updateStatusInfo("数据库连接失败"));
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
	
	public void queryRecord(RecordType type, String creatorPlat, String creatorId, long fromTime, String noteSearchStr, int num) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				JSONArray basicInfoJsons = RecordDbUtil.downloadBasicInfo(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
				if(basicInfoJsons != null) {
					Platform.runLater(()->main.updateRecordBasicInfoList(basicInfoJsons));
				} else {
					Platform.runLater(()->main.updateStatusInfo("找不到记录"));
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
	
	public void downloadRecord(RecordType type, long createTime, String devAddress) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject json = RecordDbUtil.download(type, createTime, devAddress);
				if(json != null) {
					Platform.runLater(()->main.openRecord(json));
				} else {
					Platform.runLater(()->main.updateStatusInfo("下载记录失败"));
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

}
