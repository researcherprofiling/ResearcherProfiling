package views;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class MessageWindow {

    private JFrame window;
    private JTextArea outArea;
    private JTextArea errArea;
    private JScrollPane outPane;
    private JScrollPane errPane;
    private PrintStream outStream;
    private PrintStream errStream;

    public MessageWindow() {
        window = new JFrame();
        window.setTitle("Message Window");
        window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
        window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        outArea = new JTextArea();
        outArea.setEditable(false);
        errArea = new JTextArea();
        errArea.setEditable(false);
        Dimension scrollSize = new Dimension(600, 300);
        outPane = new JScrollPane(outArea);
        outPane.setSize(scrollSize);
        outPane.setPreferredSize(scrollSize);
        errPane = new JScrollPane(errArea);
        outPane.setSize(scrollSize);
        errPane.setPreferredSize(scrollSize);
        window.getContentPane().add(outPane);
        window.getContentPane().add(errPane);
        window.pack();
        window.repaint();
        outStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                outArea.append(String.valueOf((char)b));
                outArea.setCaretPosition(outArea.getDocument().getLength());
            }
        });
        errStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                errArea.append(String.valueOf((char)b));
                errArea.setCaretPosition(errArea.getDocument().getLength());
                errArea.moveCaretPosition(errArea.getDocument().getLength());
            }
        });
        outPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        errPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        errPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        window.setVisible(true);
    }

    public PrintStream getOutStream() {
        return outStream;
    }

    public PrintStream getErrStream() {
        return errStream;
    }
}
