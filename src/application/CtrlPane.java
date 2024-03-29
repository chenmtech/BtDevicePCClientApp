package application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import com.cmtech.web.btdevice.RecordType;
import static com.cmtech.web.MyConstant.*;

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
		cboType.getItems().addAll(RecordType.ALL, RecordType.ECG, RecordType.HR, RecordType.THERMO, RecordType.EEG, RecordType.PPG, RecordType.PTT);
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
		Button btnAFDetect = new Button("房颤检测");
		Button btnProcessSignal = new Button("处理ECG信号");
		Button btnDiagnose = new Button("诊断ECG信号");
		Button btnAutoProcessDiagnoseRequest = new Button("自动处理诊断请求");
		Button btnConfig = new Button("设置环境");
		//getChildren().addAll(btnLogin, btnReload, btnLoadNext);
		
		getChildren().addAll(btnLogin, btnReload, btnLoadNext, 
				btnAFDetect, btnProcessSignal, btnDiagnose, btnAutoProcessDiagnoseRequest, btnConfig);
		
		// 登录
		btnLogin.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.login();
			}
		});
		
		// 开始查询
		btnReload.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				String creator = tfCreator.getText();
				int creatorId = ("".equals(creator)) ? INVALID_ID : Integer.parseInt(creator);
				LocalDate localDate = fromDate.getValue();
				Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
				Calendar cal = Calendar.getInstance();  
			    cal.setTime(date);
			    cal.set(Calendar.HOUR_OF_DAY, 24);
				long searchTime = cal.getTimeInMillis();
				main.reload(cboType.getValue(), creatorId, searchTime, tfNoteSearchStr.getText());
			}
		});
		
		// 继续查询
		btnLoadNext.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				String creator = tfCreator.getText();
				int creatorId = ("".equals(creator)) ? INVALID_ID : Integer.parseInt(creator);
				main.loadNext(cboType.getValue(), creatorId, tfNoteSearchStr.getText());
			}
		});
		
		// 房颤检测
		btnAFDetect.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.AFDetect();
			}
		});
		
		btnProcessSignal.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.processRecord();
			}
		});
		
		btnDiagnose.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.diagnoseRecord();
			}
		});
		
		// 自动处理远程诊断请求
		btnAutoProcessDiagnoseRequest.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.autoProcessDiagnoseRequest();
			}
		});
		
		btnConfig.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.config();
			}
		});
	}

}
