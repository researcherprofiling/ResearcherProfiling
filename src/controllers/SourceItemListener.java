package controllers;

import models.Model;
import models.wrapper.GeneralWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class SourceItemListener implements ActionListener {

    private String source;
    private String aspect;
    private Model model;

    public SourceItemListener(String source, String aspect, Model model) {
        this.source = source;
        this.aspect = aspect;
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object[] options = {"Activate", "Deactivate", "Show details"};
        int usersOption = JOptionPane.showOptionDialog(
                null,
                "Do you want to activate or deactivate source " + source +"?",
                source,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]
        );
        if (usersOption == 2) {
            try {
                Desktop.getDesktop().open(new File(GeneralWrapper.basePath + "/" + aspect + "/" + source));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        else model.setActivation(aspect, source, usersOption == 0);
    }
}
