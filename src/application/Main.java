package application;
	
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;


public class Main extends Application implements IDbOperationCallback{
	private static final String TITLE = "欢迎使用康明智联PC客户端";
	private final InfoPane infoPane = new InfoPane();;
	private final RecordPane recordPane = new RecordPane(this);;
	private DbOperator dbOperator;
	private long fromTime = new Date().getTime();
	
	@Override
	public void start(Stage primaryStage) {
		try {
			infoPane.setInfo("等待操作...");
			
			CtrlPane ctrlPane = new CtrlPane(this);
			
			BorderPane root = new BorderPane();
			root.setTop(infoPane);
			root.setCenter(new ScrollPane(recordPane));
			root.setBottom(ctrlPane);
			root.setPadding(new Insets(10,10,10,10));
			Scene scene = new Scene(root,800,800);
			primaryStage.setScene(scene);
			primaryStage.setTitle(TITLE);
			primaryStage.show();
			
			dbOperator  = new DbOperator(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void login() {
		dbOperator.testConnect();
	}
	
	public void reload() {
		recordPane.clearContent();
		RecordType type = RecordType.ALL;
		String creatorPlat = "";
		String creatorId = "";
		String noteSearchStr = "";
		int num = 20;
		fromTime = new Date().getTime();
		dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
	}
	
	public void loadNext() {
		RecordType type = RecordType.ALL;
		String creatorPlat = "";
		String creatorId = "";
		String noteSearchStr = "";
		int num = 20;
		dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
	}
	
	public void downloadRecord(RecordType type, long createTime, String devAddress) {
		dbOperator.downloadRecord(type, createTime, devAddress);
	}
	
	@Override
	public void onLoginUpdated(boolean success) {
		if(success)
			infoPane.setInfo("登录成功");
		else
			infoPane.setInfo("登录失败");
	}
	
	@Override
	public void onRecordBasicInfoListUpdated(JSONArray basicInfos) {
		if(basicInfos == null) {
			infoPane.setInfo("加载失败");
		} else {
			if(basicInfos.length() == 0) {
				infoPane.setInfo("没有记录可加载。");
			} else {
				infoPane.setInfo("找到" + basicInfos.length() + "条记录");
				for(int i = 0; i < basicInfos.length(); i++) {
					JSONObject json = (JSONObject) basicInfos.get(i);
					recordPane.addRecord(json);
					long createTime = json.getLong("createTime");
					if(fromTime > createTime)
						fromTime = createTime;
				}
			}
		}
	}
	
	@Override
	public void onRecordDownloaded(JSONObject json) {
		if(json == null) {
			infoPane.setInfo("打开记录失败");
		} else {
			System.out.println(json.toString());
			infoPane.setInfo("打开记录成功");
		}
	}
}
