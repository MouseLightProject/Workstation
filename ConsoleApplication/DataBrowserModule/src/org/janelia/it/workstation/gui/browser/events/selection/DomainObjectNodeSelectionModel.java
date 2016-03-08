package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A selection model implementation which tracks the selection of domain object nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeSelectionModel extends SelectionModel<DomainObjectNode,Reference> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectNodeSelectionModel.class);
    
    @Override
    protected void selectionChanged(DomainObjectNode domainObjectNode, Reference id, boolean select, boolean clearAll, boolean isUserDriven) {
        log.debug((select?"select":"deselect")+" {}, clearAll={}",id,clearAll);
        if (domainObjectNode instanceof ObjectSetNode) {
            ObjectSetNode objectSetNode = (ObjectSetNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), select, objectSetNode, isUserDriven));
        }
        else if (domainObjectNode instanceof FilterNode) {
            FilterNode filterNode = (FilterNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new FilterSelectionEvent(getSource(), select, filterNode, isUserDriven));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjectNode, select, clearAll, isUserDriven));
        }
    }
    
    @Override
    public Reference getId(DomainObjectNode domainObjectNode) {
        DomainObject domainObject = domainObjectNode.getDomainObject();
        return Reference.createFor(domainObject);
    }
}
