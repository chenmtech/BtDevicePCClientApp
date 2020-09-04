package application;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;

public class StatusInfoPane extends FlowPane {
	private Label statusInfo;
	
	public StatusInfoPane(String info) {
		this.setAlignment(Pos.CENTER);
		statusInfo = new Label(info);
		statusInfo.setTextFill(Color.RED);
		statusInfo.setStyle("-fx-border-color: green");	
		getChildren().add(statusInfo);
	}

	public void setInfo(String info) {
		statusInfo.setText(info);
	}
}
