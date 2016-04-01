package models.schema.fields;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import models.schema.Field;
import net.sf.json.JSONObject;
import java.util.Iterator;

public class TextField extends Field<String> {

    public TextField(String fieldName, boolean isPrimaryKey) {
        super(fieldName, isPrimaryKey);
        dataType = "Text";
    }

    @Override
    public double equal(String o1, String o2) {
        NormalizedLevenshtein leven = new NormalizedLevenshtein();
        return leven.similarity(o1.toString().toLowerCase().trim(), o2.toString().toLowerCase().trim());
    }

    @Override
    public String convertToString(JSONObject record) {
        JSONObject data = record.getJSONObject(fieldName);
        if (data.size() > 1) {
            String ret = "\n";
            for (Object k : data.keySet()) {
                String sources = (String)k;
                ret += "  From " + sources + " : " + data.getString(sources) + "\n";
            }
            return ret;
        } else {
            Iterator iter = data.keys();
            return data.getString((String)iter.next());
        }
    }

}


