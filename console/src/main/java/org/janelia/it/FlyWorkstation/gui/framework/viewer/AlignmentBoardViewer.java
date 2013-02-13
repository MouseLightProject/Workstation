package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.ABLoadWorker;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.ABTransferHandler;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorWheelColorMapping;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
public class AlignmentBoardViewer extends Viewer {

    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private ABLoadWorker loadWorker;
    private ModelMgrObserver modelMgrObserver;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);
        setLayout(new BorderLayout());
        setTransferHandler( new ABTransferHandler( alignmentBoard ) );

    }

    /** This is used by load code to establish how to colorize the things that are rendered. */
    public ColorMappingI getColorMapper() {
        return new ColorWheelColorMapping();
    }

    @Override
    public void clear() {
        clearObserver();
        //refresh();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        //this.updateUI();
        revalidate();
        repaint();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        alignmentBoard = rootedEntity.getEntity();
        if (loadWorker != null) {
            loadWorker.disregard();
            loadWorker.cancel( true );
        }
        refresh();

        // Listen for further changes, so can refresh again later.
        establishObserver();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {
        loadEntity(rootedEntity);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    @Override
    public List<RootedEntity> getRootedEntities() {
        return Arrays.asList( albRootedEntity );
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return albRootedEntity;
    }

    @Override
    public Entity getEntityById(String id) {
        return alignmentBoard;
    }

    @Override
    public void close() {
        clearObserver();
        if (loadWorker != null) {
            loadWorker.disregard();
        }
        alignmentBoard = null;
        albRootedEntity = null;
        removeAll();
        mip3d = null;
    }

    @Override
    public void refresh() {
        logger.info("Refresh called.");

        if (alignmentBoard != null) {
            showLoadingIndicator();

            if ( mip3d == null ) {
                mip3d = new Mip3d();
                if ( loadWorker == null ) {
                    loadWorker = new ABLoadWorker( this, alignmentBoard, mip3d );
                }
            }

            mip3d.refresh();

            mip3d.setClearOnLoad( true );

            // Here, should load volumes, for all the different items given.

            // Activate the layers panel for controlling visibility. This code might have to be moved elsewhere.
            LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
            layersPanel.showEntities(alignmentBoard.getOrderedChildren());
            SessionMgr.getBrowser().selectRightPanel(layersPanel);

            loadWorker.execute();

        }

    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void establishObserver() {
        modelMgrObserver = new ModelMgrListener( this, alignmentBoard );
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    private void clearObserver() {
        if ( modelMgrObserver != null ) {
            ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        }
    }

    //------------------------------Inner Classes
    /** Listens for changes to the child-set of the heard-entity. */
    public static class ModelMgrListener extends ModelMgrAdapter {
        private Entity heardEntity;
        private AlignmentBoardViewer viewer;
        ModelMgrListener( AlignmentBoardViewer viewer, Entity e ) {
            heardEntity = e;
            this.viewer = viewer;
        }

        @Override
        public void entityChildrenChanged(long entityId) {
            if (heardEntity.getId() == entityId) {
                viewer.refresh();
            }
        }
    }

}
