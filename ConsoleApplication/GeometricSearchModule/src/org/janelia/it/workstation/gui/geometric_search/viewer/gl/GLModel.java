package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.geometric_search.viewer.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.DenseVolumeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.MeshActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.SparseVolumeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorRemovedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorsClearAllEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by murphys on 8/21/2015.
 */
public class GLModel implements VoxelViewerEventListener {

    private final Logger logger = LoggerFactory.getLogger(GLModel.class);

    public static final String DISPOSE_AND_CLEAR_ALL_ACTORS_MSG = "DISPOSE_AND_CLEAR_ALL_ACTORS_MSG";

    private VoxelViewerModel model;
    VoxelViewerProperties properties;
    VoxelViewerGLPanel viewer;

    public static class MVPPrecompute {
        int timestamp;
        public Matrix4 modelView;
        public Matrix4 modelViewProjection;
        public Matrix4 normalMatrix;
    }

    public static Map<Integer, MVPPrecompute> mvpPrecomputeMap = new HashMap<>();

    public int maxActorIndex=0;

    protected GL4ShaderActionSequence denseVolumeShaderActionSequence = new GL4ShaderActionSequence("Dense Volumes");
    protected GL4ShaderActionSequence meshShaderActionSequence = new GL4ShaderActionSequence("Meshes");

    protected Deque<GL4SimpleActor> initQueue = new ArrayDeque<>();
    protected Deque<Integer> disposeQueue = new ArrayDeque<>();
    protected Deque<String> messageQueue = new ArrayDeque<>();

    public boolean hasPendingEvents() {
        if (initQueue.size()>0 || disposeQueue.size()>0 || messageQueue.size()>0) {
            return true;
        }
        return false;
    }

    public VoxelViewerModel getModel() {
        return model;
    }

    public void setModel(VoxelViewerModel model) {
        this.model = model;
    }

    public VoxelViewerProperties getProperties() {
        return properties;
    }

    public void setProperties(VoxelViewerProperties properties) {
        this.properties = properties;
    }

    public VoxelViewerGLPanel getViewer() {
        return viewer;
    }

    public void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer = viewer;
    }

    public int getNextActorIndex() {
        synchronized (this) {
            int index=maxActorIndex;
            maxActorIndex++;
            return index;
        }
    }

    public int getTransparencyQuarterDepth() {
        int transparencyQuarterDepth=0;
        try {
            transparencyQuarterDepth=properties.getInteger(VoxelViewerProperties.GL_TRANSPARENCY_QUARTERDEPTH_INT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return transparencyQuarterDepth;
    }

    public void postViewerIntegrationSetup() {
        setupDenseVolumeShader();
        setupMeshShader();
    }

    private void setupDenseVolumeShader() {
        final ArrayCubeShader arrayCubeShader = new ArrayCubeShader(properties);
        final Integer transparencyQuarterDepth=getTransparencyQuarterDepth();
        arrayCubeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                arrayCubeShader.setWidth(gl, viewer.getWidth());
                arrayCubeShader.setHeight(gl, viewer.getHeight());
                arrayCubeShader.setDepth(gl, transparencyQuarterDepth);
            }
        });
        denseVolumeShaderActionSequence.setShader(arrayCubeShader);
        denseVolumeShaderActionSequence.setApplyMemoryBarrier(true);
    }

    private void setupMeshShader() {
        final ArrayMeshShader arrayMeshShader = new ArrayMeshShader(properties);
        final Integer transparencyQuarterDepth=getTransparencyQuarterDepth();
        arrayMeshShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                arrayMeshShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                arrayMeshShader.setProjection(gl, projMatrix);

                arrayMeshShader.setWidth(gl, viewer.getWidth());
                arrayMeshShader.setHeight(gl, viewer.getHeight());
                arrayMeshShader.setDepth(gl, transparencyQuarterDepth);

            }
        });

        meshShaderActionSequence.setShader(arrayMeshShader);
        meshShaderActionSequence.setApplyMemoryBarrier(true);
    }

    public Deque<GL4SimpleActor> getInitQueue() {
        return initQueue;
    }

    public Deque<Integer> getDisposeQueue() {
        return disposeQueue;
    }

    public Deque<String> getMessageQueue() {
        return messageQueue;
    }

    public GL4ShaderActionSequence getDenseVolumeShaderActionSequence() {
        return denseVolumeShaderActionSequence;
    }

    public GL4ShaderActionSequence getMeshShaderActionSequence() {
        return meshShaderActionSequence;
    }

    public int addActorToInitQueue(GL4SimpleActor newActor) {
        synchronized(this) {
            initQueue.add(newActor);

        }
        return newActor.getActorId();
    }

    public void addActor(GL4SimpleActor actor) {
        synchronized (this) {
            if (actor instanceof ArrayCubeGLActor) {
                denseVolumeShaderActionSequence.getActorSequence().add(actor);
            } else if (actor instanceof ArrayMeshGLActor) {
                meshShaderActionSequence.getActorSequence().add(actor);
            }
        }
    }

    public void removeActorToDisposeQueue(int actorId) {
        synchronized (this) {
            disposeQueue.add(new Integer(actorId));
        }
    }

    public void removeActor(int actorId, GL4 gl) {
        synchronized (this) {
            GL4SimpleActor toBeRemoved=null;
            for (GL4SimpleActor actor : denseVolumeShaderActionSequence.getActorSequence()) {
                if (actor.getActorId()==actorId) {
                    actor.dispose(gl);
                    toBeRemoved=actor;
                    break;
                }
            }
            if (toBeRemoved!=null) {
                denseVolumeShaderActionSequence.getActorSequence().remove(toBeRemoved);
            } else {
                for (GL4SimpleActor actor : meshShaderActionSequence.getActorSequence()) {
                    if (actor.getActorId()==actorId) {
                        actor.dispose(gl);
                        toBeRemoved=actor;
                        break;
                    }
                }
                if (toBeRemoved!=null) {
                    meshShaderActionSequence.getActorSequence().remove(toBeRemoved);
                }
            }
        }
    }

    public void setDisposeAndClearAllActorsMsg() {
        synchronized (this) {
            messageQueue.add(DISPOSE_AND_CLEAR_ALL_ACTORS_MSG);
        }
    }

    public void disposeAndClearAllActors(GL4 gl) {
        synchronized (this) {
            denseVolumeShaderActionSequence.disposeAndClearActorsOnly(gl);
            meshShaderActionSequence.disposeAndClearActorsOnly(gl);
        }
    }

    public void disposeAndClearAll(GL4 gl) {
        synchronized (this) {
            initQueue.clear();
            denseVolumeShaderActionSequence.dispose(gl);
            meshShaderActionSequence.dispose(gl);
        }
    }

    public void initAll(ArrayTransparencyContext atc, GL4 gl) {
        ArrayCubeShader acs = (ArrayCubeShader)denseVolumeShaderActionSequence.getShader();
        ArrayMeshShader ams = (ArrayMeshShader)meshShaderActionSequence.getShader();
        acs.setTransparencyContext(atc);
        ams.setTransparencyContext(atc);
        try {
            denseVolumeShaderActionSequence.init(gl);
            meshShaderActionSequence.init(gl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void processEvent(VoxelViewerEvent event) {

        try {

            if (event instanceof ActorAddedEvent) {

                ActorAddedEvent actorAddedEvent = (ActorAddedEvent) event;
                final Actor actor = actorAddedEvent.getActor();
                GL4SimpleActor gl4SimpleActor = actor.getGlActor();
                if (gl4SimpleActor==null) {
                    gl4SimpleActor=actor.createAndSetGLActor();
                }
                final GL4SimpleActor gl4SimpleActor_f=gl4SimpleActor;

                if (gl4SimpleActor_f==null) {
                    throw new Exception("gl4SimpleActor is unexpectedly null from actor.createAndSetGLActor()");
                }

                if (gl4SimpleActor_f instanceof ArrayCubeGLActor) {

                    logger.info("setting up actor name="+actor.getName());

                    final DenseVolumeActor denseVolumeActor=(DenseVolumeActor)actor;

                    boolean sparseFlag=false;

                    if (actor instanceof SparseVolumeActor) {
                        sparseFlag=true;
                    }

                    final ArrayCubeGLActor arrayCubeGLActor = (ArrayCubeGLActor) gl4SimpleActor_f;
                    final ArrayCubeShader cubeShader = (ArrayCubeShader) denseVolumeShaderActionSequence.getShader();

                    Matrix4 gal4Rotation = new Matrix4();

                    // Empirically derived - for GAL4 samples
                    // todo - move to ScreenDataset and MCFODataset

                    if (sparseFlag) {
                        gal4Rotation.setTranspose(
                                1.0f, 0.0f, 0.0f, -0.5f,
                                0.0f,  1.0f, 0.0f, -0.25f,
                                0.0f, 0.0f, -1.0f, 0.625f,
                                0.0f, 0.0f, 0.0f, 1.0f);
                    } else {
                        gal4Rotation.setTranspose(
                                -1.0f, 0.0f, 0.0f, 0.5f,
                                0.0f, -1.0f, 0.0f, 0.25f,
                                0.0f, 0.0f, -1.0f, 0.625f,
                                0.0f, 0.0f, 0.0f, 1.0f);
                    }

                    arrayCubeGLActor.setModel(gal4Rotation);

                    logger.info("calling arrayCubeGLActor.setUpdateCallback()");

                    arrayCubeGLActor.setUpdateCallback(new GLDisplayUpdateCallback() {
                        @Override
                        public void update(GL4 gl) {

                            Matrix4 view = viewer.getRenderer().getViewMatrix();
                            Matrix4 proj = viewer.getRenderer().getProjectionMatrix();
                            Matrix4 model = arrayCubeGLActor.getModel();

                            Matrix4 viewCopy = new Matrix4(view);
                            Matrix4 projCopy = new Matrix4(proj);
                            Matrix4 modelCopy = new Matrix4(model);

                            Matrix4 vp = viewCopy.multiply(projCopy);
                            Matrix4 mvp = modelCopy.multiply(vp);

                            cubeShader.setMVP(gl, mvp);
                            cubeShader.setProjection(gl, projCopy);
                            cubeShader.setWidth(gl, viewer.getWidth());
                            cubeShader.setHeight(gl, viewer.getHeight());

                            float voxelUnitSize = arrayCubeGLActor.getVoxelUnitSize();
                            cubeShader.setVoxelUnitSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));
                            Vector4 actorColor = denseVolumeActor.getColor();
                            float transparency = denseVolumeActor.getTransparency();
                            float[] data = actorColor.toArray();
                            Vector4 actualColor = new Vector4(data[0], data[1], data[2], transparency);
                            float[] actualData = actualColor.toArray();
                            for (int i = 0; i < 4; i++) {
                                if (actualData[i] < 0f) {
                                    actualData[i] = 0.0f;
                                } else if (actualData[i] > 1.0f) {
                                    actualData[i] = 1.0f;
                                }
                            }
                            cubeShader.setDrawColor(gl, actualColor);
                            float brightness = denseVolumeActor.getBrightness();
                            cubeShader.setBrightness(gl, brightness);
                        }
                    });

                    logger.info("Done setting up callback for actor=" + actor.getName());

                } else if (gl4SimpleActor_f instanceof ArrayMeshGLActor) {

                    final MeshActor meshActor=(MeshActor)actor;
                    final ArrayMeshGLActor arrayMeshGLActor = (ArrayMeshGLActor) gl4SimpleActor_f;
                    final ArrayMeshShader meshShader = (ArrayMeshShader) meshShaderActionSequence.getShader();

                    arrayMeshGLActor.setUpdateCallback(new GLDisplayUpdateCallback() {
                        @Override
                        public void update(GL4 gl) {

                            Matrix4 MV;
                            Matrix4 NM;

                            int mvpGroup=arrayMeshGLActor.getMvpPrecomputeGroup();

                            if (mvpGroup>0) {
                                MVPPrecompute mvpPrecompute=mvpPrecomputeMap.get(mvpGroup);
                                if (mvpPrecompute==null)  {
                                    mvpPrecompute=new MVPPrecompute();
                                    mvpPrecomputeMap.put(mvpGroup, mvpPrecompute);
                                }
                                if (mvpPrecompute.timestamp < VoxelViewerRenderer.displayTimestep) {

                                    Matrix4 view = viewer.getRenderer().getViewMatrix();
                                    Matrix4 model = arrayMeshGLActor.getModel();
                                    Matrix4 viewCopy = new Matrix4(view);
                                    Matrix4 modelCopy = new Matrix4(model);

                                    MV = viewCopy.multiply(modelCopy);
                                    NM = MV.inverse().transpose();

                                    mvpPrecompute.modelView=MV;
                                    mvpPrecompute.normalMatrix=NM;

                                    mvpPrecompute.timestamp=VoxelViewerRenderer.displayTimestep;
                                } else {

                                    MV = mvpPrecompute.modelView;
                                    NM = mvpPrecompute.normalMatrix;
                                }
                            } else {

                                Matrix4 view = viewer.getRenderer().getViewMatrix();
                                Matrix4 model = arrayMeshGLActor.getModel();
                                Matrix4 viewCopy = new Matrix4(view);
                                Matrix4 modelCopy = new Matrix4(model);

                                MV = viewCopy.multiply(modelCopy);
                                NM = MV.inverse().transpose();

                            }

                            Matrix4 proj = viewer.getRenderer().getProjectionMatrix();
                            Matrix4 projCopy = new Matrix4(proj);

                            meshShader.setProjection(gl, projCopy);
                            meshShader.setMV(gl, MV);
                            meshShader.setNM(gl, NM);

                            meshShader.setWidth(gl, viewer.getWidth());
                            meshShader.setHeight(gl, viewer.getHeight());

                            Vector4 actorColor = meshActor.getColor();

                            float edgeFalloff = meshActor.getEdgeFalloff();
                            float intensity = meshActor.getIntensity();
                            float ambience = meshActor.getAmbience();

                            float[] data = actorColor.toArray();
                            Vector4 actualColor = new Vector4(data[0], data[1], data[2], 1.0f);
                            float[] actualData = actualColor.toArray();
                            for (int i = 0; i < 4; i++) {
                                if (actualData[i] < 0f) {
                                    actualData[i] = 0.0f;
                                } else if (actualData[i] > 1.0f) {
                                    actualData[i] = 1.0f;
                                }
                            }
                            meshShader.setDrawColor(gl, actualColor);
                            meshShader.setEdgefalloff(gl, edgeFalloff);
                            meshShader.setIntensity(gl, intensity);
                            meshShader.setAmbience(gl, ambience);

                        }
                    });
                }

                initQueue.add(gl4SimpleActor_f);

            } else if (event instanceof ActorRemovedEvent) {
                ActorRemovedEvent removedEvent = (ActorRemovedEvent) event;
                Actor actorToBeRemoved = removedEvent.getActor();
                GL4SimpleActor gl4SimpleActor = actorToBeRemoved.getGlActor();
                if (gl4SimpleActor != null) {
                    removeActorToDisposeQueue(gl4SimpleActor.getActorId());
                }
            } else if (event instanceof ActorsClearAllEvent) {
                setDisposeAndClearAllActorsMsg();
            }

        } catch(Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }

    }


}