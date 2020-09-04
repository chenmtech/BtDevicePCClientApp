package application;
	
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;


public class Main extends Application {
	private Text statusInfo;
	private GridPane recordPane;
	private DbOperator dbOperator;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FlowPane statusPane = new FlowPane();
			statusPane.setAlignment(Pos.CENTER);
			statusInfo = new Text(0,0,"null");
			statusPane.getChildren().add(statusInfo);
			
			recordPane = new GridPane();
			
			HBox btnBox = new HBox();
			btnBox.setAlignment(Pos.CENTER);
			btnBox.setSpacing(10);
			Button btnConnect = new Button("连接");
			Button btnQuery = new Button("获取记录");
			btnBox.getChildren().addAll(btnConnect, btnQuery);
			
			btnConnect.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					dbOperator.testConnect();
				}
			});
			
			btnQuery.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					dbOperator.queryRecord();
				}
			});
			
			BorderPane root = new BorderPane();
			root.setTop(statusPane);
			root.setCenter(recordPane);
			root.setBottom(btnBox);
			root.setPadding(new Insets(10,10,10,10));
			Scene scene = new Scene(root,400,400);
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
	
	public void updateText(String info) {
		statusInfo.setText(info);
	}
	
	public void updateRecordBasicInfoList(JSONArray basicInfos) {
		recordPane.getChildren().clear();
		statusInfo.setText("找到" + basicInfos.length() + "条记录");
		for(int i = 0; i < basicInfos.length(); i++) {
			JSONObject json = (JSONObject) basicInfos.get(i);
			Text recordInfo = new Text(0,0,json.toString());
			recordPane.add(recordInfo, 0, i);
		}
	}
}
