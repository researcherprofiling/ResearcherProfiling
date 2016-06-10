import com.sun.tools.javac.util.Pair;
import models.wrapper.sourceWrapper.interfaces.Getter;

import java.lang.Integer;
import java.lang.String;
import java.lang.System;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Constants;
import utils.MyHTTP;

// TODO -> The Affiliation Model needs to be included
public class MyGetter implements Getter {

	public Object getResult(JSONObject searchConditions) {

        JSONObject results = new JSONObject();
        JSONArray resultsArr = new JSONArray();

        JSONObject authorObj = JSONObject.fromObject(getAuthorJSON(searchConditions.getString("fullName")));

        System.out.println(authorObj.toString(1));

        JSONArray authorArr = authorObj.getJSONObject("d").getJSONArray("results");

        for(Object authorObject : authorArr){

            JSONObject author = JSONObject.fromObject(authorObject);

            ArrayList<Integer> paperIDs = getListOfPaperIDs((int)author.get("ID"), 0);

            int count = 0;
            for(Integer pID : paperIDs){

                Pair<String,Integer> title_year = getPaperTitleAndYear(pID);

                JSONObject unit = new JSONObject();

                unit.put("Title", title_year.fst);
                unit.put("Name", author.getString("Name"));
                unit.put("Affiliation", author.getString("Affiliation"));
                unit.put("Year", title_year.snd);

                count++;

                if(count > 25){
                    break;
                }


                resultsArr.add(unit);
            }

            break;

        }

        results.put("results", resultsArr);
        return (JSON)results;
	}


    /**
     * This method from the Microsoft Academics Author Table gets the Details about the
     * Author that is requested by Name.
     *
     * SCHEMA ONLINE:
     * ID | Name | NativeName | Affiliation | AffiliationID | Homepage | Version | LinkedInUrl |
     * WikipediaUrl | TwitterUserName | ResearchInterests
     * @param fullName
     * @return Author JSON Info (Objrct)
     */
    public Object getAuthorJSON(String fullName){

        Map<String, String> params = new HashMap<String, String>();

        // Requesting for a JSON format response
        params.put("$format", "json");
        params.put("$filter", "Name" + " " + "eq" + " '" + fullName + "' ");

        return MyHTTP.get("https://api.datamarket.azure.com/MRC/MicrosoftAcademic/v2/Author", params);
    }


    /**
     * This method for the requested full-name grabs all the Author-IDs from the Microsoft
     * Academics Author Table.
     *
     * @param fullName
     * @return List of AUthorID(s) associated with a Name
     */
    public ArrayList<Integer> getListOfAuthorIDs(String fullName){

        Map<String, String> params = new HashMap<String, String>();

        // Requesting for a JSON format response
        params.put("$format", "json");
        params.put("$filter", "Name" + " " + "eq" + " '" + fullName + "' ");

        JSONObject jsonObj = JSONObject.fromObject(MyHTTP.get("https://api.datamarket.azure.com/MRC/MicrosoftAcademic/v2/Author", params));

        JSONObject innerJSON = jsonObj.getJSONObject("d");
        JSONArray jsonArr = innerJSON.getJSONArray("results");

        ArrayList<Integer> authorIDs = new ArrayList<Integer>();

        for(Object jObj : jsonArr){

            JSONObject j = JSONObject.fromObject(jObj);

            authorIDs.add((int)j.get("ID"));
        }

        return authorIDs;
    }


    /**
     * This method for a given AuthorID provides a list of Paper-IDs associated with that Author
     * in the Microsoft Academics Paper_Author Table Storage.
     *
     * @param authorID :: ID associated with an Author
     * @param skip :: Number of initial results to skip
     * NOTE: The reponse gives 100 results at a time. To cover all use the skip parameter
     * @return List of PaperID associated to a particular Author
     */
    public ArrayList<Integer> getListOfPaperIDs(int authorID, int skip){

        Map<String, String> params = new HashMap<String, String>();

        // Requesting for a JSON format response
        params.put("$format", "json");
        params.put("$filter", "AuthorID" + " " + "eq" + " " + authorID + " ");
        params.put("$skip", String.valueOf(skip));

        JSONObject jsonObj = JSONObject.fromObject(MyHTTP.get("https://api.datamarket.azure.com/MRC/MicrosoftAcademic/v2/Paper_Author", params));

        JSONObject innerJSON = jsonObj.getJSONObject("d");
        JSONArray jsonArr = innerJSON.getJSONArray("results");

        ArrayList<Integer> paperIDs = new ArrayList<Integer>();

        for(Object jObj : jsonArr){

            JSONObject j = JSONObject.fromObject(jObj);

            if(j.get("PaperID") != null)
                paperIDs.add((int)j.get("PaperID"));
        }


        return paperIDs;

    }


    /**
     * This method from the Microsoft Academics Paper Storage Table provides the Title
     * and Year for a paper given the unique paper ID.
     *
     * @param paperID :: ID associated with a paper
     * @return Pair of Title and Year
     */
    public Pair<String, Integer> getPaperTitleAndYear(int paperID){

        Map<String, String> params = new HashMap<String, String>();

        // Requesting for a JSON format response
        params.put("$format", "json");
        params.put("$filter", "ID" + " " + "eq" + " " + paperID + " ");

        JSONObject jsonObj = JSONObject.fromObject(MyHTTP.get("https://api.datamarket.azure.com/MRC/MicrosoftAcademic/v2/Paper", params));

        JSONObject innerJSON = jsonObj.getJSONObject("d");
        JSONArray jsonArr = innerJSON.getJSONArray("results");

        for(Object pObj : jsonArr){

            JSONObject p = JSONObject.fromObject(pObj);

            Pair<String,Integer> title_year = new Pair<String, Integer>(p.getString("Title"), (int)p.get("Year"));

            return title_year;
        }

        // Error Case
        return null;
    }
}
