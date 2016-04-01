package utils.headlessBrowser;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Constants;
import utils.linkUnit.linkUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
STARTING SOURCE = http://federalreporter.nih.gov/
FORM NAME = queryterms


 */
public class HTMLUnit {

    public static void main(String args[]) throws Exception{

//        WebDriver driver = new HtmlUnitDriver();
//        driver.get("http://federalreporter.nih.gov/");
//        System.out.print(driver.getTitle());

        // turn off htmlunit warnings
        try{

            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

            WebClient webClient = new WebClient(BrowserVersion.CHROME);

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            // Setup form input values
            String firstName = "Jiawei";
            String lastName = "Han";

            ArrayList<linkUnit> researcherLinks = searchGoogle(webClient, "Jiawei Han UIUC");
            ArrayList<linkUnit> potentialHomepageLink = searchGooglePatents(webClient, "Jiawei Han UIUC");



            JSON answer = jsonCreator(researcherLinks, potentialHomepageLink);
            System.out.println(answer);

        }
        catch(Exception e){

            System.out.println("Could not find anything on 'USPTO' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }
    }


    /**
     * This method searches Google search engine with the appropriately given search string.
     * THe result units also known as link Units in context of the software are extracted and
     * stored.
     *
     * @param webClient :: Client used for the Google Search
     * @param searchString :: search query string used for the Google Search
     *
     * @return List of linkUnits
     */
    public static ArrayList<linkUnit> searchGoogle(WebClient webClient, String searchString){

        HtmlPage currentPage;
        ArrayList<linkUnit> linkUnits = new ArrayList<linkUnit>();

        try{

            currentPage = (HtmlPage) webClient.getPage("http://www.google.com");

            try{

                ((HtmlTextInput) currentPage.getElementByName("q")).setValueAttribute(searchString);
                HtmlElement searchBtn = currentPage.getElementByName("btnG");
                HtmlPage resultPage = searchBtn.click();

                HtmlBody resultBody = (HtmlBody)resultPage.getBody();

                List<HtmlDivision> results = resultBody.getElementsByAttribute("div", "class", "g");

                for(HtmlDivision div : results){

                    // Create link Unit and fill in information
                    // Data extraction
                    try{

                        String title = ((HtmlHeading3)div.getHtmlElementsByTagName("h3").get(0)).getTextContent();
                        String url = ((HtmlCitation)div.getElementsByTagName("cite").get(0)).getTextContent();
                        String description = ((HtmlSpan)div.getElementsByAttribute("span", "class", "st").get(0)).getTextContent();

                        linkUnit link = new linkUnit(url, title, description);

                        linkUnits.add(link);
                    }
                    catch(Exception e){

                        System.out.println("Faulty result unit found from Google.");
                    }


                }
            }
            catch(Exception e){

                System.out.println("Could not open the Google Web Page on the Browser");
            }
        }
        catch(Exception e){

            System.out.println("Could not open the Google Web Page on the Browser");
        }

        return linkUnits;
    }


    /**
     * This method searches Google Patents search engine with the appropriately given search string.
     * THe result units also known as link Units in context of the software are extracted and
     * stored.
     *
     * @param webClient :: Client used for the Google Search
     * @param searchString :: search query string used for the Google Search
     *
     * @return List of linkUnits
     */
    public static ArrayList<linkUnit> searchGooglePatents(WebClient webClient, String searchString){

        HtmlPage currentPage;
        ArrayList<linkUnit> linkUnits = new ArrayList<linkUnit>();

        try {

            currentPage = (HtmlPage) webClient.getPage("http://www.google.com/patents");

            try {

                ((HtmlTextInput) currentPage.getElementByName("q")).setValueAttribute(searchString);
                HtmlElement searchBtn = currentPage.getElementByName("btnG");
                HtmlPage resultPage = searchBtn.click();

                HtmlBody resultBody = (HtmlBody) resultPage.getBody();

                List<HtmlDivision> results = resultBody.getElementsByAttribute("div", "class", "g");
                List<HtmlCitation> citations = resultBody.getHtmlElementsByTagName("cite");

                for (HtmlDivision div : results) {

                    if(div.getAttribute("style").length() > 1){

                        // Create link Unit and fill in information
                        // Data extraction
                        try {

                            String title = ((HtmlHeading3) div.getHtmlElementsByTagName("h3").get(0)).getTextContent();
                            String url = ((HtmlCitation) div.getElementsByTagName("cite").get(0)).getTextContent();
                            String description = ((HtmlSpan) div.getElementsByAttribute("span", "class", "st").get(0)).getTextContent();

                            linkUnit link = new linkUnit(url, title, description);

                            linkUnits.add(link);
                        } catch (Exception e) {

                            System.out.println("Faulty result unit found from Google.");
                        }

                    }

                }
            }
            catch(Exception e){

                System.out.println("Could not open the Google Patent (About Aspect) Web Page on the Browser");
            }
        }
        catch(Exception e){

            System.out.println("Unable to open the Google Patents Page (Author About) in the web client.");
        }

        return linkUnits;
    }


    /**
     * This method goes through all the rows inside the Table presented as results for a search query
     * on Justia Patent website (http://patents.justia.com/). And then it collects the valuable
     * data and in the end presents a JSON.
     *
     * @return JSON of results
     */
    public static JSON jsonCreator(ArrayList<linkUnit> researcherLinks, ArrayList<linkUnit> homepageLink) throws Exception {

        JSONArray results = new JSONArray();

        String name = "";
        String born = "";
        String education = "";
        String homepage = "";
        String linkedIn = "";
        String about = "";
        String relatedPeople = "";

        // Create a new unit
        JSONObject jsonUnit = new JSONObject();

        if(homepageLink.size() >= 1){

            linkUnit homepageLinkUnit = homepageLink.get(0);

            homepage = homepageLinkUnit.getURL();
            about = homepageLinkUnit.getDescription();
            education = findAcademicPages(researcherLinks).toString();
            linkedIn = findLinkedinPages(researcherLinks).toString();
        }
        else{

            homepage = homePageClassifier(researcherLinks);
        }

        jsonUnit.put("Name", name);
        jsonUnit.put("Born", born);
        jsonUnit.put("Education", education);
        jsonUnit.put("Homepage", homepage);
        jsonUnit.put("LinkedIn", linkedIn);
        jsonUnit.put("About", about);
        jsonUnit.put("Related-People", relatedPeople);

        results.add(jsonUnit);

        return results;
    }


    /**
     * This is the method which classifies the homepage of the researcher from the data
     * of the links collected from Google about the researcher.
     *
     * @param researcherLinks :: Top linkUnits of the researcher collected from Google
     * @return homepage :: HomePage link of hte researcher
     */
    public static String homePageClassifier(ArrayList<linkUnit> researcherLinks){

        String homepage = "";

        // Feature: Title of the webpage contains the Name of the researcher
        // Feature: has academic website .edu
        // Feature: Name or abbreviation of the name in the URL of the webpage
        // Feature: Context of the website relates to some keywords

        boolean researcherNameInLink = false;
        boolean eduInLink = false;
        boolean researcherNameInTitle = false;
        boolean contextRelatesKeywords = false;




        return homepage;
    }


    /**
     * This method given a list of linkUnits find those links that are academic web pages.
     * Web-Pages that may or may not be researcher profiles but surely are academic webpages
     * i.e webpage belonging to an institution.
     *
     * @param researcherLinks :: List of linkUnits
     * @return academicPages :: List of URL that are academic web-pages
     */
    public static ArrayList<String> findAcademicPages(ArrayList<linkUnit> researcherLinks){

        ArrayList<String> academicPages = new ArrayList<String>();

        for(linkUnit link : researcherLinks){

            ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

            // Check if the URL words have anything common with the academic keywords
            if(!Collections.disjoint(words, Arrays.asList(Constants.academicKW))){

                academicPages.add(link.getURL());
            }
        }

        return academicPages;
    }


    /**
     * This method given a list of linkUnits find those links that are linkedin web pages.
     * Web-Pages that may or may not be researcher profiles but surely are linkedin webpages
     * i.e webpage belonging to an institution.
     *
     * @param researcherLinks :: List of linkUnits
     * @return academicPages :: List of URL that are linkedin web-pages
     */
    public static ArrayList<String> findLinkedinPages(ArrayList<linkUnit> researcherLinks){

        ArrayList<String> linkedinPages = new ArrayList<String>();

        for(linkUnit link : researcherLinks){

            ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

            System.out.println(link.getURL());
            // Check if the URL words have anything common with the academic keywords
            if(!Collections.disjoint(words, Arrays.asList(Constants.linkedinKW))){

                linkedinPages.add(link.getURL());
            }
        }

        return linkedinPages;
    }

}
