package models;

import events.Event;
import net.sf.json.JSONObject;

import javax.swing.*;
import java.util.Observable;
import java.util.concurrent.ExecutionException;

public class Model extends Observable {

    private class SearchTask extends SwingWorker<JSONObject, JSONObject> {

        private JSONObject searchConditions;
        private Model model;

        public SearchTask(JSONObject searchConditions, Model model) {
            this.searchConditions = searchConditions;
            this.model = model;
        }

        @Override
        protected JSONObject doInBackground() throws Exception {
            return handler.search(searchConditions);
        }

        @Override
        protected void done() {
            try {
                results = get();
                currentlySearching = false;
                System.out.flush();
                System.out.println("======== Done searching! =======");
                model.fireEvent("search results", results);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                results = new JSONObject();
            }
        }
    }

    private class RestartSearchTask extends SwingWorker<JSONObject, JSONObject> {

        private JSONObject searchConditions;
        private Model model;

        public RestartSearchTask(JSONObject searchConditions, Model model) {
            this.searchConditions = searchConditions;
            this.model = model;
        }

        @Override
        protected JSONObject doInBackground() throws Exception {
            return handler.redoSearch(searchConditions);
        }

        @Override
        protected void done() {
            try {
                results = get();
                currentlySearching = false;
                System.out.flush();
                System.out.println("======== Done searching! =======");
                model.fireEvent("re-search results", results);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                results = new JSONObject();
            }
        }
    }

    private SearchHandler handler;
    private JSONObject results;
    private boolean currentlySearching;

    public Model () {
        this.handler = new SearchHandler();
        this.results = new JSONObject();
        this.currentlySearching = false;
        this.setChanged();
    }

    public void search(JSONObject searchConditions) {
        if (currentlySearching) return;
        currentlySearching = true;
        fireEvent("start search", null);
        SearchTask worker = new SearchTask(searchConditions, this);
        worker.execute();
    }

    public void redoSearch(JSONObject searchConditions) {
        if (currentlySearching) return;
        currentlySearching = true;
        fireEvent("start search", null);
        RestartSearchTask worker = new RestartSearchTask(searchConditions, this);
        worker.execute();
    }

    public void fireEvent(String name, Object args) {
        setChanged();
        notifyObservers(new Event(name, args));
    }

    public JSONObject getRegisteredAspects() {
        return handler.getRegisteredAspects();
    }

    public void setActivation(String aspect, String source, boolean activated) {
        handler.setActivation(aspect, source, activated);
    }

    public void stop() {
        handler.stop();
    }

}
