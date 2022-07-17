package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;
import com.cmtech.web.connection.ConnectionPoolFactory;
import com.cmtech.web.dbUtil.RecordWebUtil;

import afdetect.AFDetectExecutor;
import ecgprocess.EcgPreProcessor;
import ecgprocess.MyEcgDiagnoseModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
	private MyProperties properties;
	private long fromTime = new Date().getTime();
	private Thread thAutoProcess;
	
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
			
			Scene scene = new Scene(root,600,400);
			recordPane.prefWidthProperty().bind(scene.widthProperty());
			primaryStage.setScene(scene);
			primaryStage.setTitle(TITLE);
			primaryStage.setMaximized(true);//全屏显示
			primaryStage.show();
			
			dbOperator  = new DbOperator(this);

			properties = new MyProperties();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() throws Exception {
		ConnectionPoolFactory.closeConnectionPool();
		if(thAutoProcess != null && thAutoProcess.isAlive()) {
			thAutoProcess.interrupt();
			thAutoProcess.join();
		}		
		super.stop();
	}



	public static void main(String[] args) {
		launch(args);
	}
	
	public void login() {
		final LoginStage loginStage = new LoginStage();
		loginStage.show();
	}
	
	public void config() {
		ConfigureStage stage;
		try {
			stage = new ConfigureStage();
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			infoPane.setInfo("初始化配置文件错误。");
		}
	}
	
	public void reload(RecordType type, int creatorId, long searchTime, String noteSearchStr) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
			
		recordPane.clearContent();
		fromTime = searchTime;

		int num = 20;
		dbOperator.downloadBasicRecords(type, creatorId, fromTime, noteSearchStr.trim(), num);
	}
	
	public void loadNext(RecordType type, int creatorId, String noteSearchStr) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
		
		int num = 20;
		dbOperator.downloadBasicRecords(type, creatorId, fromTime, noteSearchStr.trim(), num);
	}
	
	public void saveRecord(RecordType type, long createTime, String devAddress) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
		
		dbOperator.downloadRecord(type, createTime, devAddress);
	}
	
	public void getAccountInfo(int creatorId) {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}
		
		dbOperator.getAccountInfo(creatorId);
	}
	
	/**
	 * 自动处理诊断请求
	 */
	public void autoProcessDiagnoseRequest() {
		if(!ACCOUNT.isLogin()) {
			login();
			return;
		}

		if(properties == null) {
			System.out.println("properties is null.");
			return;
		}
		
		// 下载下来的需要诊断处理的记录数据JSON文件名
		String ecgDataJsonFile = System.getProperty("java.io.tmpdir") + "review.json";
		
		
		String[] args = new String[] { properties.getPythonExe(), properties.getEcgScript(), properties.getEcgNNModel(), ecgDataJsonFile};
        for(String str : args) {
        	if(str == null || str.equals(""))
        		return;
        }
		
        // 自动处理记录的线程
		thAutoProcess = new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject jsonObj = null;
				try {
					while(true) {
						// 获取请求诊断的记录JSON Object
						jsonObj = RecordWebUtil.applyForDiagnose(MyEcgDiagnoseModel.VER);
						if(jsonObj != null) {
							RecordType type = RecordType.fromCode(jsonObj.getInt("recordTypeCode"));
			                if(type != RecordType.ECG) {
			                	Platform.runLater(()->infoPane.setInfo("对不起，暂时只能处理心电信号。"));
			                } else {
			                	Platform.runLater(()->infoPane.setInfo("正在处理心电信号。"));

			                	// 获取原始心电数据
				                String ecgStr = jsonObj.getString("ecgData");
				                String[] ecgDataStr = ecgStr.split(",");
				                List<Short> ecgData = new ArrayList<>();
				                for(String str : ecgDataStr) {
				                	ecgData.add(Short.parseShort(str));
				                }
				                
				                // 获取采样率
				                int sampleRate = jsonObj.getInt("sampleRate");				                
				                
				                // 对心电信号进行诊断，并获取诊断结果
				                MyEcgDiagnoseModel diagnoseModel = new MyEcgDiagnoseModel();				                
				                String diagnoseResult = "";
				                if(diagnoseModel.process(ecgData, sampleRate, properties)) {
				                	diagnoseResult = diagnoseModel.getDiagnoseResult();
				                }

				                // 更新诊断报告
								long createTime = jsonObj.getLong("createTime");
								String devAddress = jsonObj.getString("devAddress");
								Date date = new Date(createTime);
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								String dateStr = sdf.format(date);
								System.out.println("更新诊断报告："+ dateStr + "->" + diagnoseResult);
				        		RecordWebUtil.updateDiagnoseReport(RecordType.ECG, createTime, devAddress, MyEcgDiagnoseModel.VER, 
				        				MyEcgDiagnoseModel.REPORT_PROVIDER, new Date().getTime(), diagnoseResult);	
				        		
				        		Platform.runLater(()->infoPane.setInfo("心电信号处理完毕。"));
			        		}
			        		jsonObj = null;
						}
						Thread.sleep(5000);
					}
				} catch(InterruptedException ex) {
					System.out.println("心电诊断被终止");
				} 
			}
		});
		
		thAutoProcess.start();
	}
	
	public void AFDetect() {
		FileChooser.ExtensionFilter filter =  new FileChooser.ExtensionFilter("JSON文件","*.json");
        File file = FileDialogUtil.openFileDialog(primaryStage, true, null, null, filter);
        
		AFDetectExecutor afExec = new AFDetectExecutor(infoPane);
		afExec.processRecord(file);
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
                List<Short> ecgData = new ArrayList<>();
                try {
                	String ecgStr = json.getString("ecgData");
                    String[] ecgDataStr = ecgStr.split(",");
                    for(String str : ecgDataStr) {
                    	ecgData.add(Short.parseShort(str));
                    }
                } catch (JSONException ex) {
                	JSONArray arr = json.getJSONArray("ecgData");
                	for(int i = 0; i < arr.length(); i++) {
                		ecgData.add((short)arr.getInt(i));
                	}
                }
                int sampleRate = json.getInt("sampleRate");
                
                EcgPreProcessor ecgProc = new EcgPreProcessor();
                ecgProc.process(ecgData, sampleRate);
    			
        		String srcFileName = file.getAbsolutePath();
        		String tmpFileName = srcFileName.substring(0, srcFileName.lastIndexOf('.'));
        		
        		// save review result to json file
        		String reviewJsonFileName = tmpFileName + "-review.json";
        		File reviewFile = new File(reviewJsonFileName);
        		try(PrintWriter reviewWriter = new PrintWriter(reviewFile)) {
        			reviewWriter.print(ecgProc.getResultJson().toString());
        		}
        		
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
		
		FileChooser.ExtensionFilter filter =  new FileChooser.ExtensionFilter("JSON文件", "*.json");
        File file = FileDialogUtil.openFileDialog(primaryStage, true, null, null, filter);

        if(file != null){
        	try {
        		Process proc;
            	String[] args = new String[] { properties.getPythonExe(), properties.getEcgScript(), properties.getEcgNNModel(), file.getAbsolutePath()};
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
                JSONArray predictList = (JSONArray) resultJson.get("Predict");
                JSONArray resultList = (JSONArray) resultJson.get("Result");
                System.out.println(predictList);
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
	public void onLogin(boolean success) {
		if(success)
			infoPane.setInfo("登录成功");
		else
			infoPane.setInfo("登录失败");
		ACCOUNT.setLogin(success);
	}
	
	@Override
	public void onBasicRecordsDownloaded(JSONArray basicInfos) {
		if(basicInfos == null || basicInfos.isEmpty()) {
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
	
	@Override
	public void onAccountInfoDownloaded(JSONObject json) {
		if(json == null) {
			infoPane.setInfo("下载账户信息失败");
		} else {
			System.out.println(json.toString());
			AccountStage stage;
			String userName = json.getString("userName");
			userName = "***"+ userName.substring(userName.length()-4);
			stage = new AccountStage(userName, json.getString("nickName"), json.getString("note"));
			stage.show();
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
			
			DialogPane dialogPane = new DialogPane();
			dialogPane.setContent(pane);
			dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			Button btnOk = (Button) dialogPane.lookupButton(ButtonType.OK);
			Button btnCancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
			
			btnOk.setOnAction(new EventHandler<ActionEvent>() {
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
			
			btnCancel.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					close();
				}
			});
			
			initOwner(primaryStage);
			initModality(Modality.WINDOW_MODAL);
			setResizable(false);
			setScene(new Scene(dialogPane));
			setTitle("请登录");
		}
	}
	
	private class ConfigureStage extends Stage{	
		ConfigureStage() throws IOException {
			GridPane pane = new GridPane();
			pane.setAlignment(Pos.TOP_LEFT);
			pane.setPadding(new Insets(10,10,10,10));
			pane.setHgap(5);
			pane.setVgap(5);
			pane.add(new Label("Python.exe："), 0, 0);
			TextField tfPythonExe = new TextField(); tfPythonExe.setPrefColumnCount(30);
			tfPythonExe.setText(properties.getPythonExe());pane.add(tfPythonExe, 1, 0);
			Button btnPythonExe = new Button("选择");
			final FileChooser.ExtensionFilter exeFilter =  new FileChooser.ExtensionFilter("exe文件","*.exe");
			btnPythonExe.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					File file = FileDialogUtil.openFileDialog(ConfigureStage.this, true, null, "Python.exe", exeFilter);
					tfPythonExe.setText(file.getAbsolutePath());
				}				
			});
			pane.add(btnPythonExe, 2, 0);
			
			pane.add(new Label("诊断脚本(.py)："), 0, 1);
			TextField tfScript = new TextField(); pane.add(tfScript, 1, 1);
			tfScript.setText(properties.getEcgScript());
			Button btnScript = new Button("选择");
			final FileChooser.ExtensionFilter pyFilter =  new FileChooser.ExtensionFilter("py文件","*.py");
			btnScript.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					File file = FileDialogUtil.openFileDialog(ConfigureStage.this, true, null, null, pyFilter);
					tfScript.setText(file.getAbsolutePath());
				}				
			});
			pane.add(btnScript, 2, 1);
			
			pane.add(new Label("诊断模型："), 0, 2);
			TextField tfModel = new TextField(); pane.add(tfModel, 1, 2);
			tfModel.setText(properties.getEcgNNModel());
			Button btnModel = new Button("选择");
			btnModel.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					File file = FileDialogUtil.openFileDialog(ConfigureStage.this, true, null, null, null);
					System.out.println(file.getAbsolutePath());
					tfModel.setText(file.getAbsolutePath());
				}				
			});
			pane.add(btnModel, 2, 2);
			
			DialogPane dialogPane = new DialogPane();
			dialogPane.setContent(pane);
			dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			Button btnOk = (Button) dialogPane.lookupButton(ButtonType.OK);
			Button btnCancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
			
			btnOk.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					String pythonExe = tfPythonExe.getText();
					String ecgScript = tfScript.getText();
					String nnModel = tfModel.getText();
					properties.setPythonExe(pythonExe);
					properties.setEcgScript(ecgScript);
					properties.setEcgNNModel(nnModel);
					try {
						properties.save();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					close();
				}
			});
			
			btnCancel.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					close();
				}
			});
			
			initOwner(primaryStage);
			initModality(Modality.WINDOW_MODAL);
			setResizable(false);
			setScene(new Scene(dialogPane, 600, -1));
			setTitle("请设置");
		}
	}
	
	private class AccountStage extends Stage{	
		AccountStage(String userName, String nickName, String note) {
			GridPane pane = new GridPane();
			pane.setAlignment(Pos.CENTER);
			pane.setPadding(new Insets(10,10,10,10));
			pane.setHgap(5);
			pane.setVgap(5);
			

			pane.add(new Label("用户名："), 0, 0);
			pane.add(new Label(userName), 1, 0);
			pane.add(new Label("昵称："), 0, 1);
			pane.add(new Label(nickName), 1, 1);
			pane.add(new Label("备注："), 0, 2);
			pane.add(new Label(note), 1, 2);
			
			DialogPane dialogPane = new DialogPane();
			dialogPane.setContent(pane);
			
			initOwner(primaryStage);
			initModality(Modality.WINDOW_MODAL);
			setResizable(false);
			setScene(new Scene(dialogPane));
			setTitle("账户信息");
		}
	}
}
