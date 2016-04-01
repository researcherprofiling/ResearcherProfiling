package controllers;

import models.Model;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindowListener extends WindowAdapter {

    private Model model;

    public MainWindowListener(Model model) {
        this.model = model;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        model.stop();
    }
}