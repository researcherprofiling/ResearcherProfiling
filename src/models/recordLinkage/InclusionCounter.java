package models.recordLinkage;

import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class InclusionCounter {

    public static JSONArray countInclusion(Schema schema, JSONArray records) {
        JSONArray ret = new JSONArray();
        while (!records.isEmpty()) {
            JSONObject curRec = (JSONObject)(records.remove(0));
            JSONObject toMerge = null;
            int i = 0;
            for (; i < ret.size(); i++) {
                JSONObject other = ret.getJSONObject(i);
                if (schema.mergeable(curRec, other) > 0.85) {
                    toMerge = other;
                    break;
                }
            }
            if (i >= ret.size()) {
                ret.add(curRec);
            }
            else {
                ret.remove(i);
                schema.merge(curRec, toMerge);
                records.add(curRec);
            }
        }
        return ret;
    }
    
}
