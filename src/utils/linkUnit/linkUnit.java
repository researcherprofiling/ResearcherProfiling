package utils.linkUnit;

import utils.Constants;

public class linkUnit {

    private String url;
    private String title;
    private String description;
    private Constants.linkType linkType;
    private String[] words;
    private boolean selected;

    public linkUnit(){
    }

    public linkUnit(String url, String title, String description){

        this.url = url;
        this.title = title;
        this.selected = false;
        this.description = description;
        this.words = linktoWords(url);
    }

    public String getURL(){
        return this.url;
    }

    public String getTitle(){
        return this.title;
    }

    public String getDescription(){
        return this.description;
    }

    public Constants.linkType getLinkType(){
        return this.linkType;
    }

    public String[] getWords(){
        return words;
    }

    /**
     * This method extracts all the words from a web-link.
     * @param url
     * @return
     */
    public String[] linktoWords(String url){

        String[] words = url.split("(:)|(/)|(\\.)");

        return words;
    }

    /**
     * This method selects the link unit that is chosen for the final
     * information. I.e Relevant Link
     */
    public void selectLink(){
        this.selected = true;
    }
}
