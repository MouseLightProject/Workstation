package org.janelia.it.workstation.gui.browser.nodes;

import org.janelia.it.workstation.gui.browser.model.DeadReference;
import java.awt.Image;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;

/**
 *
 * @author rokickik
 */
public class DeadReferenceNode extends DomainObjectNode {
    
    public DeadReferenceNode(ChildFactory parentChildFactory, DeadReference deadReference) throws Exception {
        super(parentChildFactory, deadReference);
    }
    
    public DeadReference getDeadReference() {
        return (DeadReference)getBean();
    }
    
    @Override
    public String getPrimaryLabel() {
        return "Missing " + getDeadReference().getType();
    }
        
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("bullet_error.png").getImage();
    }
}
