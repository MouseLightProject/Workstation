package org.janelia.it.workstation.gui.framework.viewer;


import org.apache.commons.io.IOUtils;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.shared.util.WorkstationFile;

/**
 * This viewer displays text file entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TextFileViewer extends TextViewer {

    public TextFileViewer(ViewerPane viewerPane) {
        super(viewerPane);
    }

    @Override
    public String getText(RootedEntity rootedEntity) throws Exception {
        String filepath = rootedEntity.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        WorkstationFile wfile = new WorkstationFile(filepath);
        return IOUtils.toString(wfile.getStream());
    }
}