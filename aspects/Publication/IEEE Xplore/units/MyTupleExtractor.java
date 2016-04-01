import models.wrapper.sourceWrapper.interfaces.TupleExtractor;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.lang.Exception;
import java.lang.Override;

public class MyTupleExtractor implements TupleExtractor{

    @Override
    public JSONArray getTuples(JSON all) {
        JSONObject allAsObject;
        try {
            allAsObject = JSONObject.fromObject(all);
        } catch (JSONException e) {
            return new JSONArray();
        }
        try {
            return allAsObject.getJSONArray("document");
        } catch (Exception e) {
            JSONObject rec = allAsObject.getJSONObject("document");
            JSONArray ret = new JSONArray();
            ret.add(rec);
            return ret;
        }
    }

}