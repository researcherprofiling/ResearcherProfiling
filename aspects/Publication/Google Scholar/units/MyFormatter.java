import models.wrapper.sourceWrapper.interfaces.Formatter;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

import java.lang.Object;
import java.lang.Override;
import java.util.Arrays;

public class MyFormatter implements Formatter
{

    @Override
    public JSON convertToJSON(Object rawResponse) {

        return (JSON)rawResponse;
    }

    public JSON converToJSONForScholarPy(Object rawResponse){

        String res = (String) rawResponse;
        String[] records = res.split("\n\n");
        JSONArray ret = new JSONArray();
        for (String record : records) {
            JSONObject current = new JSONObject();
            String[] fieldValues = record.split("\n");
            for (String fieldValue : fieldValues) {
                String field = fieldValue.substring(0, 14).trim();
                String value = fieldValue.substring(15).trim();
                current.put(field, value);
            }
            ret.add(current);
        }
        return ret;
    }

}