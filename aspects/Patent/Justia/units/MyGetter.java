import models.wrapper.sourceWrapper.interfaces.Getter;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import utils.Constants;
import utils.MyHTTP;

// Headless Browser imports
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.lang.Iterable;
import java.util.List;

import java.util.ArrayList;

public class MyGetter implements Getter {

    public Object getResult(JSONObject searchConditions) {

        // turn off htmlunit warnings
        try{

            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

            WebClient webClient = new WebClient();

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);


            String baseURL = "http://patents.justia.com/";
            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = nameParts[1];

            // Submit the WebForm
            HtmlPage nextPage = webClient.getPage(baseURL + "search?q=" + firstName + "+" + lastName);

            List<HtmlDivision> results = (List<HtmlDivision>)nextPage.getByXPath("//div[@class=\"result\"]");

            JSON answer = jsonCreator(results);
            return (JSON)answer;

        }
        catch(Exception e){

            System.out.println("Could not find anything on 'Justia Patents' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on Justia Patent website (http://patents.justia.com/). And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @param allResults :: Unordered list of Justia's result
     * @return JSON of results
     */
    public static JSON jsonCreator(List<HtmlDivision> allResults) {

        JSONArray results = new JSONArray();


        for (HtmlElement result : allResults) {



            String title = "";
            String inventors = "";
            String date_filed = "";
            String date_issued = "";
            String number = "";
            String assignee = "";
            String patentAbstract = "";

            try {
                // Get Patent Title
                title = result.getOneHtmlElementByAttribute("div", "class", "head").getTextContent().replaceAll("(  |\\n|:)", "");;
            }catch(Exception e){

            }

            try {
                // Get Inventors
                inventors = result.getOneHtmlElementByAttribute("div", "class", "inventors").getTextContent().replaceAll("(Inventors|  |\\n|:)", "");;
            }catch(Exception e){

            }

            try {
                number = result.getOneHtmlElementByAttribute("div", "class", "number").getTextContent().replaceAll("(Patent number|Application number|  |\\n|:)", "");;
            }catch(Exception e){

            }

            try {
                date_issued = result.getOneHtmlElementByAttribute("div", "class", "date-issued").getTextContent().replaceAll("(Issued|  |\\n|:)", "");;
            }catch(Exception e){

            }

            try {
                date_filed = result.getOneHtmlElementByAttribute("div", "class", "date-filed").getTextContent().replaceAll("(Filed|  |\\n|:)", "");;
            }catch(Exception e){

            }

            try {
                patentAbstract = result.getOneHtmlElementByAttribute("div", "class", "abstract").getTextContent().replaceAll("(Abstract|  |\\n|:)", "");
            }catch(Exception e){

            }

            try {
                assignee = result.getOneHtmlElementByAttribute("div", "class", "assignees").getTextContent().replaceAll("(Assignee|  |\\n|:)", "");
            }catch(Exception e){

            }

            // Create a new unit
            JSONObject jsonUnit = new JSONObject();

            jsonUnit.put("Title", title);
            jsonUnit.put("Inventors", inventors);
            jsonUnit.put("Filed", date_filed);
            jsonUnit.put("Issued", date_issued);
            jsonUnit.put("Abstract", patentAbstract);
            jsonUnit.put("Assignee", assignee);
            jsonUnit.put("Number", number);

            results.add(jsonUnit);

        }

        return results;
    }

}
