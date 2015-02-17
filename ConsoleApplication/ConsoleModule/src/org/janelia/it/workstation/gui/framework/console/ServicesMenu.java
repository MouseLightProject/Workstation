package org.janelia.it.workstation.gui.framework.console;

import org.janelia.it.workstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.workstation.gui.dialogs.ScreenEvaluationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ServicesMenu extends JMenu {

    private Browser currentBrowser;

    public ServicesMenu(final Browser browser) {
        super("Services");
        currentBrowser = browser;

        final ScreenEvaluationDialog screenEvaluationDialog = browser.getScreenEvaluationDialog();
        if (screenEvaluationDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Screen Evaluation");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	screenEvaluationDialog.showDialog();
                }
            });
            add(menuItem);
        }
        
        final DataSetListDialog dataSetListDialog = browser.getDataSetListDialog();
        if (dataSetListDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("Data Sets");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	dataSetListDialog.showDialog();
                }
            });
            add(menuItem);
        }
    }
}
