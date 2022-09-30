package application;

import org.json.JSONArray;
import org.json.JSONObject;

public interface IDbOperationCallback {
	/**
	 * 响应登录操作
	 * @param success: true-登录成功， false-登陆失败
	 */
	void onLogin(boolean success);
	
	/**
	 * 响应下载记录列表操作
	 * @param records: 获取的记录列表打包为JSONArray
	 */
	void onRecordListDownloaded(JSONArray records);
	
	/**
	 * 响应记录下载操作
	 * @param json：下载的记录，打包为JSON Object
	 */
	void onRecordDownloaded(JSONObject json);
	
	/**
	 * 响应账户信息下载操作
	 * @param json：下载的账户信息，打包为JSON Object
	 */
	void onAccountInfoDownloaded(JSONObject json);
}
