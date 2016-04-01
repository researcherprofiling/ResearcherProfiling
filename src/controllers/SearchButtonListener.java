package controllers;

import models.Model;
import net.sf.json.JSONObject;
import views.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static utils.Utilities.getNameTokens;

public class SearchButtonListener implements ActionListener {

    private Model model;
    private View view;

    public SearchButtonListener(Model m, View v) {
        model = m;
        view = v;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = view.getInputName();
        String affiliation = view.getInputAff();
        if (name.equals("Name, i.e. Jiawei Han")) {
            return;
        }
        JSONObject searchConditions = new JSONObject();
        String[] nameTokens = getNameTokens(name);
        searchConditions.put("first", nameTokens[0]);
        searchConditions.put("last", nameTokens[1]);
        if (nameTokens[2].length() != 0) {
            searchConditions.put("middle", nameTokens[2]);
        }
        searchConditions.put("fullName", name);
        if(!affiliation.equals("Affiliation, i.e. UIUC")) {
            searchConditions.put("affiliation", affiliation);
        }
        model.search(searchConditions);
    }
}
