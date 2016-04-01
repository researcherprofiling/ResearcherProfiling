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
            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = nameParts[1];

            // Submit the WebForm
            HtmlPage nextPage = webClient.getPage("http://dblp.uni-trier.de/search?q=" + firstName + "+" + lastName);

            // First check if an exact name is present or not
            nextPage = dblpAuthorExists(nextPage, firstName, lastName);

            // Scroll to the bottom of the page to get all the results
            HtmlElement body = nextPage.getBody();

            HtmlAnchor scrollToTheBottom = body.getOneHtmlElementByAttribute("a", "href", "#footer");

            // Click to scroll to the bottom
            scrollToTheBottom.click();

            HtmlElement results = body.getOneHtmlElementByAttribute("div", "id", "publ-section");

            JSON answer = jsonCreator(results);
            return (JSON)answer;

        }
        catch(Exception e){

            System.out.println("Could not find anything on 'DBLP' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method checks if in the result section of dblp an exact author exists or not. The dblp
     * website depending on the search query finds correct related authors and displays them on the top.
     * It showcases exact author match and then similar author match. If the exact author match exists then we would like
     * to search the information on that exact match else not. This method checks if that exact match exists or not. And
     * if the exact match exists the author page is returned else the normal result page is returned.
     *
     * @param nextPage :: Initial Result Page of dblp
     * @param firstName :: first name of the author
     * @param lastName :: last name of the author
     * @return page :: Author/Result Page
     */
    public static HtmlPage dblpAuthorExists(HtmlPage nextPage, String firstName, String lastName) throws Exception{

        WebClient webClient = new WebClient();

        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.setJavaScriptTimeout(10000);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setTimeout(10000);


        boolean exactNamePresent = false;
        HtmlDivision authorSection = (HtmlDivision)nextPage.getElementById("completesearch-authors");

        boolean openValve = false;

        ArrayList<HtmlElement> exactMatches = new ArrayList<HtmlElement>();

        for(HtmlElement authorEle : authorSection.getHtmlElementDescendants()){

            // while valve is open
            if(openValve){

                if(authorEle.getClass().toString().equalsIgnoreCase("class com.gargoylesoftware.htmlunit.html.HtmlListItem")){

                    exactMatches.add(authorEle);
                    break;
                }

            }

            if(authorEle.getTextContent().equalsIgnoreCase("Exact matches")){

                openValve = true;
            }

            else if(authorEle.getTextContent().equalsIgnoreCase("Likely matches")){

                openValve = false;
                break;
            }
        }

        // Check if there were any matches
        if(exactMatches.size() >= 1)
            exactNamePresent = true;

        HtmlPage page = nextPage;

        if(exactNamePresent){

            page = webClient.getPage("http://dblp.uni-trier.de/pers/hd/" + lastName.toLowerCase().charAt(0) + "/" + lastName + ":" + firstName);
        }

        return page;
    }

    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on the Federal reporter website (http://dblp.uni-trier.de/). And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @param allResults :: Unordered list of DBLP's result
     * @return JSON of results
     */
    public static JSON jsonCreator(HtmlElement allResults) {

        JSONArray results = new JSONArray();

        List<HtmlElement> units = allResults.getElementsByAttribute("ul", "class", "publ-list");


        for (HtmlElement unit : units) {

            int count = 0;

            List<HtmlListItem> allLi = unit.getHtmlElementsByTagName("li");

            for (HtmlElement li : allLi) {

                if(count >= 30){
                     break;
                }

                String currentYear = "";

                String liClass = li.getAttribute("class").toString();

                if (liClass.equalsIgnoreCase("year")) {

                    currentYear = li.getTextContent();
                }

                else if(liClass.equalsIgnoreCase("drop-down") || liClass.equalsIgnoreCase("select-on-click")){

                }
                else if(li.toString().equalsIgnoreCase("HtmlListItem[<li>]")){

                }

                // Else it is an article
                else {

                    count++;

                    HtmlDivision data = li.getOneHtmlElementByAttribute("div", "class", "data");
                    List<HtmlSpan> allSpans = data.getHtmlElementsByTagName("span");

                    String authors = "";
                    String title = "";
                    String publisher = "";

                    for (HtmlSpan span : allSpans) {

                        String itemProp = span.getAttribute("itemprop");
                        String className = span.getAttribute("class");

                        if (itemProp.equalsIgnoreCase("author")) {

                            authors = authors + span.getTextContent() + "; ";
                        } else if (className.equalsIgnoreCase("title")) {

                            title = span.getTextContent();
                        }else if (itemProp.equalsIgnoreCase("name")) {

                            publisher = span.getTextContent();
                        } else if (itemProp.equalsIgnoreCase("datePublished")) {

                            currentYear = span.getTextContent();
                        }
                    }

                    // Create a new unit
                    JSONObject jsonUnit = new JSONObject();

                    jsonUnit.put("Title", title);
                    jsonUnit.put("Authors", authors);
                    jsonUnit.put("Year", currentYear);
                    jsonUnit.put("Publisher", publisher);

                    results.add(jsonUnit);

                }

            }


        }
        return results;
    }

}
