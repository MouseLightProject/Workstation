package org.janelia.it.workstation.gui.browser.tools.preferences;

public interface PrefMgrListener {

  /**
   * This is the general message that gets sent to listeners of preferences.
   * A catch-all method that can be used for any change.
   */
  public void preferencesChanged();

}