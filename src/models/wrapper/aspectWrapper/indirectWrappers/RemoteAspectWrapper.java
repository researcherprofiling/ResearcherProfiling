package models.wrapper.aspectWrapper.indirectWrappers;


import database.EmbeddedDB;
import models.schema.Schema;
import models.wrapper.aspectWrapper.GeneralAspectWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.MyHTTP;

import java.util.HashMap;
import java.util.Map;

public class RemoteAspectWrapper extends GeneralAspectWrapper {

    private String url;
    public boolean isActive;

    public RemoteAspectWrapper(EmbeddedDB db, String name, String url) {
        super(db, name);
        this.url = url;
        this.isActive = true;
        if (url != null) {
            try {
                String response = MyHTTP.get(url + "/schema", null);
                this.schema = Schema.fromJSONArray(JSONArray.fromObject(response));
                this.isValid = true;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                this.schema = null;
                this.isValid = false;
            }
        }
    }

    @Override
    public JSONObject getResultAsJSON(JSONObject searchConditions) {
        Map<String, String> params = new HashMap<String, String>();
        for (Object k : searchConditions.keySet()) {
            String key = (String)k;
            params.put(key, searchConditions.getString(key));
        }
        return JSONObject.fromObject(MyHTTP.get(url, params));
    }

    @Override
    public JSONArray getRegisteredSources() {
        return new JSONArray();
    }

    @Override
    public JSONObject timedGetResultAsJSON(JSONObject searchConditions)  {
        System.out.println("Retrieving data pertaining to aspect " + name + ".");
        long startTime = System.currentTimeMillis();
        JSONObject ret = getResultAsJSON(searchConditions);
        long endTime = System.currentTimeMillis();
        System.out.println("Aspect " + name + " finished execution. Time elapsed: " + ((double)(endTime - startTime)/1000.0) + " seconds.");
        return ret;
    }

    @Override
    public JSONObject redoSearch(JSONObject searchConditions) {
        return null;
    }

    @Override
    public boolean isActivated() {
        return this.isActive;
    }

    @Override
    public void setActivation(String source, boolean newIsActive) {
        this.isActive = newIsActive;
    }

    @Override
    public void print() {
        System.out.println(name + " at " + url + "\n");
    }
}
