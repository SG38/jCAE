/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh;

import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.validation.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;

import org.jcae.viewer3d.FPSBehavior;
import org.jcae.viewer3d.OEMMViewer;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.FEDomain;
import org.jcae.viewer3d.View;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshOEMMViewer3d
{
	private static Logger logger=Logger.getLogger(MeshOEMMViewer3d.class);
	private static ViewableBG fineMesh;
	private static ViewableFE decMesh;

	private static boolean showOctree = true;
	private static boolean showAxis = true;

	public static void main(String args[])
	{
		if (args.length < 1)
		{
			System.out.println("Usage: MeshOEMMViewer3d dir");
			System.exit(0);
		}
		String dir=args[0];
		JFrame feFrame=new JFrame("jCAE Demo");
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final OEMM oemm = Storage.readOEMMStructure(dir);
		final View bgView=new View(feFrame);
		final ViewableBG octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));
		try
		{
			bgView.add(octree);
			bgView.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent event)
				{
					if(event.getKeyChar()=='A')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						Set<Integer> set = new HashSet<Integer>();
						for(OEMM.Node in: oemm.leaves) {
							set.add(in.leafIndex);
						}
						fineMesh = new ViewableBG(OEMMViewer.meshOEMM(oemm, set));
						//octree.unselectAll();
						bgView.add(fineMesh);
					}
					else if(event.getKeyChar()=='n')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						fineMesh = new ViewableBG(OEMMViewer.meshOEMM(oemm, octree.getResultSet()));
						//octree.unselectAll();
						bgView.add(fineMesh);
					}
					else if(event.getKeyChar()=='o')
					{
						showOctree = !showOctree;
						if (showOctree)
						{
							bgView.add(octree);
							bgView.setCurrentViewable(octree);
						}
						else
							bgView.remove(octree);
					}
					else if(event.getKeyChar()=='s')
					{
						Mesh mesh = Storage.loadNodes(oemm, octree.getResultSet(), true, false);
						Storage.saveNodes(oemm, mesh, octree.getResultSet());
					}
					else if(event.getKeyChar()=='c')
					{
						Set<Integer> leaves = octree.getResultSet();
						if (leaves.size() == 1)
						{
							int idx = leaves.iterator().next();
							OEMM.Node current = oemm.leaves[idx];
							Mesh amesh = Storage.loadNodeWithNeighbours(oemm, idx, false);
							MinAngleFace qproc = new MinAngleFace();
							QualityFloat data = new QualityFloat(amesh.getTriangles().size());
							data.setQualityProcedure(qproc);
							for (Iterator itf = amesh.getTriangles().iterator(); itf.hasNext();)
							{
								Triangle f = (Triangle) itf.next();
								if (f.getGroupId() == idx)
									data.compute(f);
							}
							data.setTarget((float) Math.PI/3.0f);
							String outFile = oemm.getDirectory()+File.separator+current.file+"q";
							data.writeRawData(outFile);
							logger.info("Quality factor written into "+outFile);
						}
						else
						{
							logger.error("Only one node must be selected!");
						}
					}
					else if(event.getKeyChar()=='d')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						Mesh amesh = Storage.loadNodes(oemm, octree.getResultSet(), true, false);
						HashMap opts = new HashMap();
						opts.put("maxtriangles", Integer.toString(amesh.getTriangles().size() / 100));
						new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, opts).compute();
						String xmlDir = "dec-tmp";
						String xmlFile = "jcae3d";
						MeshWriter.writeObject3D(amesh, xmlDir, xmlFile, ".", "tmp.brep");
						octree.unselectAll();
						try
						{
							AmibeProvider ap = new AmibeProvider(new File(xmlDir));
							decMesh = new ViewableFE(ap);                
							int [] ids = ap.getDomainIDs();
							logger.info("Nr. of triangles: "+((FEDomain)ap.getDomain(ids[0])).getNumberOfTria3());
							bgView.add(decMesh);
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
					else if(event.getKeyChar()=='a')
					{
						showAxis = !showAxis;
						bgView.setOriginAxisVisible(showAxis);
					}
					else if(event.getKeyChar()=='q')
						System.exit(0);
				}
			});
			FPSBehavior fps = new FPSBehavior();
			fps.setSchedulingBounds(new BoundingSphere(
					new Point3d(), Double.MAX_VALUE));
			fps.addPropertyChangeListener(new PropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (logger.isInfoEnabled()) {
						logger.info("FPS>" + evt.getNewValue());
					}
				}
				
			});
			BranchGroup bg = new BranchGroup();
			bg.addChild(fps);
			bgView.addBranchGroup(bg);
			bgView.fitAll();
			feFrame.getContentPane().add(bgView);
			feFrame.setVisible(true);
			bgView.setOriginAxisVisible(showAxis);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
