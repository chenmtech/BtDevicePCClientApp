package application;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

public class RecordPane extends GridPane {
	//private static final Node[] HEADER;
	private static final String[] HEADER_TITLES = {"记录类型", "版本号", "创建者账户", "创建时间", "设备地址", "记录时长", "备注", "操作"};
	private final List<JSONObject> recordJsons = new ArrayList<>();
	private final Main main;
	
/*	static {
		List<Node> nodes = new ArrayList<>();
		for(String str : HEADER_TITLES) {
			Label label = new Label(str);
			label.setTextAlignment(TextAlignment.CENTER);
			nodes.add(label);
		}
		HEADER = nodes.toArray(new Node[0]);
	}*/
	
	public RecordPane(Main main) {
		this.main = main;
		this.setAlignment(Pos.BOTTOM_LEFT);
		this.setGridLinesVisible(true);
		setStyle("-fx-border-color:red");
		
		List<Node> nodes = new ArrayList<>();
		for(String str : HEADER_TITLES) {
			Label label = new Label(str);
			label.setTextAlignment(TextAlignment.CENTER);
			label.setAlignment(Pos.CENTER);
			label.prefWidthProperty().bind(widthProperty().divide(8));
			label.maxWidthProperty().bind(widthProperty().divide(4));
			nodes.add(label);
		}
		addRow(0, nodes.toArray(new Node[0]));
	}
	
	public void addRecord(JSONObject json) {
		recordJsons.add(json);
		System.out.println(json.toString());
		int recordTypeCode = json.getInt("recordTypeCode");
		RecordType type = RecordType.fromCode(recordTypeCode);
		String ver = json.getString("ver");
		String creatorPlat = json.getString("creatorPlat");
		String creatorId = json.getString("creatorId");
		String creator = creatorPlat+creatorId;
		long createTime = json.getLong("createTime");
		String devAddress = json.getString("devAddress");
		String note = json.getString("note");
		String recordSecondStr = "无";
		if(json.has("recordSecond")) {
			int recordSecond = json.getInt("recordSecond");
			recordSecondStr = secondToTime(recordSecond);
		}
		Button btnGet = new Button("导出记录");
		btnGet.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.saveRecord(type, createTime, devAddress);
			}
		});
		List<Node> nodes = new ArrayList<>();
		nodes.add(new Label(type.getName()));
		nodes.add(new Label(ver));
		TextField tfCreator = new TextField(creator);
		tfCreator.setEditable(false);
		nodes.add(tfCreator);
		DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd hh:mm");
		String dateStr = dateFmt.format(new Date(createTime)) + '\n' + createTime;
		nodes.add(new Label(dateStr));
		nodes.add(new Label(devAddress));
		nodes.add(new Label(recordSecondStr));
		TextArea taNote = new TextArea(note);
		taNote.setEditable(false);
		taNote.setWrapText(true);
		taNote.prefWidthProperty().bind(widthProperty().divide(4));
		taNote.setMaxHeight(40);
		nodes.add(taNote);
		nodes.add(btnGet);
		addRow(recordJsons.size(), nodes.toArray(new Node[0]));
	}
	
	public void clearContent() {
		getChildren().remove(HEADER_TITLES.length+1, getChildren().size());
		recordJsons.clear();
	}
	
	public static String secondToTime(int second) {
		if (second<60) {
			return second+"秒";
		}else if (second>60&&second<3600) {
			int m = second/60;
			int s = second%60;
			return m+"分"+s+"秒";
		}else {
			int h = second/3600;
    		int m = (second%3600)/60;
    		int s = (second%3600)%60;
    		return h+"小时"+m+"分"+s+"秒";
		}
	}
}
