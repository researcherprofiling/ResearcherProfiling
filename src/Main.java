import controllers.MainWindowListener;
import controllers.RestartSearchButtonListener;
import controllers.SearchButtonListener;
import models.Model;
import views.MessageWindow;
import views.View;

public class Main {

    public static void main(String[] args) {
        MessageWindow messageWindow = new MessageWindow();
        System.setOut(messageWindow.getOutStream());
        System.setErr(messageWindow.getErrStream());
        System.out.println("This is normal message window.");
        System.err.println("This is error message window.");
        View view = new View();
        Model model = new Model();
        view.addWindowListener(new MainWindowListener(model));
        model.addObserver(view);
        model.fireEvent("registration", model);
        SearchButtonListener sbl = new SearchButtonListener(model, view);
        RestartSearchButtonListener rsbl = new RestartSearchButtonListener(model, view);
        view.setSearchButtonListener(sbl);
        view.setRestartSearchButtonListener(rsbl);
    }
}
