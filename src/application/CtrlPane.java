package application;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class CtrlPane extends HBox{
	
	public CtrlPane(Main main) {
		setAlignment(Pos.CENTER);
		setSpacing(10);
		setPadding(new Insets(10,10,10,10));
		Button btnLogin = new Button("登录");
		Button btnReload = new Button("重新加载");
		Button btnLoadNext = new Button("继续加载");
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
				main.reload();
			}
		});
		
		btnLoadNext.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				main.loadNext();
			}
		});
	}

}
