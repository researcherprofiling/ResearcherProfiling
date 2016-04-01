package views;

import controllers.SourceItemListener;
import events.Event;
import models.Model;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

public class View extends JFrame implements Observer {

    private JMenuBar menubar;
    private SearchPanel spanel;
    private ResultPanel rpanel;

    public View() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTitle("Researcher Profiling System");
        this.setLayout(new BorderLayout(5, 5));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        /* Initialize result panel */
        this.spanel = new SearchPanel();
        this.getContentPane().add(this.spanel, BorderLayout.NORTH);
        this.rpanel = new ResultPanel();
        this.getContentPane().add(this.rpanel, BorderLayout.CENTER);
        this.pack();
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(
                (int)(size.getWidth() - this.getWidth())/2,
                (int)(size.getHeight() - this.getHeight())/2
        );
        this.setMinimumSize(this.getSize());
        this.setResizable(false);
        this.setVisible(true);
    }

    @Override
    public void update(Observable o, Object arg) {
        events.Event event = (Event)arg;
        switch (event.getName()) {
            case "start search":
                spanel.disableInput();
                break;
            case "search results":
                this.remove(this.rpanel);
                this.rpanel = new ResultPanel();
                this.getContentPane().add(this.rpanel, BorderLayout.CENTER);
                rpanel.display(JSONObject.fromObject(event.getData()));
                spanel.enbaleRestartSearchButton();
                spanel.enableInput();
                break;
            case "registration":
                spanel.enableInput();
                initializeMenuBar((Model)event.getData());
                spanel.promptAndFocus();
                break;
            case "re-search results":
                this.remove(this.rpanel);
                this.rpanel = new ResultPanel();
                this.getContentPane().add(this.rpanel, BorderLayout.CENTER);
                rpanel.display(JSONObject.fromObject(event.getData()));
                spanel.enbaleRestartSearchButton();
                spanel.enableInput();
                break;
        }
        this.validate();
        this.repaint();
    }

    public void initializeMenuBar(Model model) {
        JSONObject registeredAspects = model.getRegisteredAspects();
        menubar  = new JMenuBar();
        JMenu registration = new JMenu("Registration");
        menubar.add(registration);
        for (Object k : registeredAspects.keySet()) {
            final String aspect = (String)k;
            JMenu aspectItem = new JMenu(aspect);
            registration.add(aspectItem);
            JSONArray sources = registeredAspects.getJSONArray(aspect);
            for (Object s : sources) {
                final String source = (String)s;
                JMenuItem sourceItem = new JMenuItem(source);
                sourceItem.addActionListener(new SourceItemListener(source, aspect, model));
                aspectItem.add(sourceItem);
            }
        }
        this.setJMenuBar(menubar);
        spanel.enableSearchButton();
        this.validate();
        this.repaint();
    }

    public String getInputName() {
        return spanel.getInputName();
    }

    public String getInputAff() {
        return spanel.getInputAffiliation();
    }

    public void setSearchButtonListener(ActionListener l) {
        spanel.setSearchButtonListener(l);
    }

    public void setRestartSearchButtonListener(ActionListener l) {
        spanel.setRestartSearchButtonListener(l);
    }

    public JSONObject getFeedback() {
        return rpanel.getFeedBack();
    }

}
