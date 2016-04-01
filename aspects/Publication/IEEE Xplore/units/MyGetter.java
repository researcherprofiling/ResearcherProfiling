import models.wrapper.sourceWrapper.interfaces.Getter;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import utils.Constants;
import utils.MyHTTP;

public class MyGetter implements Getter {

	public Object getResult(JSONObject searchConditions) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("au", searchConditions.getString("fullName"));
        if (searchConditions.containsKey("affiliation")) {
            params.put("cs", searchConditions.getString("affiliation"));
        }
        return MyHTTP.get("http://ieeexplore.ieee.org/gateway/ipsSearch.jsp", params);
	}

}
