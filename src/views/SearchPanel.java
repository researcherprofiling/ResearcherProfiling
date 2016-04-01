package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SearchPanel extends JPanel {

    private JTextField nameInput;
    private JTextField affiliationInput;
    private JButton restartSearchButton;
    private JButton searchButton;

    public SearchPanel() {
        setSize(800, 80);
        setBackground(new Color(231, 231, 231));
        setPreferredSize(new Dimension(800, 80));
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        //  Set up and add name input
        nameInput = new JTextField();
        nameInput.setPreferredSize(new Dimension((this.getWidth() / 2) - 5, this.getHeight() - 40));
        nameInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                if (nameInput.getText().equals("Name, i.e. Jiawei Han")) {
                    nameInput.setText("");
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (nameInput.getText().length() == 0) {
                    nameInput.setText("Name, i.e. Jiawei Han");
                }
            }
        });
        add(this.nameInput);
        //  Set up and add affiliation input
        affiliationInput = new JTextField();
        affiliationInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                if (affiliationInput.getText().equals("Affiliation, i.e. UIUC")) {
                    affiliationInput.setText("");
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (affiliationInput.getText().length() == 0) {
                    affiliationInput.setText("Affiliation, i.e. UIUC");
                }
            }
        });
        affiliationInput.setPreferredSize(new Dimension((this.getWidth() / 2) - 5, this.getHeight() - 40));
        add(this.affiliationInput);
        //  Set up and add search button
        searchButton = new JButton("Get Profile");
        searchButton.setEnabled(false);
        searchButton.setSize(120, 30);
        add(searchButton);
        //  Ste up and add restart search button
        restartSearchButton = new JButton("Redo search");
        restartSearchButton.setEnabled(false);
        restartSearchButton.setSize(120, 30);
        add(restartSearchButton);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                requestFocus();
            }
        });
        disableInput();
    }

    public void enableInput() {
        nameInput.setEnabled(true);
        affiliationInput.setEnabled(true);
    }

    public void disableInput() {
        nameInput.setEnabled(false);
        affiliationInput.setEnabled(false);
    }

    public void enbaleRestartSearchButton() {
        restartSearchButton.setEnabled(true);
    }

    public void enableSearchButton() {
        searchButton.setEnabled(true);
    }

    public String getInputName() {
        return this.nameInput.getText();
    }

    public String getInputAffiliation() {
        return this.affiliationInput.getText();
    }

    public void promptAndFocus() {
        this.searchButton.requestFocus();
        this.nameInput.setText("Name, i.e. Jiawei Han");
        this.affiliationInput.setText("Affiliation, i.e. UIUC");
    }

    public void setSearchButtonListener(ActionListener l) {
        searchButton.addActionListener(l);
    }

    public void setRestartSearchButtonListener(ActionListener l) {
        restartSearchButton.addActionListener(l);
    }

}

