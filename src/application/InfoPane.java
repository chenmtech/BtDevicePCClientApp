package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;

public class InfoPane extends FlowPane {
	private Label lblInfo = new Label();
	
	public InfoPane() {
		this.setAlignment(Pos.CENTER);
		this.setPadding(new Insets(10,10,10,10));
		lblInfo.setTextFill(Color.RED);
		lblInfo.setStyle("-fx-border-color: green");	
		getChildren().add(lblInfo);
	}

	public void setInfo(String info) {
		lblInfo.setText(info);
	}
}
