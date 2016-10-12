package org.janelia.it.workstation.gui.browser.workers;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.workers.WorkerChangedEvent;
import org.janelia.it.workstation.gui.browser.events.workers.WorkerEndedEvent;
import org.janelia.it.workstation.gui.browser.events.workers.WorkerStartedEvent;
import org.janelia.it.workstation.gui.browser.components.ProgressTopComponent;
import org.janelia.it.workstation.gui.browser.util.ConcurrentUtils;

/**
 * A worker thread which can be monitored in the background.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class BackgroundWorker extends SimpleWorker {

    private String status;
    private Callable<Void> success;

    public BackgroundWorker() {
    }
    
    public BackgroundWorker(Callable<Void> success) {
        this.success = success;
    }

    @Override
    protected void hadSuccess() {
    }
    
    @Override
    protected void hadError(Throwable error) {
    }
    
    @Override
    protected void done() {
        super.done();
        Events.getInstance().postOnEventBus(new WorkerEndedEvent(this));
    }

    public abstract String getName();

    public void setStatus(String status) {
        this.status = status;
        Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
    }

    public void setFinalStatus(String status) {
        setStatus(status);
        setProgress(100);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
    }
    
    public String getStatus() {
        return status;
    }
    
    public Callable<Void> getSuccessCallback() {
        return success;
    }

    public void setSuccessCallback(Callable<Void> success) {
        this.success = success;
    }

    public void runSuccessCallback() {
        try {
            ConcurrentUtils.invoke(getSuccessCallback());
        }
        catch (Exception e) {
            hadError(e);
        }
    }
    
    /**
     * Same as execute(), except throws events on the ModelMgr's EventBus.
     */
    public void executeWithEvents() {
        execute();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressTopComponent.ensureActive();
            }
        });
        Events.getInstance().postOnEventBus(new WorkerStartedEvent(this));
    }
}
