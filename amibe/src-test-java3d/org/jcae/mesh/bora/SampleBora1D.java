/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

package org.jcae.mesh.bora;

import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;

import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.View;

import javax.media.j3d.Appearance;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.LineAttributes;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.xml.sax.SAXException;

public class SampleBora1D
{
	private static BranchGroup [] getBranchGroups(BModel model)
	{
		BCADGraphCell root = model.getGraph().getRootCell();
		// Count edges
		int nEdges = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = it.next();
			if (edge.getOrientation() != 0)
			{
				if (edge.getReversed() != null)
					edge = edge.getReversed();
			}
			BDiscretization d = edge.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d), "n");
			if (!nodesfile.exists())
				continue;
			File parasfile = new File(model.getOutputDir(d), "p");
			if (!parasfile.exists())
				continue;
			nEdges++;
		}
		// Count nodes and beams on each edge
		int [] nrNodes = new int[nEdges+1];
		int [] nrBeams = new int[nEdges+1];
		nrNodes[0] = 0;
		nrBeams[0] = 0;
		nEdges = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = it.next();
			if (edge.getOrientation() != 0)
			{
				if (edge.getReversed() != null)
					edge = edge.getReversed();
			}
			BDiscretization d = edge.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d), "n");
			if (!nodesfile.exists())
				continue;
			File parasfile = new File(model.getOutputDir(d), "p");
			if (!parasfile.exists())
				continue;
			nrNodes[nEdges+1] = nrNodes[nEdges] + (int) nodesfile.length() / 24;
			File beamsfile = new File(model.getOutputDir(d), "b");
			if (!beamsfile.exists())
				continue;
			nrBeams[nEdges+1] = nrBeams[nEdges] + (int) beamsfile.length() / 8;
			nEdges++;
		}

		int nVertices = nrNodes[nEdges];
		int nBeams = nrBeams[nEdges];

		double [] xyz = new double[3*nVertices];
		double [] x1 = new double[nVertices];
		double [] x3d1 = new double[3*nVertices];
		int [] beams = new int[2*nBeams];
		CADShapeFactory factory = CADShapeFactory.getFactory();

		nEdges = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = it.next();
			if (edge.getOrientation() != 0)
			{
				if (edge.getReversed() != null)
					edge = edge.getReversed();
			}
			BDiscretization d = edge.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			try
			{
				File nodesfile = new File(model.getOutputDir(d), "n");
				if (!nodesfile.exists())
					continue;
				File parasfile = new File(model.getOutputDir(d), "p");
				if (!parasfile.exists())
					continue;
				File beamsfile = new File(model.getOutputDir(d), "b");
				if (!beamsfile.exists())
					continue;

				int nr = nrNodes[nEdges+1] - nrNodes[nEdges];
				FileChannel fcP = new FileInputStream(parasfile).getChannel();
				MappedByteBuffer bbP = fcP.map(FileChannel.MapMode.READ_ONLY, 0L, fcP.size());
				DoubleBuffer parasBuffer = bbP.asDoubleBuffer();
				parasBuffer.get(x1, 0, nr);
				fcP.close();
				CADGeomCurve3D curve = factory.newCurve3D((CADEdge) edge.getShape());
				for (int i = 0; i < nr; i++)
				{
					double [] x3 = curve.value(x1[i]);
					x3d1[3*nrNodes[nEdges]+3*i]   = x3[0];
					x3d1[3*nrNodes[nEdges]+3*i+1] = x3[1];
					x3d1[3*nrNodes[nEdges]+3*i+2] = x3[2];
				}
				FileChannel fcN = new FileInputStream(nodesfile).getChannel();
				MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
				DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
				nodesBuffer.get(xyz, 3*nrNodes[nEdges], 3*nr);
				fcN.close();

				nr = nrBeams[nEdges+1] - nrBeams[nEdges];
				FileChannel fcB = new FileInputStream(beamsfile).getChannel();
				MappedByteBuffer bbB = fcB.map(FileChannel.MapMode.READ_ONLY, 0L, fcB.size());
				IntBuffer beamsBuffer = bbB.asIntBuffer();
				beamsBuffer.get(beams, 2*nrBeams[nEdges], 2*nr);
				// Add node offset
				for (int i = 0; i < 2*nr; i++)
					beams[2*nrBeams[nEdges]+i] += nrNodes[nEdges] - 1;
				fcB.close();
				nEdges++;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		BranchGroup [] ret = new BranchGroup[3];
		PointAttributes pa = new PointAttributes();
		pa.setPointSize(4.0f);
		int iRet = -1;

		// 3D edges
		iRet++;
		ret[iRet] = new BranchGroup();
		IndexedLineArray l = new IndexedLineArray(nVertices,
			GeometryArray.COORDINATES,
			beams.length);
		l.setCoordinateIndices(0, beams);
		l.setCoordinates(0, xyz);
		l.setCapability(GeometryArray.ALLOW_COUNT_READ);
		l.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		l.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
		l.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);

		Appearance lineApp = new Appearance();
		lineApp.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,false));
		lineApp.setColoringAttributes(new ColoringAttributes(1f,0f,0f,ColoringAttributes.SHADE_FLAT));

		Shape3D shapeEdges = new Shape3D(l, lineApp);
		shapeEdges.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapeEdges.setPickable(false);
		ret[iRet].addChild(shapeEdges);

		// 1D nodes
		iRet++;
		ret[iRet] = new BranchGroup();
		PointArray p1 = new PointArray(nVertices, GeometryArray.COORDINATES);
		p1.setCoordinates(0, x3d1);
		Appearance vert1App = new Appearance();
		vert1App.setPointAttributes(pa);
		vert1App.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint1=new Shape3D(p1, vert1App);
		shapePoint1.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint1.setPickable(false);
		ret[iRet].addChild(shapePoint1);

		// 3D nodes
		iRet++;
		ret[iRet] = new BranchGroup();
		PointArray p = new PointArray(nVertices, GeometryArray.COORDINATES);
		p.setCoordinates(0, xyz);

		Appearance vertApp = new Appearance();
		vertApp.setPointAttributes(pa);
		vertApp.setColoringAttributes(new ColoringAttributes(1f,1f,0f,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint = new Shape3D(p, vertApp);
		shapePoint.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint.setPickable(false);
		ret[iRet].addChild(shapePoint);

		return ret;
	}

	public static void main(String args[]) throws SAXException, IOException
	{
		final BModel model = BModelReader.readObject(args[0]);
		JFrame feFrame = new JFrame("Bora Demo");
		final View view = new View(feFrame);
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final BranchGroup [] bgList = getBranchGroups(model);
		final ViewableBG [] viewList = new ViewableBG[bgList.length];
		// bgList:
		//   0: edges
		//   1: 1D nodes on the 3D curve
		//   2: 3D nodes
		final boolean [] active = new boolean[bgList.length];
		for (int i = 0; i < bgList.length; i++)
		{
			active[i] = true;
			viewList[i] = new ViewableBG(bgList[i]);
		}
		for (int i = 0; i < bgList.length; i++)
			if (active[i])
				view.add(viewList[i]);
		view.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event)
			{
				char k = event.getKeyChar();
				if ('1' == k)
					active[1] = !active[1];
				else if ('3' == k)
					active[2] = !active[2];
				else if ('e' == k)
					active[0] = !active[0];
				else if ('q' == k)
					System.exit(0);
				else
					return;
				for (int i = 0; i < bgList.length; i++)
				{
					view.remove(viewList[i]);
					if (active[i])
						view.add(viewList[i]);
				}
			}
		});
		view.fitAll(); 
		feFrame.getContentPane().add(view);
		feFrame.setVisible(true);
	}
}
