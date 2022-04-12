package util;

import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * 文件对话框使用帮助类
 * @author gdmc
 *
 */
public class FileDialogUtil {
	// 当前选择的文件路径
	private static String SELECTED_DIR = ".";
	
	/**
	 * 显示文件对话框（可以是打开或保存），进行操作
	 * @param owner
	 * @param open：打开或保存
	 * @param defaultDir：指定的缺省路径
	 * @param defaultName：指定的缺省文件名
	 * @param filter：指定的文件类型过滤
	 * @return：返回操作后的文件File对象
	 */
	public static File openFileDialog(Window owner, boolean open, String defaultDir, String defaultName, FileChooser.ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		if(open)
			fileChooser.setTitle("打开文件");
		else
			fileChooser.setTitle("保存文件");
		
        //设置初始文件路径
		if(defaultDir == null) 
			defaultDir = SELECTED_DIR;
		
        fileChooser.setInitialDirectory(new File(defaultDir));
        fileChooser.setInitialFileName(defaultName);
        
        if(filter != null)
        	fileChooser.getExtensionFilters().add(filter);
        
        File file;
        if(open)
        	file = fileChooser.showOpenDialog(owner);
        else
        	file = fileChooser.showSaveDialog(owner);
        
        if(file != null) {
        	SELECTED_DIR = file.getParent();
        }
        
        return file;
	}

}
