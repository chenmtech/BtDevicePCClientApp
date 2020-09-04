package application;
	
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;


public class Main extends Application {
	private StatusInfoPane infoPane;
	private RecordPane recordPane;
	private DbOperator dbOperator;
	private long fromTime = new Date().getTime();;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			infoPane = new StatusInfoPane("欢迎使用康明智联PC客户端");
			
			recordPane = new RecordPane(this);
			
			HBox btnBox = new HBox();
			btnBox.setAlignment(Pos.CENTER);
			btnBox.setSpacing(10);
			Button btnConnect = new Button("测试连接");
			Button btnRestart = new Button("重新加载");
			Button btnQuery = new Button("加载记录");
			btnBox.getChildren().addAll(btnConnect, btnRestart, btnQuery);
			
			btnConnect.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					dbOperator.testConnect();
				}
			});
			
			btnRestart.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					recordPane.clear();
					RecordType type = RecordType.ALL;
					String creatorPlat = "";
					String creatorId = "";
					String noteSearchStr = "";
					int num = 20;
					fromTime = new Date().getTime();
					dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
				}
			});
			
			btnQuery.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					RecordType type = RecordType.ALL;
					String creatorPlat = "";
					String creatorId = "";
					String noteSearchStr = "";
					int num = 20;
					dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr, num);
				}
			});
			
			BorderPane root = new BorderPane();
			root.setTop(infoPane);
			root.setCenter(recordPane);
			root.setBottom(btnBox);
			root.setPadding(new Insets(10,10,10,10));
			Scene scene = new Scene(root,800,800);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			
			dbOperator  = new DbOperator(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void updateStatusInfo(String info) {
		infoPane.setInfo(info);
	}
	
	public void updateRecordBasicInfoList(JSONArray basicInfos) {
		infoPane.setInfo("找到" + basicInfos.length() + "条记录");
		for(int i = 0; i < basicInfos.length(); i++) {
			JSONObject json = (JSONObject) basicInfos.get(i);
			recordPane.addRecord(json);
			long createTime = json.getLong("createTime");
			if(fromTime > createTime)
				fromTime = createTime;
		}
	}
	
	public void downloadRecord(RecordType type, long createTime, String devAddress) {
		dbOperator.downloadRecord(type, createTime, devAddress);
	}
	
	public void openRecord(JSONObject json) {
		System.out.println(json.toString());
		infoPane.setInfo("打开记录成功");
	}
}
