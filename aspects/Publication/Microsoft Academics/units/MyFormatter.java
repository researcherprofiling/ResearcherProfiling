import models.wrapper.sourceWrapper.interfaces.Formatter;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

import java.lang.Object;
import java.lang.Override;
import java.lang.System;

public class MyFormatter implements Formatter
{

    @Override
    public JSON convertToJSON(Object rawResponse) {
        XMLSerializer xmlConverter = new XMLSerializer();
        //return xmlConverter.read((String)rawResponse);
        return JSONObject.fromObject(rawResponse);
    }



}