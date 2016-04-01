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

        try {
            // Turn of HTMLUnit Warnings
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

            WebClient webClient = new WebClient();

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            // Get the first page
            HtmlPage searchPage = webClient.getPage("http://federalreporter.nih.gov/");

            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = "";

            for (int i = 1; i < nameParts.length; i++) {
                lastName = lastName + nameParts[i];
            }

            String fiscalYear = "All";

            // Get the form that we are dealing with and within that form
            HtmlForm form = searchPage.getFormByName("queryterms");
            Iterable<HtmlElement> elements = form.getHtmlElementDescendants();

            // Get relevant web form HTML elements
            HtmlTextInput firstNameInput = (HtmlTextInput) form.getInputsByName("p_pi_first").get(0);
            HtmlTextInput lastNameInput = (HtmlTextInput) form.getInputsByName("p_pi_last").get(0);
            HtmlHiddenInput fiscalYearInput = (HtmlHiddenInput) form.getInputsByName("p_fy").get(0);
            HtmlAnchor submitButton = form.getOneHtmlElementByAttribute("a", "title", "Submit Query");

            // Fill up the Web Form
            firstNameInput.setValueAttribute(firstName);
            lastNameInput.setValueAttribute(lastName);
            fiscalYearInput.setValueAttribute(fiscalYear);

            // Submit the WebForm
            HtmlPage nextPage = submitButton.click();

            HtmlForm resultForm = nextPage.getFormByName("frmSearchResults");
            HtmlTable resultTable = resultForm.getOneHtmlElementByAttribute("table", "class", "proj_search_cont");

            JSON answer = jsonCreator(resultTable);
            return answer;

        } catch (Exception e) {

            System.out.println("Could not find anything on 'Federal Reporter' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on the Federal reporter website (http://federalreporter.nih.gov/). And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @param table :: Table at the Federal Reporter's Result Page
     * @return JSON of results
     */
    public static JSON jsonCreator(HtmlTable table){

        JSONArray results = new JSONArray();

        List<HtmlTableRow> tableRows = table.getRows();

        int rowCounter = 0;

        for(HtmlTableRow row : tableRows){

            // Skip 1st row
            // 1st row in Federal Reporter source is empty and therefore useless
            if(rowCounter == 0){

                rowCounter++;
                continue;
            }

            DomNodeList<HtmlElement> rowTds = row.getElementsByTagName("td");

            String title = "";
            String researcherName = "";
            String organization = "";
            String year = "";
            String agency = "";
            String totalCost = "";

            int count = 1;

            for(HtmlElement td : rowTds){

                if(count == 3)
                    title = ((HtmlTableCell)td).getTextContent();

                else if(count == 4) {

                    researcherName = ((HtmlTableCell) td).getTextContent();

                    if(td.getHtmlElementsByTagName("a").toString().length() > 4){

                        try{

                            // Clean text by removing 'et al.'
                            researcherName = researcherName.replace("et al.", "").replace(" ", "");
                            String moreNames = ((HtmlElement)td.getHtmlElementsByTagName("a").get(0)).getAttribute("onkeypress").toString();

                            moreNames = moreNames.replace("<br>", ";").replace("Tip(", "");
                            String[] moreNameElements = moreNames.split("(\\bTITLE\\b)");
                            moreNames = moreNameElements[0];
                            researcherName = researcherName + "; " + moreNames;
                        }
                        catch(Exception e){

                        }
                    }

                }

                else if(count == 5)
                    organization = ((HtmlTableCell)td).getTextContent();

                else if(count == 6)
                    year = ((HtmlTableCell)td).getTextContent();

                else if(count == 7) {

                    int subCount = 0;
                    HtmlTable subTable = (HtmlTable)td.getElementsByTagName("table").get(0);
                    DomNodeList<HtmlElement> subRowTds = subTable.getElementsByTagName("td");

                    for(HtmlElement subTd : subRowTds){

                        if (subCount == 0){
                            agency = ((HtmlTableCell) subTd).getTextContent();
                        }
                        else{
                            totalCost = ((HtmlTableCell) subTd).getTextContent();
                        }

                        subCount++;

                    }
                }

                count++;
                rowCounter++;

            }

            // Create a new unit
            JSONObject unit = new JSONObject();

            unit.put("Title", title.replace("\t","").replace("\n", ""));
            unit.put("Investigator", researcherName.replace("\t", "").replace("\n", ""));
            unit.put("Affiliation", organization.replace("\t","").replace("\n", ""));
            unit.put("Agency", agency.replace("\t","").replace("\n", ""));
            unit.put("Year", year.replace("\t","").replace("\n", ""));
            unit.put("Amount", totalCost.replace("\t","").replace("\n", "").replace(" ", ""));

            results.add(unit);

        }

        return results;
    }

}
