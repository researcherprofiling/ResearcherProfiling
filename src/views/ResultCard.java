package views;

import models.schema.Field;
import models.schema.Schema;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Constants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Map;
import java.util.Stack;


public class ResultCard extends JPanel{

    private class RecordNode implements MutableTreeNode {

        private JSONObject data;
        public int relevance;   //  -1 for irrelevant, 1 for relevant, 0 for unclassified
        public boolean[] clustered;

        public RecordNode(JSONObject data) {
            this.data = data;
            this.relevance = 0;
            this.clustered = new boolean[data.getJSONArray("Original record(s)").size()];
        }

        @Override
        public String toString() {
            String txt = "<html>";
            for (Field field : schema.getAllFields()) {
                if (data.containsKey(field.fieldName) && field.convertToString(data).trim().length() > 0 ) {
                    txt += "<b>" + field.fieldName + "</b>: " + field.convertToString(data).replace("\n", "<br />") + "<p>";
                }
            }
            txt += "Record provided by source(s): " + data.getString("Provided by") + "</html>";
            return txt;
        }

        @Override
        public void insert(MutableTreeNode child, int index) {}

        @Override
        public void remove(int index) {}

        @Override
        public void remove(MutableTreeNode node) {}

        @Override
        public void setUserObject(Object object) {
            this.data = JSONObject.fromObject(object);
        }

        public JSONObject getUserObject() {
            return this.data;
        }

        @Override
        public void removeFromParent() {}

        @Override
        public void setParent(MutableTreeNode newParent) {}

        @Override
        public TreeNode getChildAt(int childIndex) {
            return null;
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public TreeNode getParent() {
            return null;
        }

        @Override
        public int getIndex(TreeNode node) {
            return 0;
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public Enumeration children() {
            return null;
        }
    }

    private JTree tree;
    private Schema schema;
    private JScrollPane scrollPane;

    public ResultCard(JSONObject data) {
        super();
        this.schema = Schema.fromJSONArray(data.getJSONArray("schema"));
        this.setSize(800, 540);
        this.setPreferredSize(new Dimension(800, 540));
        this.setBackground(new Color(255, 255, 255));
        display(data.getJSONObject("results"));
    }

    public void display(JSONObject results) {

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Keywords:");

        JSONObject relevant = results.getJSONObject("relevant");
        for (Object e : relevant.entrySet()) {
            Map.Entry<String, JSONArray> ent = (Map.Entry)e;
            String[] kwAndUniqueKWs = ent.getKey().split(Constants.uniquekwDelimiter);
            String[] kws = kwAndUniqueKWs[0].split(Constants.kwDelimiter);
            JSONArray group = ent.getValue();
            if (group.size() == 0) continue;
            String groupName = "<html><b>Frequent words</b>:";
            if(ent.getKey().equalsIgnoreCase("About the Researcher")){

                groupName = "<html><b>About the Researcher</b></html>";
            }
            else {
                for (int i = 0; i < kws.length; i++) {
                    groupName += kws[i] + ", ";
                }
                groupName = groupName.substring(0, groupName.length() - 2);
                groupName += ";  <b>Identifying words</b>:";
                kws = kwAndUniqueKWs[1].split(Constants.kwDelimiter);
                for (int i = 0; i < kws.length; i++) {
                    groupName += kws[i] + ", ";
                }
                groupName = groupName.substring(0, groupName.length() - 2);
                groupName += "</html>";
            }
            DefaultMutableTreeNode currentGroupNode = new DefaultMutableTreeNode(groupName);
            top.add(currentGroupNode);
            for (int j = 0; j < group.size(); j++) {
                JSONObject current = group.getJSONObject(j);
                RecordNode node = new RecordNode(current);
                currentGroupNode.add(node);
            }
        }

        JSONArray irrelevant = results.getJSONArray("irrelevant");
        DefaultMutableTreeNode irrelevantGroup = new DefaultMutableTreeNode("Possibly Irrelevant:");
        for (int i = 0; i < irrelevant.size(); i++) {
            JSONObject rec = irrelevant.getJSONObject(i);
            RecordNode node = new RecordNode(rec);
            irrelevantGroup.add(node);
        }
        top.add(irrelevantGroup);

        this.tree = new JTree(top);
        MouseListener ml = new MouseAdapter() {

            class PopUpMenu extends JPopupMenu {
                RecordNode node;
                JMenuItem showOriginal;
                public PopUpMenu(RecordNode rn){
                    node = rn;
                    showOriginal = new JMenuItem("Show original record(s)");
                    JSONArray origRecs = node.data.getJSONArray("Original record(s)");
                    showOriginal.addActionListener(e -> {
                        JOptionPane.showMessageDialog(this, origRecs.toString(1));
                        for(int i=0; i<origRecs.size(); i++) {
                            JSONObject rec = origRecs.getJSONObject(i);
                            int choice = JOptionPane.showConfirmDialog(this, rec.toString(1), "Does this record belong to the feedback group?", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                rn.clustered[i] = true;
                            }
                        }
                    });
                    add(showOriginal);
                }
            }

            public void mouseClicked(MouseEvent e) {
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selPath == null || !tree.getModel().isLeaf(selPath.getLastPathComponent()) ||
                        !(selPath.getLastPathComponent() instanceof RecordNode)) return;
                if (e.getClickCount() >= 2) {
                    int choice = JOptionPane.showConfirmDialog(null, "Is this record relevant?",
                            "Relevance Feedback", JOptionPane.YES_NO_OPTION);
                    if ( choice == JOptionPane.YES_OPTION) {
                        ((RecordNode)selPath.getLastPathComponent()).relevance = 1;
                    } else if (choice == JOptionPane.NO_OPTION) {
                        ((RecordNode)selPath.getLastPathComponent()).relevance = -1;
                    }
                }
                else if (SwingUtilities.isRightMouseButton(e)) {
                    PopUpMenu menu = new PopUpMenu((RecordNode)selPath.getLastPathComponent());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

        };
        tree.addMouseListener(ml);
        tree.setRootVisible(false);
        tree.setScrollsOnExpand(false);
        tree.setRowHeight(0);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            private Border border = BorderFactory.createEmptyBorder ( 6, 6, 6, 6 );
            private ImageIcon loadIcon(String iconName) throws Exception{
                Image img = ImageIO.read(getClass().getResource(iconName));
                BufferedImage scaledImg = new BufferedImage(15,15,BufferedImage.TYPE_INT_ARGB);
                scaledImg.createGraphics().drawImage(img, 0, 0, 15, 15, null);
                return new ImageIcon(scaledImg);
            }
            public Component getTreeCellRendererComponent ( JTree tree, Object value, boolean sel,
                                                            boolean expanded, boolean leaf, int row,
                                                            boolean hasFocus )
            {
                JLabel label = ( JLabel ) super
                        .getTreeCellRendererComponent ( tree, value, sel, expanded, leaf, row,
                                hasFocus );
                label.setBorder ( border );
                if (leaf && value instanceof RecordNode) {
                    try {
                        RecordNode node = (RecordNode)value;
                        if (node.relevance == 1) {
                            setIcon(loadIcon("pics/checked.png"));
                        } else if (node.relevance == -1) {
                            setIcon(loadIcon("pics/crossed.png"));
                        } else {
                            setIcon(loadIcon("pics/unknown.png"));
                        }
                    } catch (Exception e) {
                        System.out.println("Error setting icon for tree node.");
                        e.printStackTrace();
                    }

                }
                return label;
            }
        });
        this.tree.putClientProperty("JTree.lineStyle", "Horizontal");
        this.scrollPane = new JScrollPane(this.tree);
        this.add(this.scrollPane);
        this.scrollPane.setSize(800, 535);
        this.scrollPane.setPreferredSize(new Dimension(800, 535));
        this.tree.setVisible(true);
        this.scrollPane.setVisible(true);
    }

    public JSONObject getFeedBack() {
        JSONArray pos = new JSONArray();
        JSONArray neg = new JSONArray();
        TreeModel model = this.tree.getModel();
        Object cur = model.getRoot();
        Stack<Object> toVisit = new Stack<Object>();
        toVisit.push(cur);
        JSONArray temp = new JSONArray();
        JSONArray notLinked = new JSONArray();
        while (!toVisit.empty()) {
            cur = toVisit.pop();
            int childrenCount = model.getChildCount(cur);
            for (int i = 0; i < childrenCount; i++) {
                toVisit.add(model.getChild(cur, i));
            }
            if (model.isLeaf(cur) && cur instanceof RecordNode) {
                RecordNode node = (RecordNode)cur;
                if (node.relevance == 1) {
                    pos.add(node.data);
                }
                else if (node.relevance == -1) {
                    neg.add(node.data);
                }
                JSONArray origRecs = node.data.getJSONArray("Original record(s)");
                for (int i = 0; i < node.clustered.length; i++) {
                    if (node.clustered[i]) {
                        for (int j = 0; j < node.clustered.length; j++) {
                            if (i != j && !node.clustered[j]) {
                                JSONArray pair = new JSONArray();
                                pair.add(origRecs.get(i));
                                pair.add(origRecs.get(j));
                                notLinked.add(pair);
                            }
                        }
                        temp.add(origRecs.get(i));
                    }
                }
            }
        }
        JSONArray linked = new JSONArray();
        for (int i = 0; i < temp.size(); i++) {
            for (int j = i+1; j < temp.size(); j++) {
                JSONArray pair = new JSONArray();
                pair.add(temp.get(i));
                pair.add(temp.get(j));
                linked.add(pair);
            }
        }
        JSONObject ret = new JSONObject();
        ret.put("positive", pos);
        ret.put("negative", neg);
        ret.put("linked", linked);
        ret.put("notlinked", notLinked);
        return ret;
    }

}
