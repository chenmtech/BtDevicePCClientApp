package application;
	
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import ecgprocess.EcgProcessor;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import util.FileDialogUtil;


public class Main extends Application implements IDbOperationCallback{
	private static final String VER = "0.1";
	private static final String TITLE = "欢迎使用康明智联PC客户端App Ver:" + VER;
	public static final Account ACCOUNT = new Account();
	private Stage primaryStage;
	private final InfoPane infoPane = new InfoPane();
	private final RecordPane recordPane = new RecordPane(this);
	private DbOperator dbOperator;
	private long fromTime = new Date().getTime();
	
	@Override
	public void start(Stage primaryStage) {
		try {
			this.primaryStage = primaryStage;
			infoPane.setInfo("请先登录。");
			CtrlPane ctrlPane = new CtrlPane(this);
			
			BorderPane root = new BorderPane();
			root.setTop(infoPane);
			root.setCenter(new ScrollPane(recordPane));
			root.setBottom(ctrlPane);
			root.setPadding(new Insets(10,10,10,10));
			
			Scene scene = new Scene(root,1600,800);
			recordPane.prefWidthProperty().bind(scene.widthProperty());
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
		final LoginStage loginStage = new LoginStage();
		loginStage.show();
	}
	
	public void reload(RecordType type, String creator, long searchTime, String noteSearchStr) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
			
		recordPane.clearContent();
		String creatorPlat = "";
		String creatorId = "";
		if(!"".equals(creator)) {
			creatorPlat = "PH";
			creatorId = "86"+creator;
		}
		fromTime = searchTime;

		int num = 20;
		dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr.trim(), num);
	}
	
	public void loadNext(RecordType type, String creator, String noteSearchStr) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
		
		String creatorPlat = "";
		String creatorId = "";
		if(!"".equals(creator)) {
			creatorPlat = "PH";
			creatorId = "86"+creator;
		}
		
		int num = 20;
		dbOperator.queryRecord(type, creatorPlat, creatorId, fromTime, noteSearchStr.trim(), num);
	}
	
	public void saveRecord(RecordType type, long createTime, String devAddress) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
		
		dbOperator.downloadRecord(type, createTime, devAddress);
	}
	
	public void processRecord() {
		if(!ACCOUNT.isLogin()) {
			//login();
			//return;
		}

        FileChooser.ExtensionFilter filter =  new FileChooser.ExtensionFilter("JSON文件","*.json");
        File file = FileDialogUtil.openFileDialog(primaryStage, true, null, null, filter);
        
        if(file != null){
        	try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
        		String tempString = null;
                StringBuilder builder = new StringBuilder();
                // 一次读入一行，直到读入null为文件结束
                while ((tempString = reader.readLine()) != null) {
                    builder.append(tempString);
                }
                JSONObject json = new JSONObject(builder.toString());
                //System.out.println(json.toString());
                RecordType type = RecordType.fromCode(json.getInt("recordTypeCode"));
                if(type != RecordType.ECG) {
                	infoPane.setInfo("对不起，暂时只能处理心电信号。");
                	return;
                }
                String ecgStr = json.getString("ecgData");
                String[] ecgDataStr = ecgStr.split(",");
                List<Short> ecgData = new ArrayList<>();
                for(String str : ecgDataStr) {
                	ecgData.add(Short.parseShort(str));
                }
                int sampleRate = json.getInt("sampleRate");
                
                EcgProcessor ecgProc = new EcgProcessor();
                ecgProc.process(ecgData, sampleRate);
    			
        		String srcFileName = file.getAbsolutePath();
        		String tmpFileName = srcFileName.substring(0, srcFileName.lastIndexOf('.'));
        		
        		// save review result to json file
        		String reviewJsonFileName = tmpFileName + "-review.json";
        		File reviewFile = new File(reviewJsonFileName);
        		try(PrintWriter reviewWriter = new PrintWriter(reviewFile)) {
        			reviewWriter.print(ecgProc.getReviewResult().toString());
        		}
        		
        		// save the string of the segment data to txt file
/*        		String txtFileName = tmpFileName + ".txt";
        		File txtFile = new File(txtFileName);
        		try(PrintWriter txtWriter = new PrintWriter(txtFile)) {
        			txtWriter.print(ecgProc.getSegEcgDataString());
        		}*/
        		
    			infoPane.setInfo("已将处理结果保存到文件中。");
        	} catch (IOException e) {
        		infoPane.setInfo("处理信号失败");
				e.printStackTrace();
			}
        }
	}
	
	public void diagnoseRecord() {
		if(!ACCOUNT.isLogin()) {
			//login();
			//return;
		}
		
		FileChooser.ExtensionFilter filter =  new FileChooser.ExtensionFilter("JSON文件","*.json");
        File file = FileDialogUtil.openFileDialog(primaryStage, true, null, null, filter);

        if(file != null){
        	try {
        		Process proc;
            	String[] args = new String[] { "d:\\python38\\python.exe", "D:\\PythonCode\\ecgDiagnose.py", "D:\\pythoncode\\my_model2-9222.h5", file.getAbsolutePath()};
                proc = Runtime.getRuntime().exec(args);// 执行py文件
                //用输入输出流来截取结果
                BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }
                BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                String errStr = null;
                while ((errStr = err.readLine()) != null) {
                    System.out.println(errStr);
                }
                in.close();
                err.close();
                proc.waitFor();
                JSONObject resultJson = new JSONObject(builder.toString());
                JSONArray resultList = (JSONArray) resultJson.get("Result");
                System.out.println(resultList);
                int length = Math.min(100, resultJson.toString().length());
                infoPane.setInfo(resultJson.toString().substring(0, length));
            } catch (Exception e) {
                e.printStackTrace();
                infoPane.setInfo("诊断错误：" + e.getMessage());
            }
        } else {
        	return;
        }

	}
	
	@Override
	public void onLoginUpdated(boolean success) {
		if(success)
			infoPane.setInfo("登录成功");
		else
			infoPane.setInfo("登录失败");
		ACCOUNT.setLogin(success);
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
			infoPane.setInfo("下载记录失败");
		} else {
			String defaultName = createJsonFileName(RecordType.getName(json.getInt("recordTypeCode")), json.getLong("createTime") , json.getString("devAddress"));
            FileChooser.ExtensionFilter filter =  new FileChooser.ExtensionFilter("JSON文件","*.json");
            File file = FileDialogUtil.openFileDialog(primaryStage, false, null, defaultName, filter);
            if(file != null){
            	try(PrintWriter writer = new PrintWriter(file)) {
            		writer.print(json.toString()); 
            		infoPane.setInfo("记录已保存");
            	} catch (FileNotFoundException e) {
            		infoPane.setInfo("保存记录失败");
					e.printStackTrace();
				}
            }
		}
	}
	
	private String createJsonFileName(String type, long createTime, String devAddress) {
		return type + createTime + "(" +devAddress.replace(":", "-") + ")";		
	}
	
	private class LoginStage extends Stage{	
		LoginStage() {
			GridPane pane = new GridPane();
			pane.setAlignment(Pos.CENTER);
			pane.setPadding(new Insets(10,10,10,10));
			pane.setHgap(5);
			pane.setVgap(5);
			pane.add(new Label("用户名："), 0, 0);
			TextField tfName = new TextField();
			pane.add(tfName, 1, 0);
			pane.add(new Label("密码："), 0, 1);
			PasswordField pfPwd = new PasswordField();
			pane.add(pfPwd, 1, 1);
			Button btnOK = new Button("确定");
			btnOK.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					String name = tfName.getText();
					String password = pfPwd.getText();
					ACCOUNT.setName(name);
					ACCOUNT.setPassword(password);
					ACCOUNT.setLogin(false);
					dbOperator.testConnect(name, password);
					close();
				}
			});
			pane.add(btnOK, 1, 2);
			GridPane.setHalignment(btnOK, HPos.RIGHT);
			Scene scene = new Scene(pane);
			setScene(scene);
			setTitle("请登录");
		}
	}
	
/*	private void outputEcgDataToTxtFile(PrintWriter writer, List<Short> ecgData, JSONObject json) {
		JSONArray beatBegin = (JSONArray)json.get("BeatBegin");
		for(int i = 0; i < beatBegin.length()-1; i++) {
			long begin = beatBegin.getLong(i);
			long end = beatBegin.getLong(i+1);
			int length = (int)(end - begin);
			
			int fill = 0;
			if(length > 250) {
				begin += (length-250)/2;
				end = begin + 250;
			} else if(length < 250) {
				fill = 250-length;
			}
			long pos;
			for(pos = begin; pos <end; pos++) {
				writer.print(ecgData.get((int)pos));
				writer.print(' ');
			}
			int lastNum = ecgData.get((int)(pos-1));
			for(int j = 0; j < fill; j++) {
				writer.print(lastNum);
				writer.print(' ');
			}
			writer.print("\r\n");
		}
	}*/
}
