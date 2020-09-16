package util;

import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class FileDialogUtil {
	private static String DIR = ".";
	
	public static File openFileDialog(Window owner, boolean open, String defaultDir, String defaultName, FileChooser.ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		if(open)
			fileChooser.setTitle("打开文件");
		else
			fileChooser.setTitle("保存文件");
        //设置将当前目录作为初始显示目录
		if(defaultDir == null) 
			defaultDir = DIR;
        fileChooser.setInitialDirectory(new File(defaultDir));
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(filter);
        File file;
        if(open)
        	file = fileChooser.showOpenDialog(owner);
        else
        	file = fileChooser.showSaveDialog(owner);
        if(file != null) {
        	DIR = file.getParent();
        }
        return file;
	}

}
