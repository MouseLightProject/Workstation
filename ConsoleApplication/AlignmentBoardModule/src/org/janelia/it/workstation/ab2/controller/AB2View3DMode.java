package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.workstation.ab2.event.AB2MouseBeginDragEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseWheelEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseReleasedEvent;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2View3DMode extends AB2ControllerMode {

    Logger logger= LoggerFactory.getLogger(AB2View3DMode.class);

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    public AB2View3DMode(AB2Controller controller) {
        super(controller);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void processEvent(AB2Event event) {
        AB2UserContext userContext=AB2Controller.getController().getUserContext();
        if (event instanceof AB2MouseReleasedEvent) {
            if (userContext.isMouseIsDragging()) {
                AB2Renderer dragRenderer=userContext.getCurrentDragRenderer();
                if (dragRenderer!=null) {
                    dragRenderer.processEvent(event);
                }
                userContext.clear();
            }
        } else if (event instanceof AB2MouseDraggedEvent) {
            MouseEvent mouseEvent=((AB2MouseDraggedEvent) event).getMouseEvent();
            Point p1 = mouseEvent.getPoint();

            if (!userContext.isMouseIsDragging()) {
                userContext.setMouseIsDragging(true);
                userContext.getPositionHistory().add(p1);
                AB2Renderer dragRenderer=getRendererAtPosition(p1);
                AB2MouseBeginDragEvent beginDragEvent=new AB2MouseBeginDragEvent(((AB2MouseDraggedEvent) event).getMouseEvent());
                if (dragRenderer!=null) {
                    dragRenderer.processEvent(beginDragEvent);
                }
                userContext.setCurrentDragRenderer(dragRenderer);
                return;
            }

            // Assume we have an established drag state
            userContext.getPositionHistory().add(p1);
            AB2Renderer dragRenderer=userContext.getCurrentDragRenderer();
            dragRenderer.processEvent(event);
            controller.repaint();
        } else if (event instanceof AB2MouseWheelEvent) {
            AB2Renderer currentRenderer=getRendererAtPosition(((AB2MouseWheelEvent) event).getMouseWheelEvent().getPoint());
            currentRenderer.processEvent(event);
            controller.repaint();
        }
    }

}
