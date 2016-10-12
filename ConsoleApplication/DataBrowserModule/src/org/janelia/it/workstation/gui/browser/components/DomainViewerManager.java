package org.janelia.it.workstation.gui.browser.components;

import java.awt.Component;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.gui.browser.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Manages the life cycle of domain viewers based on user generated selected events. This manager
 * either reuses existing viewers, or creates them as needed and docks them in the appropriate place.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainViewerManager implements ViewerManager<DomainViewerTopComponent>  {

    private final static Logger log = LoggerFactory.getLogger(DomainViewerManager.class);
    
    public static DomainViewerManager instance;
    
    private DomainViewerManager() {
    }
    
    public static DomainViewerManager getInstance() {
        if (instance==null) {
            instance = new DomainViewerManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active instance of this top component */
    
    private DomainViewerTopComponent activeInstance;
    @Override
    public void activate(DomainViewerTopComponent instance) {
        activeInstance = instance;
    }
    @Override
    public boolean isActive(DomainViewerTopComponent instance) {
        return activeInstance == instance;
    }
    @Override
    public DomainViewerTopComponent getActiveViewer() {
        return activeInstance;
    }

    @Override
    public String getViewerName() {
        return "DomainViewerTopComponent";
    }
    
    @Override
    public Class<DomainViewerTopComponent> getViewerClass() {
        return DomainViewerTopComponent.class;
    }

    @Subscribe
    public void domainObjectsSelected(DomainObjectSelectionEvent event) {

        // We only care about single selections
        DomainObject domainObject = event.getObjectIfSingle();
        if (domainObject==null) {
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }

        // We only care about events generated by a domain list viewer
        if (!Utils.hasAncestorWithType((Component)event.getSource(),DomainListViewTopComponent.class)) {
            log.trace("Event source is not a list view: {}",event);
            return;
        }

        log.info("domainObjectSelected({})",Reference.createFor(domainObject));
        
        DomainViewerTopComponent viewer = DomainViewerManager.getInstance().getActiveViewer();
        if (viewer!=null) {
            // If we are reacting to a selection event in another viewer, then this load is not user driven.
            viewer.loadDomainObject(domainObject, false);
        }
    }

    public static DomainObject getObjectToLoad(DomainObject domainObject) throws Exception {
        if (domainObject instanceof NeuronFragment) {
            NeuronFragment fragment = (NeuronFragment) domainObject;
            return DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());
        }
        else if (domainObject instanceof LSMImage) {
            LSMImage lsmImage = (LSMImage) domainObject;
            Reference sampleRef = lsmImage.getSample();
            if (sampleRef!=null) {
                return DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);
            }
            else {
                return null;
            }
        }
        return domainObject;
    }

}
