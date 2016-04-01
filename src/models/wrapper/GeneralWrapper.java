package models.wrapper;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Created by Morpheusss on 15/7/28.
 */
public interface GeneralWrapper {

    String basePath = "aspects";

    JSON getResultAsJSON(JSONObject searchConditions);

    void print();

}
