package application;

import java.sql.Connection;
import java.util.Date;

import org.json.JSONArray;

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
					Platform.runLater(()->main.updateText("数据库连接正常"));
					DbUtil.disconnect(conn);
				} else {
					Platform.runLater(()->main.updateText("数据库连接失败"));
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
	
	public void queryRecord() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				RecordType type = RecordType.ALL;
				String creatorPlat = "PH";
				String creatorId = "8615019187404";
				long fromTime = new Date().getTime();
				String noteSearchStr = "";
				int num = 20;
				JSONArray basicInfoJsons = RecordDbUtil.downloadBasicInfo(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
				if(basicInfoJsons != null) {
					Platform.runLater(()->main.updateRecordBasicInfoList(basicInfoJsons));
				} else {
					Platform.runLater(()->main.updateText("找不到记录"));
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
