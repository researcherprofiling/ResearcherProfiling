import models.wrapper.sourceWrapper.interfaces.Getter;

import java.lang.*;
import java.lang.Integer;
import java.lang.Iterable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.*;

import net.sf.json.JSONObject;
import utils.Constants;
import utils.MyHTTP;

// Headless Browser imports
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import utils.linkUnit.linkUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

    /*
    STARTING SOURCE = http://federalreporter.nih.gov/
    FORM NAME = queryterms


     */

public class MyGetter implements Getter {

    public Object getResult(JSONObject searchConditions) {

        try {

            // Setup form input values
            String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
            String firstName = nameParts[0];
            String lastName = "";
            String affiliation = "";
            try{
                affiliation = searchConditions.getString("affiliation");
            }catch(Exception e){
                System.out.println("No Affiliation provided");
            }

            for (int i = 1; i < nameParts.length; i++) {
                lastName = lastName + nameParts[i];
            }

            String researcherName = firstName + " " + lastName;

            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);

            WebClient webClient = new WebClient(BrowserVersion.CHROME);

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            ArrayList<linkUnit> researcherLinks = searchGoogle(webClient, firstName + " " + lastName + " " + affiliation + "UIUC");
            ArrayList<linkUnit> potentialHomepageLink = searchGooglePatents(webClient, firstName + " " + lastName + " " + affiliation + "UIUC");


            JSON answer = jsonCreator(researcherLinks, potentialHomepageLink, researcherName, affiliation);

            System.out.println(answer);

            return answer;

        } catch (Exception e) {

            System.out.println("Could not find anything on 'Google' for the Search Query");
            System.out.println("ERROR REPORT: " + e.toString());
        }

        // Failure Case
        return null;
    }


    /**
     * This method creates information plate for the researcher in the form of JSON.
     *
     * @param researcherName :: Name of the researcher
     * @param affiliation :: Affiliation association of the reseearcher
     * @param homepageLink :: Google Patent predicted homepage
     * @param researcherLinks :: Relevant links received from Google
     * @return JSON of results
     */
    public static JSON jsonCreator(ArrayList<linkUnit> researcherLinks, ArrayList<linkUnit> homepageLink, String researcherName, String affiliation) throws Exception {

        JSONArray results = new JSONArray();

        String name = "";
        String born = "";
        String education = "";
        String academic = "";
        String homepage = "";
        String linkedIn = "";
        String about = "";
        String relatedPeople = "";
        String researchGate = "";


        // Create a new unit
        JSONObject jsonUnit = new JSONObject();

        name = researcherName;
        academic = findAcademicPages(researcherLinks).toString().replaceAll("(?:\\]|\\[)", "");
        linkedIn = findLinkedinPages(researcherLinks, researcherName, affiliation).toString().replaceAll("(?:\\]|\\[)", "");
        researchGate = findResearchGatePages(researcherLinks, researcherName, affiliation).toString().replaceAll("(?:\\]|\\[)", "");

        if(homepageLink.size() >= 1){

            //linkUnit homepageLinkUnit = homePageClassifier(researcherLinks, researcherName, affiliation);
            linkUnit homepageLinkUnit = homepageLink.get(0);
            homepage = homepageLinkUnit.getURL().toString().replaceAll("(?:\\]|\\[)", "");
            about = homepageLinkUnit.getDescription();
        }
        else{

            linkUnit homepageLinkUnit = homePageClassifier(researcherLinks, researcherName, affiliation);
            homepage = homepageLinkUnit.getURL().toString().replaceAll("(?:\\]|\\[)", "");
            about = homepageLinkUnit.getDescription();
        }

        jsonUnit.put("Name", name);
        //jsonUnit.put("Born", born);
        //jsonUnit.put("Education", education);
        jsonUnit.put("Academic", academic);
        jsonUnit.put("Homepage", homepage);
        jsonUnit.put("LinkedIn", linkedIn);
        jsonUnit.put("About", about);
        jsonUnit.put("ResearchGate", researchGate);
        //jsonUnit.put("Related-People", relatedPeople);

        results.add(jsonUnit);

        return results;
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

                            System.out.println("Faulty result unit found from Google Patents.");
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
     * This is the method which classifies the homepage of the researcher from the data
     * of the links collected from Google about the researcher.
     *
     * @param researcherLinks :: Top linkUnits of the researcher collected from Google
     * @param affiliation :: Affiliation association of the researcher
     * @param researcherName :: Name of the researcher
     * @return homepage :: HomePage link of hte researcher
     */
    public static linkUnit homePageClassifier(ArrayList<linkUnit> researcherLinks, String researcherName, String affiliation){

        linkUnit homepage = researcherLinks.get(0);
        ArrayList<linkUnit> homePageList = new ArrayList<linkUnit>();

        boolean researcherNameInLink = false;
        boolean eduInLink = false;
        boolean researcherNameInTitle = false;
        boolean contextRelatesKeywords = false;

        for(linkUnit link : researcherLinks){

            int score = 0;

            // Feature: Title of the webpage contains the Name of the researcher
            String title = link.getTitle();
            ArrayList<String> titleWords = new ArrayList<String>(Arrays.asList(title.split(" ")));
            ArrayList<String> researcherNameWords = new ArrayList<String>(Arrays.asList(researcherName.split(" ")));

            if (!Collections.disjoint(researcherNameWords, titleWords)) {

                score++;
            }

            // Feature: Context of the website relates to some keywords
            String context = link.getDescription();
            ArrayList<String> contextWords = new ArrayList<String>(Arrays.asList(context.split(" ")));
            if (!Collections.disjoint(researcherNameWords, new ArrayList<String>(Arrays.asList(Constants.contextKw)))) {

                score++;
            }

            // Feature: Name or abbreviation of the name in the URL of the webpage
            ArrayList<String> urlWords = new ArrayList<String>(Arrays.asList(link.getWords()));
            for(String urlWord : urlWords){

                try{

                    if(isOrderedSubString(urlWord, researcherName)){

                        score++;
                        break;
                    }
                }
                catch(Exception e){

                    System.out.println("Researcher Name in URL Check -- ");
                }
            }

            // Feature: has academic website .edu
            if (!Collections.disjoint(urlWords, new ArrayList<String>(Arrays.asList(Constants.academicKW)))) {

                score++;
            }

            if(score >= 3){
                homePageList.add(link);
            }
        }

        if(homePageList.size() > 0)
            return homePageList.get(0);

        else{
            return homepage;
        }
    }


    /**
     * This method checks if s1 is an ordered substring of s2 or not.
     * @param s1
     * @param s2
     * @return true/flase
     */
    public static boolean isOrderedSubString(String s1, String s2){

        int s1Length = s1.length();
        int s2Length = s2.length();

        int s1Index = 0;
        int s2Index = 0;

        while(s1Index < s1Length && s2Index < s2Length){

            if(s1.charAt(s1Index) == s2.charAt(s2Index)){

                s1Index++;
                s2Index++;
            }
            else{

                s2Index++;
            }
        }

        if(s1Index >= s1Length){

            return true;
        }
        else{

            return false;
        }


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
     * @param researcherName :: Name of the researcher
     * @param affiliation :: Affiliaiton related to the researcher
     * @return academicPages :: List of URL that are linkedin web-pages
     */
    public static ArrayList<String> findLinkedinPages(ArrayList<linkUnit> researcherLinks, String researcherName, String affiliation){

        ArrayList<String> linkedinPages = new ArrayList<String>();

        for(linkUnit link : researcherLinks){

            ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

            // Check if the URL words have anything common with the academic keywords
            if(!Collections.disjoint(words, new ArrayList<String>(Arrays.asList(Constants.linkedinKW)))){

                linkedinPages.add(link.getURL());
            }
        }

        if(linkedinPages.size() == 0) {

            WebClient webClient = new WebClient(BrowserVersion.CHROME);

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            // Do a newe query google search
            ArrayList<linkUnit> newResearcherLinks = searchGoogle(webClient, researcherName + " " + affiliation + " " + "linkedin");

            for (linkUnit link : newResearcherLinks) {

                ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

                // Check if the URL words have anything common with the academic keywords
                if (!Collections.disjoint(words, new ArrayList<String>(Arrays.asList(Constants.linkedinKW)))) {

                    linkedinPages.add(link.getURL());
                }

            }
        }

        return linkedinPages;
    }


    /**
     * This method given a list of linkUnits find those links that are ResearchGate web pages.
     * Web-Pages that may or may not be researcher profiles but surely are Researchgate webpages
     * i.e webpage belonging to an institution.
     *
     * @param researcherLinks :: List of linkUnits
     * @param researcherName :: Name of the researcher
     * @param affiliation :: Affiliaiton related to the researcher
     * @return academicPages :: List of URL that are Researchgate web-pages
     */
    public static ArrayList<String> findResearchGatePages(ArrayList<linkUnit> researcherLinks, String researcherName, String affiliation){

        ArrayList<String> researchGatePages = new ArrayList<String>();

        for(linkUnit link : researcherLinks){

            ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

            // Check if the URL words have anything common with the academic keywords
            if(!Collections.disjoint(words, Arrays.asList(Constants.researchgateKW))){

                researchGatePages.add(link.getURL());
            }
        }

        if(researchGatePages.size() == 0) {

            WebClient webClient = new WebClient(BrowserVersion.CHROME);

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.setJavaScriptTimeout(10000);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(10000);

            // Do a newe query google search
            ArrayList<linkUnit> newResearcherLinks = searchGoogle(webClient, researcherName + " " + affiliation + " " + "researchgate");

            for (linkUnit link : newResearcherLinks) {

                ArrayList<String> words = new ArrayList<String>(Arrays.asList(link.getWords()));

                System.out.println(words.toString());

                // Check if the URL words have anything common with the academic keywords
                if (!Collections.disjoint(words, Arrays.asList(Constants.researchgateKW))) {

                    researchGatePages.add(link.getURL());
                }

            }
        }

        return researchGatePages;
    }

}
