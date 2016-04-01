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

    /*
    STARTING SOURCE = http://federalreporter.nih.gov/
    FORM NAME = queryterms


     */

public class MyGetter implements Getter {

    public Object getResult(JSONObject searchConditions) {

        try{

            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

            WebClient webClient = new WebClient();

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = nameParts[1];

            // Submit the WebForm
            HtmlPage nextPage = webClient.getPage("http://www.ncbi.nlm.nih.gov/pubmed/?term=" + firstName + "+" + lastName);

            HtmlDivision results = (HtmlDivision)nextPage.getElementById("maincontent");

            JSON answer = jsonCreator(results);
            return (JSON)answer;

        }
        catch(Exception e){

            System.out.println("Could not find anything on 'PubMed' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on the Federal reporter website (http://www.ncbi.nlm.nih.gov/pubmed). And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @param allResults :: Table at the PubMed's Result Page
     * @return JSON of results
     */
    public static JSON jsonCreator(HtmlDivision allResults){

        JSONArray results = new JSONArray();

        List<HtmlDivision> units = allResults.getOneHtmlElementByAttribute("div", "class", "content").getElementsByAttribute("div", "class", "rprt");

        for(HtmlDivision unit : units){

            HtmlDivision unitContent = unit.getOneHtmlElementByAttribute("div", "class", "rslt");

            // Get the Title of the Paper
            String title = unitContent.getHtmlElementsByTagName("p").get(0).getTextContent();

            List<HtmlDivision> supportContent = unitContent.getElementsByAttribute("div", "class", "supp");

            // Get the Authors for the Paper
            String authors = supportContent.get(0).getOneHtmlElementByAttribute("p", "class", "desc").getTextContent();

            // Get Year
            String publisher = supportContent.get(0).getOneHtmlElementByAttribute("p", "class", "details").getHtmlElementsByTagName("span").get(0).getAttribute("title");
            int buffer = supportContent.get(0).getOneHtmlElementByAttribute("p", "class", "details").getHtmlElementsByTagName("span").get(0).getTextContent().length();
            String year = supportContent.get(0).getOneHtmlElementByAttribute("p", "class", "details").getTextContent().substring(buffer + 2, buffer + 6);


            // Create a new unit
            JSONObject jsonUnit = new JSONObject();

            jsonUnit.put("Title", title);
            jsonUnit.put("Authors", authors);
            jsonUnit.put("Year", year);
            jsonUnit.put("Publisher", publisher);

            results.add(jsonUnit);

        }

        return results;
    }

}
