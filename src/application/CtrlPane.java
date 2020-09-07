package application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import com.cmtech.web.btdevice.RecordType;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class CtrlPane extends HBox{
	
	public CtrlPane(Main main) {
		setAlignment(Pos.CENTER);
		setSpacing(10);
		setPadding(new Insets(10,10,10,10));
		
		getChildren().add(new Label("记录类型："));
		ComboBox<RecordType> cboType = new ComboBox<>();
		cboType.getItems().addAll(RecordType.ALL, RecordType.ECG, RecordType.HR, RecordType.THERMO, RecordType.EEG);
		cboType.setValue(cboType.getItems().get(0));
		getChildren().add(cboType);
		
		getChildren().add(new Label("创建者账号："));
		TextField tfCreator = new TextField();
		tfCreator.setPrefColumnCount(10);
		getChildren().add(tfCreator);
		
		getChildren().add(new Label("日期："));
		DatePicker fromDate = new DatePicker(LocalDate.now());
		getChildren().add(fromDate);
		
		getChildren().add(new Label("备注包含："));
		TextField tfNoteSearchStr = new TextField();
		tfNoteSearchStr.setPrefColumnCount(10);
		getChildren().add(tfNoteSearchStr);
		
		Button btnLogin = new Button("登录");
		Button btnReload = new Button("开始查询");
		Button btnLoadNext = new Button("继续查询");
		getChildren().addAll(btnLogin, btnReload, btnLoadNext);
		
		btnLogin.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.login();
			}
		});
		
		btnReload.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDate localDate = fromDate.getValue();
				Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
				Calendar cal = Calendar.getInstance();  
			    cal.setTime(date);
			    cal.set(Calendar.HOUR_OF_DAY, 24);
				long searchTime = cal.getTimeInMillis();
				main.reload(cboType.getValue(), tfCreator.getText(), searchTime, tfNoteSearchStr.getText());
			}
		});
		
		btnLoadNext.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.loadNext(cboType.getValue(), tfCreator.getText(), tfNoteSearchStr.getText());
			}
		});
	}

}
