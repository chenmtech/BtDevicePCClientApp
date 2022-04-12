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
	 * 响应BasicRecord下载列表操作
	 * @param basicRecords: 获取的BasicRecords打包为JSONArray
	 */
	void onBasicRecordsDownloaded(JSONArray basicRecords);
	
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
