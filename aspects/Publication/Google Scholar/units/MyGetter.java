import models.wrapper.sourceWrapper.interfaces.Getter;
import net.sf.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Exception;
import java.lang.ProcessBuilder;
import java.lang.System;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;


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


			String baseURL = "http://scholar.google.com";
			// Setup form input values
			String[] nameParts = searchConditions.getString("fullName").split("(\\b \\b)");
			String firstName = nameParts[0];
			String lastName = nameParts[1];

			// Submit the WebForm
			HtmlPage nextPage = webClient.getPage(baseURL + "/scholar?hl=en&q=" + firstName + "+" + lastName);

			HtmlDivision gsBody = nextPage.getHtmlElementById("gs_bdy");

			HtmlHeading4 authorh4 = gsBody.getOneHtmlElementByAttribute("h4", "class", "gs_rt2");

			HtmlAnchor authorProfileLink = (HtmlAnchor)authorh4.getHtmlElementsByTagName("a").get(0);

			String authorExtension = authorProfileLink.getAttribute("href");

			// If we are in the author page lets extend the page:
			// 1. show the max page result size (i.e 100)
			// 2. Sort the results by Citation Numbers
			HtmlPage extendedAuthorPage = webClient.getPage(baseURL + authorExtension + "&pagesize=100&view_op=list_works");

			// Now grab the table of results from the author page
			HtmlTableBody results = (HtmlTableBody)extendedAuthorPage.getElementById("gsc_a_b");

			JSON answer = jsonCreator(results);
			return (JSON)answer;

		}
		catch(Exception e){

			System.out.println("Could not find anything on 'Google Scholar' for the Search Query");
			System.out.println("ERROR REPORT: " + e.toString());
		}

		// Failure Case
		return null;

	}


	/**
	 * This method goes through all the rows inside the Table presented as results for a search query
	 * on Google Scholar website (http://scholar.google.com). And then it collects the valuable
	 * data and in the end presents a JSON.
	 *
	 * @param allResults :: Unordered list of DBLP's result
	 * @return JSON of results
	 */
	public static JSON jsonCreator(HtmlElement allResults) {

		JSONArray results = new JSONArray();

		List<HtmlElement> tableRows = allResults.getElementsByAttribute("tr", "class", "gsc_a_tr");

		for (HtmlElement row : tableRows) {

			List<HtmlTableDataCell> dataCells = row.getHtmlElementsByTagName("td");

			int count = 0;

			String title = "";
			String authors = "";
			String year = "";
			String publisher = "";
			String citation = "";

			for (HtmlElement td : dataCells) {

				if(count == 0){

					title = td.getHtmlElementsByTagName("a").get(0).getTextContent();
					List<HtmlElement> divisions = td.getHtmlElementsByTagName("div");

					if(divisions.size() >= 1)
						authors = divisions.get(0).getTextContent();

					if(divisions.size() >= 2)
						publisher = divisions.get(1).getTextContent();

				}
				else if(count == 1){

					List<HtmlElement> citationData = td.getHtmlElementsByTagName("a");

					if(citationData.size() >= 1)
						citation = citationData.get(0).getTextContent();

				}
				else if(count == 2){

					year = td.getTextContent();
				}

				count++;

			}

			// Create a new unit
			JSONObject jsonUnit = new JSONObject();

			jsonUnit.put("Title", title);
			jsonUnit.put("Authors", authors);
			jsonUnit.put("Citations", citation);
			jsonUnit.put("Year", year);
			jsonUnit.put("Publisher", publisher);

			results.add(jsonUnit);

		}

		return results;
	}

	public static Object runScholarPy(JSONObject searchConditions){

		try {
			String[] command = {"python", System.getProperty("user.dir") + "scholar.py-master/scholar.py", "-a", "\""+searchConditions.getString("fullName")+"\""};
			ProcessBuilder pb = new ProcessBuilder(command);
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			String ret = "";
			while ((line = reader.readLine()) != null) {
				ret += line + "\n";
			}
			p.waitFor();
			reader.close();
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}


}