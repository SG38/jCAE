/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005 Jerome Robert <jeromerobert@users.sourceforge.net>
    Copyright (C) 2008, by EADS France

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
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.validation.*;

import java.io.File;
import java.io.IOException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	static Logger logger=Logger.getLogger(MeshOEMMViewer3d.class.getName());
	static ViewableBG fineMesh;
	static ViewableFE decMesh;

	static boolean showOctree = true;
	static boolean showAxis = true;
	static boolean showFPS = false;
	static boolean showNonReadableTriangles = false;

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
		final MeshReader mr = new MeshReader(oemm);
		final View bgView=new View(feFrame);
		final ViewableBG octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));
		FPSBehavior fpsB = new FPSBehavior();
		fpsB.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.MAX_VALUE));
		fpsB.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				logger.info("FPS>" + evt.getNewValue());
			}
		});
		BranchGroup fpsBG = new BranchGroup();
		fpsBG.addChild(fpsB);
		final ViewableBG fps = new ViewableBG(fpsBG);
		try
		{
			bgView.add(octree);
			if (showFPS)
				bgView.add(fps);

			bgView.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event)
				{
					char k = event.getKeyChar();
					if(k == '?')
					{
						printInteractiveUsage();
					}
					else if (k == 'w')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						fineMesh = new ViewableBG(OEMMViewer.meshOEMM(mr.buildWholeMesh()));
						//octree.unselectAll();
						bgView.add(fineMesh);
					}
					else if (k == 'n')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						fineMesh = new ViewableBG(OEMMViewer.meshOEMM(mr.buildMesh(octree.getResultSet())));
						//octree.unselectAll();
						bgView.add(fineMesh);
					}
					else if (k == 'o')
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
					else if (k == 'a')
					{
						showAxis = !showAxis;
						bgView.setOriginAxisVisible(showAxis);
					}
					else if (k == 'F')
					{
						showFPS = !showFPS;
						if (showFPS)
						{
							bgView.add(fps);
							bgView.setCurrentViewable(fps);
						}
						else
							bgView.remove(fps);
					}
					else if (k == 'R')
					{
						showNonReadableTriangles = !showNonReadableTriangles;
						logger.info("Show non-readable triangles: "+showNonReadableTriangles);
						mr.setLoadNonReadableTriangles(showNonReadableTriangles);
					}
					else if (k == 's')
					{
						TIntHashSet leaves = octree.getResultSet();
						Mesh amesh = mr.buildMesh(leaves);
						Storage.saveNodes(oemm, amesh, leaves);
					}
					else if (k == 'S')
					{
						Mesh amesh = mr.buildWholeMesh();
						try
						{
							MeshWriter.writeObject3D(amesh, "WholeMesh", null);
							logger.info("Mesh successfully written into 'WholeMesh' directory");
						}
						catch(IOException ex)
						{
							logger.severe("Unable to print mesh into 'WholeMesh' directory");
						}
					}
					else if (k == 'i')
					{
						if (logger.isLoggable(Level.INFO))
							logger.info("Selected: " + octree.getResultSet());
					}
					else if (k == 'c')
					{
						TIntHashSet leaves = octree.getResultSet();
						if (leaves.size() == 1)
						{
							int idx = leaves.iterator().next();
							OEMM.Node current = oemm.leaves[idx];
							Mesh amesh = mr.buildMesh(leaves);
							MinAngleFace qproc = new MinAngleFace();
							QualityFloat data = new QualityFloat(amesh.getTriangles().size());
							data.setQualityProcedure(qproc);
							for (Triangle f: amesh.getTriangles())
							{
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
							logger.severe("Only one node must be selected!");
						}
					}
					else if (k == 'd')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						Mesh amesh = mr.buildMesh(octree.getResultSet());
						HashMap<String, String> opts = new HashMap<String, String>();
						opts.put("maxtriangles", Integer.toString(amesh.getTriangles().size() / 100));
						new org.jcae.mesh.amibe.algos3d.QEMDecimateHalfEdge(amesh, opts).compute();
						String xmlDir = "dec-tmp";
						octree.unselectAll();
						try
						{
							MeshWriter.writeObject3D(amesh, xmlDir, "dummy.brep");
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
					else if (k == 'p')
					{
						int triangles = 0;
						int vertices = 0;
						for(OEMM.Node current: oemm.leaves)
						{
							triangles += current.tn;
							vertices += current.vn;
						}
						System.out.println("Total mesh: "+triangles+" triangles and "+vertices+" vertices");
					}
					else if (k == 'q')
						System.exit(0);
				}
			});
			bgView.fitAll();
			bgView.setOriginAxisVisible(showAxis);
			feFrame.getContentPane().add(bgView);
			feFrame.setVisible(true);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	static final void printInteractiveUsage()
	{
		System.out.println("Key usage:");
		System.out.println("  ?: Display this help message");
		System.out.println("  q: Exit");
		System.out.println("  w: Display whole mesh");
		System.out.println("  n: Display mesh loaded from selected octree nodes");
		System.out.println("  R: Toggle display of non-readable triangles");
		System.out.println("  o: Toggle display of octree boxes");
		System.out.println("  a: Toggle axis display");
		System.out.println("  F: Toggle FPS display");
		System.out.println("  s: Load selected octree nodes and store them back to disk");
		System.out.println("  S: Save whole mesh in Amibe format into 'WholeMesh' directory");
		System.out.println("  i: Print selected nodes");
		System.out.println("  c: When a single octree node is selected, compute its mesh quality");
		System.out.println("  d: Decimate selected octree nodes into dec-tmp directory");
		System.out.println("  p: Print mesh statistics");
	}
}
