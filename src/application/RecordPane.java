package application;

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
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

public class RecordPane extends GridPane {
	private static final Node[] HEADER;
	private static final String[] HEADER_TITLES = {"记录类型", "版本号", "创建者账户", "创建时间", "设备地址", "备注", "记录时长(秒)", "操作"};
	private final List<JSONObject> recordJsons = new ArrayList<>();
	private final Main main;
	
	static {
		List<Node> nodes = new ArrayList<>();
		for(String str : HEADER_TITLES) {
			Label label = new Label(str);
			label.setTextAlignment(TextAlignment.CENTER);
			nodes.add(label);
		}
		HEADER = nodes.toArray(new Node[0]);
	}
	
	public RecordPane(Main main) {
		this.main = main;
		this.setAlignment(Pos.CENTER);
		this.setGridLinesVisible(true);
		addRow(0, HEADER);
	}
	
	public void addRecord(JSONObject json) {
		recordJsons.add(json);
		System.out.println(json.toString());
		int recordTypeCode = json.getInt("recordTypeCode");
		RecordType type = RecordType.getType(recordTypeCode);
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
			recordSecondStr = recordSecond + "";
		}
		List<Node> nodes = new ArrayList<>();
		nodes.add(new Label(type.getName()));
		nodes.add(new Label(ver));
		nodes.add(new Label(creator));
		nodes.add(new Label(new Date(createTime).toLocaleString()));
		nodes.add(new Label(devAddress));
		nodes.add(new Label(note));
		nodes.add(new Label(recordSecondStr));
		Button btnGet = new Button("打开记录");
		nodes.add(btnGet);
		btnGet.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.downloadRecord(type, createTime, devAddress);
			}
		});
		addRow(recordJsons.size(), nodes.toArray(new Node[0]));
	}
	
	public void clearContent() {
		getChildren().remove(HEADER.length, getChildren().size());
		recordJsons.clear();
	}
}
