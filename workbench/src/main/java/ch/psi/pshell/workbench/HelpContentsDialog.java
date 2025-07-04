package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Sys;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 *
 */
public class HelpContentsDialog extends StandardDialog {

    final DefaultMutableTreeNode root;
    boolean initialMessage;


    public HelpContentsDialog(java.awt.Window parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Help Contents");
        this.editor.setContentType("text/html");

        root = new DefaultMutableTreeNode("Help Contents");
        DefaultMutableTreeNode nodeBuiltinFunction = new DefaultMutableTreeNode("Built-in Functions");
        root.add(nodeBuiltinFunction);
        DefaultMutableTreeNode nodeTutorial = new DefaultMutableTreeNode("Tutorial");
        root.add(nodeTutorial);
        tree.setModel(new DefaultTreeModel(root));

        //Initialize built-in function names, waiting interpreter to start
        new Thread(() -> {
            String[] builtinFunctions = null;
            while (builtinFunctions == null) {
                try {
                    if (Context.getInterpreter().getState().isInitialized()){
                        builtinFunctions = Context.getInterpreter().getBuiltinFunctionsNames();
                    }
                } catch (Exception ex) {
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    return;
                }
                if (!HelpContentsDialog.this.isDisplayable()) {
                    return;
                }
            }
            for (String function : builtinFunctions) {
                nodeBuiltinFunction.add(new DefaultMutableTreeNode(function));
            }
            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(nodeBuiltinFunction);

        }).start();

        setText("Loading contents...", "");
        initialMessage = true;

        //Initialize static contents in thread because may be too slow
        new Thread(() -> {
            String[] contents = Setup.getHelpItems("");
            contents = Arr.removeEquals(contents, nodeBuiltinFunction.toString());
            for (String content : contents) {
                if (!content.startsWith("Tutorial_")) {
                    DefaultMutableTreeNode node = addNode(root, content);
                }
            }
            //This is to move the SimulatedDevices item to the top of the list
            if (nodeTutorial.getChildCount() > 1) {
                DefaultMutableTreeNode simulatedDevicesNode = (DefaultMutableTreeNode) nodeTutorial.getChildAt(nodeTutorial.getChildCount() - 1);
                if (simulatedDevicesNode.isLeaf()) {
                    nodeTutorial.insert(simulatedDevicesNode, 0);
                }
            }

            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(root);
            if (initialMessage) {
                SwingUtilities.invokeLater(() -> {
                    clear();
                });
            }
        }).start();

        tree.addTreeSelectionListener((TreeSelectionEvent event) -> {
            String header = "";
            String text = "";
            try {
                TreePath tp = event.getPath();
                if ((tp.getPathCount() == 3) && (tp.getPath()[1] == nodeBuiltinFunction)) {
                    header = tp.getPath()[2].toString();
                    text = Context.getInterpreter().getBuiltinFunctionDoc(header);
                } else if (tp.getPathCount() > 1) {
                    String resource = String.join("/", Arr.remove(Convert.toStringArray(tp.getPath()), 0));
                    if (resource.startsWith("Tutorial/")) {
                        resource = resource.replaceFirst("Tutorial", getTutorialRoot());
                    }
                    Setup.HelpContent content = Setup.getHelpContent(resource);
                    if (content != null) {
                        header = tp.getPath()[tp.getPath().length - 1].toString();
                        setText(header, content.content, content.type);
                        return;
                    }
                }
                setText(header, text);
            } catch (Exception ex) {
                setException(ex);
            }
        });

        JPopupMenu popupMenuScript = new JPopupMenu();
        JMenuItem menuRun = new JMenuItem("Run");
        JMenuItem menuDebug = new JMenuItem("Debug in Editor");
        menuRun.addActionListener((ActionEvent e) -> {
            TreePath tp = tree.getSelectionPath();
            try {
                Context.getInterpreter().assertReady();
                final Path path = Paths.get(Sys.getTempFolder(), "Tutorial_" + tp.getPath()[tp.getPath().length - 1].toString() + "." + getScriptType());
                Files.write(path, editor.getText().getBytes());
                Context.getInterpreter().evalFileAsync(path.toString()).handle((ok, ex) -> {
                    path.toFile().delete();
                    return ok;
                });

            } catch (Exception ex) {
                showException(ex);
            }
        });

        menuDebug.addActionListener((ActionEvent e) -> {
            TreePath tp = tree.getSelectionPath();
            try {
                Context.getView().newScript(editor.getText());
            } catch (Exception ex) {
                showException(ex);
            }
        });

        popupMenuScript.add(menuRun);
        popupMenuScript.add(menuDebug);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        TreePath clicked = tree.getPathForLocation(e.getX(), e.getY());
                        TreePath selected = tree.getSelectionPath();
                        if ((Context.getState().isInitialized()) && (selected != null) && (selected.equals(clicked))) {
                            if ((clicked.getPath()[1] == nodeTutorial) && (clicked.getPathCount() >= 3) && ((TreeNode) clicked.getLastPathComponent()).isLeaf()) {
                                popupMenuScript.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

        });
    }
    
    public void clear() {
        tree.clearSelection();
        setText("", "");
    }

    public String getScriptType() {
        return Context.getScriptType().toString();
    }

    public String getTutorialRoot() {
        //return "Tutorial_" + ScriptType.getDefault();
        return "Tutorial_" + getScriptType();
    }

    DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, String content) {
        if ((content==null) || (content.trim().isEmpty())){
            return null;
        }
        DefaultMutableTreeNode node = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            TreeNode child = parent.getChildAt(i);
            if (child.toString().equals(content)) {
                node = (DefaultMutableTreeNode) child;
                break;
            }
        }
        if (node == null) {
            node = new DefaultMutableTreeNode(content);
        }
        //If inside the if block above, then won't enforce the order in the help folder
        parent.add(node);
        String[] par = new String[node.getPath().length - 1];
        for (int i = 1; i < node.getPath().length; i++) {
            par[i - 1] = node.getPath()[i].toString();
        }
        if (par[0].equals("Tutorial")) {
            par[0] = getTutorialRoot();
        }
        String path = String.join("/", par);
        for (String child : Setup.getHelpItems(path)) {
            addNode(node, child);
        }
        return node;
    }

    public void initialize() {
        SwingUtils.collapseAll(tree);
        tree.expandPath(new TreePath(root));
        clear();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        jScrollPane2 = new javax.swing.JScrollPane();
        editor = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jSplitPane1.setDividerLocation(200);

        jScrollPane1.setViewportView(tree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        editor.setEditable(false);
        jScrollPane2.setViewportView(editor);

        jSplitPane1.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        Context.getApp().init(args);
        java.awt.EventQueue.invokeLater(() -> {
            HelpContentsDialog dialog = new HelpContentsDialog(new javax.swing.JFrame(), true);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setSize(800, 600);
            SwingUtils.centerComponent(null, dialog);
            dialog.setVisible(true);
        });
    }

    //boolean isHtml(String str){
    //    return str.matches("[\\S\\s]*\\<html[\\S\\s]*\\>[\\S\\s]*\\<\\/html[\\S\\s]*\\>[\\S\\s]*");
    //}
    void setException(Exception ex) {
        setText("Error", ex.toString());
    }

    void setText(String header, String content) {
        setText(header, content, Setup.HelpContentType.txt);
    }

    void setText(String header, String content, Setup.HelpContentType type) {
        initialMessage = false;
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case html:
                editor.setContentType("text/html");
                editor.setText(content);
                break;
            case md:  
                List<Extension> extensions = Arrays.asList(TablesExtension.create());
                Parser parser = Parser.builder().extensions(extensions).build();
                Node document = parser.parse(content);      
                HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
                String htmlContent = renderer.render(document); 
                editor.setContentType("text/html");
                editor.setText(htmlContent);                
                break;
            case py:
            case js:
            case groovy:
                editor.setContentType("text/plain");
                editor.setText(content);
                editor.setSize(2000, editor.getHeight());
                break;
            case txt:
                header = header.replace("\n", "<br>");
                content = content.replace("\n", "<br>");
                header = header.replace("    ", "&emsp ");      //Consider 4 spaces a tab
                content = content.replace("    ", "&emsp ");
                sb.append("<!DOCTYPE html>\n");
                sb.append("<html>\n");
                sb.append("<head>\n");
                sb.append("</head>\n");
                sb.append("<body>\n");
                if (header != null) {
                    sb.append("<h1>").append(header).append("</h1>");
                }
                sb.append("<br>\n");
                sb.append(content).append("\n");
                sb.append("<br>\n");
                sb.append("</body>\n");
                sb.append("</html>\n");
                editor.setContentType("text/html");
                editor.setText(sb.toString());
                break;
        }
        editor.setCaretPosition(0);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane editor;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables
}
