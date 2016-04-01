package models.wrapper.sourceWrapper;

import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.MyHTTP;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import static utils.Constants.localServerPortNumber;

public class IndirectSourceWrapper extends GeneralSourceWrapper {

    private String aspect;

    public IndirectSourceWrapper(Schema schema, String name, String aspect) {
        super(schema, name);
        this.aspect = aspect;
    }

    @Override
    public JSONArray getResultAsJSON(JSONObject searchConditions) {
        Map<String, String> params = new HashMap<String, String>();
        for (Object k : searchConditions.keySet()) {
            String key = (String)k;
            params.put(key, searchConditions.get(key).toString());
        }
        String url;
        try {
            url = "http://localhost:" + localServerPortNumber + "/" + URLEncoder.encode(this.aspect, "UTF-8") + "/" + URLEncoder.encode(this.name, "UTF-8");
        } catch (IOException e) {
            System.out.println("Failed to encode aspect or source name for source: " + this.name);
            return null;
        }
        String response = MyHTTP.get(url, params);
        if (response == null) {
            return null;
        } else {
            JSONArray ret = JSONArray.fromObject(response);
            for (Object o : ret) {
                ((JSONObject)o).put("Provided by", name);
                JSONArray temp = new JSONArray();
                temp.add(o);
                ((JSONObject) o).put("Original record(s)", temp);
            }
            return ret;
        }
    }
}
