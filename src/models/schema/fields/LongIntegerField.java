package models.schema.fields;

import models.schema.Field;
import net.sf.json.JSONObject;
import java.util.Iterator;

public class LongIntegerField extends Field<Long> {

    public LongIntegerField(String fieldName, boolean isPrimaryKey) {
        super(fieldName, isPrimaryKey);
        dataType = "LongInteger";
    }

    @Override
    public double equal(Long o1, Long o2) {
        return 1 - (double)Math.abs(o1 - o2) / (double)Math.max(o1,o2);
    }

    @Override
    public String convertToString(JSONObject record) {
        JSONObject data = record.getJSONObject(fieldName);
        if (data.size() > 1) {
            String ret = "\n";
            for (Object k : data.keySet()) {
                String sources = (String)k;
                ret += "  From " + sources + " : " + data.getLong(sources) + "\n";
            }
            return ret;
        } else {
            Iterator iter = data.keys();
            return Long.toString(data.getLong((String)iter.next()));
        }
    }

}
