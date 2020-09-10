package application;
	
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

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
import qrsdetbyhamilton.QrsDetectorWithQRSInfo;


public class Main extends Application implements IDbOperationCallback{
	private static final String TITLE = "欢迎使用康明智联PC客户端";
	private static final String DEFAULT_JSON_FILE_DIR = "F:\\360云盘\\360同步文件夹\\科研\\data\\jsonfile";
	public static final Account ACCOUNT = new Account();
	private Stage primaryStage;
	private final InfoPane infoPane = new InfoPane();;
	private final RecordPane recordPane = new RecordPane(this);;
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
			
			Scene scene = new Scene(root,1200,800);
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
		
		FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择需处理的信号JSON文件");
        //设置将当前目录作为初始显示目录
        fileChooser.setInitialDirectory(new File(DEFAULT_JSON_FILE_DIR));
        //创建文件选择过滤器
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("JSON文件","*.json");
        //设置文件过滤器
        fileChooser.getExtensionFilters().add(filter);
        File file = fileChooser.showOpenDialog(primaryStage);
        if(file != null){
        	try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
        		String tempString = null;
                StringBuilder jsonStrBuilder = new StringBuilder();
                // 一次读入一行，直到读入null为文件结束
                while ((tempString = reader.readLine()) != null) {
                    jsonStrBuilder.append(tempString);
                }
                JSONObject json = new JSONObject(jsonStrBuilder.toString());
                System.out.println(json.toString());
                RecordType type = RecordType.fromCode(json.getInt("recordTypeCode"));
                if(type != RecordType.ECG) {
                	infoPane.setInfo("暂时无法处理该类型信号。");
                	return;
                }
                String ecgStr = json.getString("ecgData");
                String[] ecgDataStr = ecgStr.split(",");
                List<Short> ecgData = new ArrayList<>();
                for(String str : ecgDataStr) {
                	ecgData.add(Short.parseShort(str));
                }
                int sampleRate = json.getInt("sampleRate");
                final JSONObject[] jsonArr = new JSONObject[1];
                Thread thProcess = new Thread(new Runnable() {
        			@Override
        			public void run() {
        				jsonArr[0] = getEcgQrsAndBeatBeginPos(ecgData, sampleRate);
        			}			
        		});
        		thProcess.start();
    			try {
					thProcess.join();
				} catch (InterruptedException e1) {
        			infoPane.setInfo("处理被中断。");
					e1.printStackTrace();
					return;
				}
        		String srcFileName = file.getAbsolutePath();
        		String tmpFile = srcFileName.substring(0, srcFileName.lastIndexOf('.'));
        		String tgtJsonFileName = tmpFile + "-review.json";
        		String tgtTxtFileName = tmpFile + ".txt";
        		File outJson = new File(tgtJsonFileName);
        		File outTxt = new File(tgtTxtFileName);
        		try(PrintWriter jsonWriter = new PrintWriter(outJson); PrintWriter txtWriter = new PrintWriter(outTxt)) {
        			jsonWriter.print(jsonArr[0].toString());
        			
        			//outputShortEcgDataToTXTFile(txtWriter, ecgData, jsonArr[0]);
        			
        			List<Float> out = normalizeEcgData(ecgData, jsonArr[0]);
        			outputFloatEcgDataToTXTFile(txtWriter, out, jsonArr[0]);
        			
        			infoPane.setInfo("已将处理结果保存到文件中。");
        		}
        	} catch (IOException e) {
        		infoPane.setInfo("处理信号失败");
				e.printStackTrace();
			}
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
			FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("文件保存对话框");
            //设置将当前目录作为初始显示目录
            fileChooser.setInitialDirectory(new File(DEFAULT_JSON_FILE_DIR));
            String defaultName = createJsonFileName(RecordType.getName(json.getInt("recordTypeCode")), json.getLong("createTime") , json.getString("devAddress"));
            fileChooser.setInitialFileName(defaultName);
            //创建文件选择过滤器
            FileChooser.ExtensionFilter filter =
                    new FileChooser.ExtensionFilter("JSON文件","*.json");
            //设置文件过滤器
            fileChooser.getExtensionFilters().add(filter);
            File file = fileChooser.showSaveDialog(primaryStage);
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
	
	private JSONObject getEcgQrsAndBeatBeginPos(List<Short> ecgData, int sampleRate) {
		if(ecgData == null || ecgData.isEmpty()) {
			return null;
		}
		QrsDetectorWithQRSInfo detector = new QrsDetectorWithQRSInfo(sampleRate);
		for(Short datum : ecgData) {
			detector.outputRRInterval((int)datum);
		}
		List<Long> qrsPos = detector.getQrsPositions();
		List<Integer> rrInterval = detector.getRrIntervals();
		List<Long> beatBegin = new ArrayList<>();
		for(int i = 0; i < rrInterval.size(); i++) {
			beatBegin.add(qrsPos.get(i+1) - Math.round(rrInterval.get(i)*2.0/5));
		}
		JSONObject json = new JSONObject();
		json.put("QrsPos", qrsPos);
		json.put("BeatBegin", beatBegin);
		System.out.println(json);
		return json;
	}
	
	private List<Float> normalizeEcgData(List<Short> ecgData, JSONObject json) {
		List<Float> out = new ArrayList<>();
		
		for(Short d : ecgData) {
			out.add((float)d);
		}
		
		List<Float> oneBeat =  new ArrayList<>();
		JSONArray beatBegin = (JSONArray)json.get("BeatBegin");
		for(int i = 0; i < beatBegin.length()-1; i++) {
			for(long begin = beatBegin.getLong(i); begin < beatBegin.getLong(i+1); begin++) {
				oneBeat.add(out.get((int)begin));
			}
			System.out.println(oneBeat);
			
			float ave = average(oneBeat);
			float std = standardDiviation(oneBeat);
			
			for(int ii = 0; ii < oneBeat.size(); ii++) {
				oneBeat.set(ii, (oneBeat.get(ii)-ave)/std);
			}
			
			System.out.println("" + ave + " " + std);
			System.out.println(oneBeat);
			
			for(long begin = beatBegin.getLong(i),  ii = 0; begin < beatBegin.getLong(i+1); begin++, ii++) {
				out.set((int)begin, oneBeat.get((int)ii));
			}
			
			oneBeat.clear();
		}
		return out;
	}
	
	 //均值
	 public static float average(List<Float> x) { 
		  int m=x.size();
		  float sum=0;
		  for(int i=0;i<m;i++){//求和
			  sum+=x.get(i);
		  }
		 return sum/m; 
	 }
	
	 //标准差σ=sqrt(s^2)
	 public static float standardDiviation(List<Float> x) { 
		  int m=x.size();
		  float sum=0;
		  for(int i=0;i<m;i++){//求和
			  sum+=x.get(i);
		  }
		  double dAve=sum/m;//求平均值
		  double dVar=0;
		  for(int i=0;i<m;i++){
		      dVar+=(x.get(i)-dAve)*(x.get(i)-dAve);
		  }
	   //reture Math.sqrt(dVar/(m-1));
		return (float)Math.sqrt(dVar/(m-1));    
	 }
	
	private void outputShortEcgDataToTXTFile(PrintWriter writer, List<Short> ecgData, JSONObject json) {
		JSONArray beatBegin = (JSONArray)json.get("BeatBegin");
		for(int i = 0; i < beatBegin.length()-1; i++) {
			int length = (int)(beatBegin.getLong(i+1) - beatBegin.getLong(i));
			long toPos = beatBegin.getLong(i+1);
			int fill = 0;
			if(length > 250) {
				toPos = beatBegin.getLong(i) + 250;
			} else if(length < 250) {
				fill = 250-length;
			}
			long pos;
			for(pos = beatBegin.getLong(i); pos <toPos; pos++) {
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
	}
	
	private void outputFloatEcgDataToTXTFile(PrintWriter writer, List<Float> ecgData, JSONObject json) {
		JSONArray beatBegin = (JSONArray)json.get("BeatBegin");
		for(int i = 0; i < beatBegin.length()-1; i++) {
			int length = (int)(beatBegin.getLong(i+1) - beatBegin.getLong(i));
			long toPos = beatBegin.getLong(i+1);
			int fill = 0;
			if(length > 250) {
				toPos = beatBegin.getLong(i) + 250;
			} else if(length < 250) {
				fill = 250-length;
			}
			long pos;
			for(pos = beatBegin.getLong(i); pos <toPos; pos++) {
				writer.printf("%.3f", ecgData.get((int)pos));
				writer.print(' ');
			}
			float lastNum = ecgData.get((int)(pos-1));
			for(int j = 0; j < fill; j++) {
				writer.printf("%.3f", lastNum);
				writer.print(' ');
			}
			writer.print("\r\n");
		}
	}
}
