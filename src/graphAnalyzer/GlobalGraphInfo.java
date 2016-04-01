package graphAnalyzer;


import java.util.ArrayList;
import java.util.HashMap;

public class GlobalGraphInfo {

    public static ArrayList<String> keys = new ArrayList<String>();
    public static HashMap<String,Integer> sourceToCount = new HashMap<String,Integer>();


    /**
     * This method adds the Source Name to the list of Sources represented
     * in the final bar graph.
     * @param sourceName
     * @param count
     */
    public static void addSourceTitle(String sourceName, int count){

        sourceToCount.put(sourceName, count);
        keys.add(sourceName);
    }
}
