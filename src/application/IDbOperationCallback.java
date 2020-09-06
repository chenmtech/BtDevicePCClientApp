package application;

import org.json.JSONArray;
import org.json.JSONObject;

public interface IDbOperationCallback {
	void onLoginUpdated(boolean success);
	void onRecordBasicInfoListUpdated(JSONArray basicInfos);
	void onRecordDownloaded(JSONObject json);
}
