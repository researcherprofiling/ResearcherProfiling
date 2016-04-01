import models.schema.fields.IntegerField;
import models.wrapper.sourceWrapper.interfaces.FieldExtractor;
import net.sf.json.JSON;
import java.lang.Object;
import java.lang.String;
import java.lang.Long;

import net.sf.json.JSONObject;

public class MyFieldExtractor implements FieldExtractor {

    public Object extractField(JSON potentialTuple) {
        JSONObject tuple = (JSONObject) potentialTuple;
        return Long.parseLong(tuple.getString("fundsObligatedAmt"));
    }

}