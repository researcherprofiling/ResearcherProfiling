
import graphAnalyzer.GlobalGraphInfo;
import models.wrapper.sourceWrapper.interfaces.TupleExtractor;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.lang.Exception;
import java.lang.Override;
import java.lang.System;

import graphAnalyzer.GraphAnalyzer.*;

public class MyTupleExtractor implements TupleExtractor{

    @Override
    public JSONArray getTuples(JSON all) {
        JSONObject allAsObject = JSONObject.fromObject(all);

        try {
            JSONObject innerJSON = JSONObject.fromObject(allAsObject.get("search-results"));

            return innerJSON.getJSONArray("entry");
        } catch (Exception e) {

            JSONArray ret = new JSONArray();
            if(allAsObject.get("search-results") != null){
                JSONObject innerJSON = JSONObject.fromObject(allAsObject.get("search-results"));
                JSONObject rec = innerJSON.getJSONObject("entry");

                ret.add(rec);
            }
            return ret;
        }
    }

}