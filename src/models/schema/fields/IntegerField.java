package models.schema.fields;

import models.schema.Field;
import net.sf.json.JSONObject;

import java.util.Iterator;

public class IntegerField extends Field<Integer> {

    public IntegerField(String fieldName, boolean isPrimaryKey) {
        super(fieldName, isPrimaryKey);
        dataType = "Integer";
    }

    @Override
    public double equal(Integer o1, Integer o2) {
        return 1 - (double)Math.abs(o1 - o2) / (double)Math.max(o1,o2);
    }

    @Override
    public String convertToString(JSONObject record) {
        JSONObject data = record.getJSONObject(fieldName);
        if (data.size() > 1) {
            String ret = "\n";
            for (Object k : data.keySet()) {
                String sources = (String)k;
                ret += "  From " + sources + " : " + data.getInt(sources) + "\n";
            }
            return ret;
        } else {
            Iterator iter = data.keys();
            return Integer.toString(data.getInt((String)iter.next()));
        }
    }

}
