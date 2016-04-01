import models.wrapper.sourceWrapper.interfaces.Getter;

import java.lang.String;
import java.lang.System;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Constants;
import utils.MyHTTP;

public class MyGetter implements Getter {

	public Object getResult(JSONObject searchConditions) {
        Map<String, String> params = new HashMap<String, String>();

        String query;
        if (searchConditions.containsKey("affiliation")) {
            query = "(AUTHOR-NAME("+searchConditions.getString("last")+
                    ","+searchConditions.getString("first")+")) AND (AFFIL(" +
                    searchConditions.getString("affiliation")+"))";
        }
        else{
            query = "(AUTHOR-NAME("+searchConditions.getString("last")+
                    ","+searchConditions.getString("first")+"))";
        }
        if (searchConditions.containsKey("kws")) {
            JSONArray positive = searchConditions.getJSONObject("kws").getJSONArray("positive");
            JSONArray negative = searchConditions.getJSONObject("kws").getJSONArray("negative");
            if (!positive.isEmpty()) {
                for (int i = 0; i < positive.size(); i++) {
                    query += " AND ALL(\"" + positive.getString(i) + "\")";
                }
            }
            if (!negative.isEmpty()) {
                for (int i = 0; i < negative.size(); i++) {
                    query += " AND NOT ALL(\"" + negative.getString(i) + "\")";
                }
            }
        }
        params.put("query", query);
        params.put("apiKey", getApiKey());
        return MyHTTP.get("http://api.elsevier.com/content/search/scopus", params);
	}


    /**
     *  This method gets the API Key for Scopus from a remote
     *  location.
     *
     * @return apiKey :: The API key to validate the source
     */
     // TODO -> Make the Key Secret
	public String getApiKey(){

	    return "19b4b3546222699157deac547bc8e232";
	}


}
