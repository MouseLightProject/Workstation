package org.janelia.it.workstation.browser.api;

import javax.swing.JOptionPane;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.openide.LifecycleManager;

/**
 * Helps mock out the Workstation well enough to carry out tests. Logs in, etc.
 * Created by fosterl on 1/27/14.
 */
public class WorkstationEnvironment {
    public void invoke() {
        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();

        // Assuming that the user has entered the login/password information, now validate
        String username = (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_NAME);
        String password = (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_PASSWORD);
        String runAsUser = (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.RUN_AS_USER);
        String email = (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_EMAIL);

        if (username==null || email==null) {
            Object[] options = {"Enter Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                throw new IllegalStateException("Please enter your login information");
            }
            else {
                LifecycleManager.getDefault().exit(0);
            }
        }

        AccessManager.getAccessManager().loginSubject(username, password);
        AccessManager.getAccessManager().setRunAsUser(runAsUser);
    }
}
