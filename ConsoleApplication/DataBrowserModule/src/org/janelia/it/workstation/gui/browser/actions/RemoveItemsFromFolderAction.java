package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Remove items from a tree node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveItemsFromFolderAction extends AbstractAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveItemsFromFolderAction.class);

    private final TreeNode treeNode;
    private final Collection<DomainObject> domainObjects;

    public RemoveItemsFromFolderAction(TreeNode treeNode, Collection<DomainObject> domainObjects) {
        super(getName(treeNode, domainObjects));
        this.treeNode = treeNode;
        this.domainObjects = domainObjects;
    }

    public static final String getName(TreeNode treeNode, Collection<DomainObject> domainObjects) {
        return domainObjects.size() > 1 ? "Remove " + domainObjects.size() + " Items From Folder '"+treeNode.getName()+"'" : "Remove This Item From Folder '"+treeNode.getName()+"'";
    }

    @Override
    public void actionPerformed(ActionEvent event) {
    	
        ActivityLogHelper.logUserAction("RemoveItemsFromFolderAction.doAction", treeNode);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        final Multimap<TreeNode,DomainObject> removeFromFolders = ArrayListMultimap.create();
        final List<DomainObject> listToDelete = new ArrayList<>();

        for(DomainObject domainObject : domainObjects) {

            // first check to make sure this Object only has one ancestor references; if it does pop up a dialog before removal
            List<Reference> refList = model.getContainerReferences(domainObject);
            if (refList==null || refList.size()<=1) {
                listToDelete.add(domainObject);
            }
            else {
                log.info("{} has multiple references: {}", domainObject, refList);
            }
            removeFromFolders.put(treeNode,domainObject);
        }

        if (!listToDelete.isEmpty()) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(),
                    "There are " + listToDelete.size() + " items in your remove list that will be deleted permanently.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                // Remove references
                for (TreeNode treeNode : removeFromFolders.keySet()) {
                    for (DomainObject domainObject : removeFromFolders.get(treeNode)) {
                        log.info("Removing {} from {}", domainObject, treeNode);
                        model.removeChild(treeNode, domainObject);
                    }
                }

                // Remove any actual objects that are no longer references
                if (!listToDelete.isEmpty()) {
                    log.info("Deleting entirely: {}", listToDelete);
                    model.remove(listToDelete);
                }
            }

            @Override
            protected void hadSuccess() {
                // Handled by the event system
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Removing items", "", 0, 100));
        worker.execute();
    }
}
