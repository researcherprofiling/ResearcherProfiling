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

            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = nameParts[1];

            // Submit the WebForm
            HtmlPage nextPage = webClient.getPage("http://patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO2&Sect2=HITOFF&u=%2Fnetahtml%2FPTO%2Fsearch-adv.htm&r=0&p=1&f=S&l=50&Query=IN%2F" + lastName + "-" + firstName + "&d=PTXT");

            String pageTitle = nextPage.getTitleText();

            if(pageTitle.split("( )")[0].equalsIgnoreCase("Patent")){

                HtmlTable table = (HtmlTable)nextPage.getBody().getElementsByTagName("table").get(1);
                JSON answer = jsonCreator(table);
                return (JSON)answer;
            }
            else{

                JSON answer = onlyOne(nextPage);
                return (JSON)answer;

            }

        }
        catch(Exception e){

            System.out.println("Could not find anything on 'USPTO' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on USPTO webstie. And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @param allResults :: Unordered list of Justia's result
     * @return JSON of results
     */
    public static JSON jsonCreator(HtmlTable allResults) throws Exception {

        JSONArray results = new JSONArray();

        List<HtmlTableRow> allTr = allResults.getHtmlElementsByTagName("tr");


        Boolean notFirst = true;

        for (HtmlElement tr : allTr) {

            // Skip the first Row
            if(notFirst){

                notFirst = false;
                continue;
            }

            HtmlElement patent = tr.getHtmlElementsByTagName("td").get(3).getHtmlElementsByTagName("a").get(0);

            String title = "";
            String inventors = "";
            String date_filed = "";
            String date_issued = "";
            String number = "";
            String assignee = "";
            String patentAbstract = "";

            try {
                // Get Patent Title
                title = patent.getTextContent().replaceAll("(  |\\n)", "");
            }catch(Exception e){

                System.out.println("Title Error");
            }




            HtmlPage patentPage = patent.click();

            HtmlBody patentBody = (HtmlBody) patentPage.getBody();

            HtmlTable patentTable = (HtmlTable) patentBody.getHtmlElementsByTagName("table").get(3);

            List<HtmlTableRow> patentTableRows = patentTable.getHtmlElementsByTagName("tr");

            try{
                patentAbstract = patentBody.getHtmlElementsByTagName("p").get(0).getTextContent().replaceAll("(  |\\n)", "");
            }
            catch(Exception e){
                System.out.println("Abstract ERROR");
            }


            for(HtmlTableRow row : patentTableRows) {

                String header = "";
                List<HtmlElement> th = row.getHtmlElementsByTagName("th");
                if(th.size() != 0)
                    header = th.get(0).getTextContent().replaceAll("( |\\n)", "");

                if (header.equalsIgnoreCase("Inventors:")) {

                    inventors = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
                } else if (header.equalsIgnoreCase("Appl.No.:")) {

                    number = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
                } else if (header.equalsIgnoreCase("Filed:")) {

                    date_filed = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
                } else if (header.equalsIgnoreCase("Assignee:")) {

                    assignee = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
                }

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


    public static JSON onlyOne(HtmlPage patentPage){

        JSONArray results = new JSONArray();

        String title = "";
        String inventors = "";
        String date_filed = "";
        String date_issued = "";
        String number = "";
        String assignee = "";
        String patentAbstract = "";

        HtmlBody patentBody = (HtmlBody) patentPage.getBody();

        HtmlTable patentTable = (HtmlTable) patentBody.getHtmlElementsByTagName("table").get(3);

        List<HtmlTableRow> patentTableRows = patentTable.getHtmlElementsByTagName("tr");

        try{
            patentAbstract = patentBody.getHtmlElementsByTagName("p").get(0).getTextContent().replaceAll("(  |\\n)", "");
        }
        catch(Exception e){
            System.out.println("Abstract ERROR");
        }


        for(HtmlTableRow row : patentTableRows) {

            String header = "";
            List<HtmlElement> th = row.getHtmlElementsByTagName("th");
            if(th.size() != 0)
                header = th.get(0).getTextContent().replaceAll("( |\\n)", "");

            if (header.equalsIgnoreCase("Inventors:")) {

                inventors = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
            } else if (header.equalsIgnoreCase("Appl.No.:")) {

                number = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
            } else if (header.equalsIgnoreCase("Filed:")) {

                date_filed = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
            } else if (header.equalsIgnoreCase("Assignee:")) {

                assignee = row.getHtmlElementsByTagName("td").get(0).getTextContent().replaceAll("(  |\\n)", "");
            }

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

        return results;

    }

}
