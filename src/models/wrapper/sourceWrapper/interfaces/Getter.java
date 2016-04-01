package models.wrapper.sourceWrapper.interfaces;

import net.sf.json.JSONObject;

/**
 * Standard interface for a getter
 */
public interface Getter {

    /*
    * Input: key-value pairs as search conditions
    * Output: Raw response from the information source
    * */
    public Object getResult(JSONObject searchConditions);

}
