/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;

/**
 * Creates triangles and their constituent vertices, to wrap a point
 * with a sphere.
 * 
 * @author fosterl
 */
public class PointEnclosureFactory implements TriangleSource  {
    private final List<VertexInfoBean> vtxInfoBeans;
    private final List<Triangle> triangles;
    private final Map<Integer, VertexInfoBean> offsetToVertex;
    
    private PointPrototypeHelper prototypeHelper;
    
    private int numSides;
    
    private int currentVertexNumber = 0;
    
    public PointEnclosureFactory(int numSides, double radius) {
        vtxInfoBeans = new ArrayList<>();
        triangles = new ArrayList<>();
        offsetToVertex = new HashMap<>();        

        setCharacteristics(numSides, radius);
    }

    /**
     * If transitioning to a different number-of-sides or radius, invoke this.
     * It will also be called at construction.
     * 
     * @param numSides how many sides in the "lateral rings" making the sphere?
     * @param radius how wide is the largest lateral ring?
     */
    public final void setCharacteristics(int numSides, double radius) {
        this.numSides = numSides;
        prototypeHelper = new PointPrototypeHelper(numSides, radius);
    }

    /**
     * @return the currentVertexNumber
     */
    public int getCurrentVertexNumber() {
        return currentVertexNumber;
    }

    /**
     * @param currentVertexNumber the currentVertexNumber to set
     */
    public void setCurrentVertexNumber(int currentVertexNumber) {
        this.currentVertexNumber = currentVertexNumber;
    }
    
    @Override
    public List<VertexInfoBean> getVertices() {
        return vtxInfoBeans;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    public void addEnclosure(double[] pointCoords, float[] color) {
        // Making a new bean for every point.
        int enclosureBaseIndex = currentVertexNumber;
        for (Matrix point: prototypeHelper.getPrototypePoints()) {
            beanFromPoint(point, pointCoords, color);
        }
        createTriangles(numSides, enclosureBaseIndex);
    }
    
    /**
     * Given a point matrix, create a new vertex bean.
     * 
     * @param point triple telling position of point.
     */
    private VertexInfoBean beanFromPoint(Matrix point, double[] pointCoords, float[] color) {
        VertexInfoBean bean = new VertexInfoBean();
        
        VertexInfoKey key = new VertexInfoKey();
        // Move the coords to be relative to the incoming point coords.
        Matrix transform = Matrix.identity(4, 4);
        transform.set(0, 3, pointCoords[0]);
        transform.set(1, 3, pointCoords[1]);
        transform.set(2, 3, pointCoords[2]);
        point = transform.times(point);
        
        key.setPosition(new double[] { point.get(0, 0), point.get(1, 0), point.get(2, 0) });
        bean.setKey(key);
        bean.setAttribute( VertexInfoBean.KnownAttributes.b_color.toString(), color, 3 );
        offsetToVertex.put( currentVertexNumber, bean);
        bean.setVtxBufOffset(currentVertexNumber ++);
        
        vtxInfoBeans.add( bean );
        return bean;
    }

    private void createTriangles(int numSides, int offset) {
        List<Matrix> prototypePoints = prototypeHelper.getPrototypePoints();

        // Must apply modulo op, to allow triangles to include points from
        // either end of the total.
        int totalVertexCount = offsetToVertex.size();
        // Now create triangles.
        for (int i = 0; i < prototypePoints.size(); i++) {
            Triangle triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get((offset + i) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + i + 1)  % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + i + numSides) % totalVertexCount));
            triangles.add(triangle);

            triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get((offset + i + 1) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + i + numSides + 1) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + i + numSides) % totalVertexCount));
            triangles.add(triangle);
        }

        // Include end caps.
        // Winding to point close end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get((offset + 0) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + (numSides - i - 1)) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + (numSides - i - 2)) % totalVertexCount));
            triangles.add(triangle);
        }

        // Winding to point far end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            int initialVertex = prototypePoints.size() - numSides;
            triangle.addVertex(offsetToVertex.get((offset + initialVertex) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + initialVertex + i + 2) % totalVertexCount));
            triangle.addVertex(offsetToVertex.get((offset + initialVertex + i + 1) % totalVertexCount));
            triangles.add(triangle);
        }
    }

}
