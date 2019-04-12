package org.janelia.it.workstation.browser.gui.options;

import static org.janelia.it.workstation.browser.options.OptionConstants.ANNOTATION_TABLES_HEIGHT_PROPERTY;
import static org.janelia.it.workstation.browser.options.OptionConstants.DISABLE_IMAGE_DRAG_PROPERTY;
import static org.janelia.it.workstation.browser.options.OptionConstants.DUPLICATE_ANNOTATIONS_PROPERTY;
import static org.janelia.it.workstation.browser.options.OptionConstants.SHOW_ANNOTATION_TABLES_PROPERTY;
import static org.janelia.it.workstation.browser.options.OptionConstants.UNLOAD_IMAGES_PROPERTY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImagesPanel;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BrowserOptionsPanel extends javax.swing.JPanel {

    private static final Logger log = LoggerFactory.getLogger(BrowserOptionsPanel.class);

    private final BrowserOptionsPanelController controller;
    private final GroupedKeyValuePanel mainPanel;
    
    private JCheckBox unloadImagesCheckbox;
    private JCheckBox disableImageDrag;
    private JCheckBox allowDuplicateAnnotations;
    private JCheckBox showAnnotationTables;
    private JSlider annotationTableHeight;

    DocumentListener listener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            controller.changed();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            controller.changed();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            controller.changed();
        }
    };
    
    BrowserOptionsPanel(BrowserOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        
        this.mainPanel = new GroupedKeyValuePanel();
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    void load() {

        log.info("Loading browser settings...");
        mainPanel.removeAll();

        mainPanel.addSeparator("Image Browser");

        // Unload Images

        unloadImagesCheckbox = new JCheckBox();
        unloadImagesCheckbox.setText("Unload images which are not visible on the screen");
        unloadImagesCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });

        if (FrameworkImplProvider.getModelProperty(UNLOAD_IMAGES_PROPERTY) == null) {
            FrameworkImplProvider.setModelProperty(UNLOAD_IMAGES_PROPERTY, Boolean.FALSE);
        }
        unloadImagesCheckbox.setSelected((Boolean) FrameworkImplProvider.getModelProperty(UNLOAD_IMAGES_PROPERTY));

        mainPanel.addItem(unloadImagesCheckbox);

        // Disable drag/drop
        
        disableImageDrag = new JCheckBox();
        disableImageDrag.setText("Disable drag and drop in the image viewer");
        disableImageDrag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });

        if (FrameworkImplProvider.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY) == null) {
            FrameworkImplProvider.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, Boolean.FALSE);
        }
        disableImageDrag.setSelected((Boolean) FrameworkImplProvider.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY));

        mainPanel.addItem(disableImageDrag);

        // Allow duplicate annotation keys
        
        allowDuplicateAnnotations = new JCheckBox();
        allowDuplicateAnnotations.setText("Allow duplicate annotations on a single item");
        allowDuplicateAnnotations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });
        if (FrameworkImplProvider.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY) == null) {
            FrameworkImplProvider.setModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, Boolean.FALSE);
        }
        allowDuplicateAnnotations.setSelected((Boolean) FrameworkImplProvider.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY));
     
        mainPanel.addItem(allowDuplicateAnnotations);
        
        // Use Annotation Tables
        
        showAnnotationTables = new JCheckBox();
        showAnnotationTables.setText("Show annotations in a table instead of a tag cloud");
        showAnnotationTables.addActionListener((e) -> {
            controller.changed();
        });
        if (FrameworkImplProvider.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY) == null) {
            FrameworkImplProvider.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, Boolean.FALSE);
        }
        showAnnotationTables.setSelected((Boolean) FrameworkImplProvider.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY));

        mainPanel.addItem(showAnnotationTables);

        // Annotation table height

        annotationTableHeight = new JSlider(ImagesPanel.MIN_TABLE_HEIGHT, ImagesPanel.MAX_TABLE_HEIGHT, ImagesPanel.DEFAULT_TABLE_HEIGHT);
        annotationTableHeight.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        annotationTableHeight.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        annotationTableHeight.addChangeListener((e) -> {
            controller.changed();
        });

        if (FrameworkImplProvider.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY) == null) {
            FrameworkImplProvider.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
        }
        annotationTableHeight.setValue((Integer) FrameworkImplProvider.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY));

        mainPanel.addItem("Annotation table height", annotationTableHeight);
    }

    void store() {

        if (unloadImagesCheckbox.isSelected() != (Boolean) FrameworkImplProvider.getModelProperty(UNLOAD_IMAGES_PROPERTY)) {
            log.info("Saving unload images setting: {}", unloadImagesCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(UNLOAD_IMAGES_PROPERTY, unloadImagesCheckbox.isSelected());
        }

        if (disableImageDrag.isSelected() != (Boolean) FrameworkImplProvider.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY)) {
            log.info("Saving disable image drag: {}", disableImageDrag.isSelected());
            FrameworkImplProvider.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, disableImageDrag.isSelected());
        }

        if (allowDuplicateAnnotations.isSelected() != (Boolean) FrameworkImplProvider.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY)) {
            log.info("Saving allow annotation duplicates: {}", allowDuplicateAnnotations.isSelected());
            FrameworkImplProvider.setModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, allowDuplicateAnnotations.isSelected());
        }
        
        if (showAnnotationTables.isSelected() != (Boolean) FrameworkImplProvider.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY)) {
            log.info("Saving show annotation tables: {}", showAnnotationTables.isSelected());
            FrameworkImplProvider.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, showAnnotationTables.isSelected());
        }

        if (annotationTableHeight.getValue() != (Integer) FrameworkImplProvider.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY)) {
            log.info("Saving annotation table height: {}", annotationTableHeight.getValue());
            FrameworkImplProvider.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, annotationTableHeight.getValue());
        }
    }

    boolean valid() {
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
