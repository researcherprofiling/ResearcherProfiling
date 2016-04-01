import models.wrapper.sourceWrapper.interfaces.Formatter;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

import java.lang.Object;
import java.lang.Override;

public class MyFormatter implements Formatter
{

    @Override
    public JSON convertToJSON(Object rawResponse) {
        return JSONObject.fromObject(rawResponse);
    }



}