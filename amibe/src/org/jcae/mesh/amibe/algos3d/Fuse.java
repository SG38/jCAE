/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.KdTree;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * (Obsolete) Fuse near nodes in a <code>MMesh3D</code> instance.
 */
public class Fuse
{
	private static final Logger LOGGER=Logger.getLogger(Fuse.class.getName());
	private final Mesh mesh;
	private final double tolerance;
	
	/**
	 * Creates a <code>Fuse</code> instance.
	 *
	 * @param m  the <code>MMesh3D</code> instance to fuse.
	 */
	public Fuse(Mesh m)
	{
		this(m, 0.0);
	}
	
	/**
	 * Creates a <code>Fuse</code> instance.
	 *
	 * @param m  the <code>MMesh3D</code> instance to refine.
	 * @param eps  tolerance.
	 */
	public Fuse(Mesh m, double eps)
	{
		mesh = m;
		tolerance = eps*eps;
	}
	
	/**
	 * Fuse boundary nodes which are closer than a given bound.
	 */
	public void compute()
	{
		LOGGER.fine("Running Fuse");
		double [] bmin = new double[3];
		double [] bmax = new double[3];
		for (int i = 0; i < 3; i++)
		{
			bmin[i] = Double.MAX_VALUE;
			bmax[i] = Double.MIN_VALUE;
		}
		for (Vertex n: mesh.getNodes())
		{
			bmin[0] = Math.min(bmin[0], n.getX());
			bmax[0] = Math.max(bmax[0], n.getX());
			bmin[1] = Math.min(bmin[1], n.getY());
			bmax[1] = Math.max(bmax[1], n.getY());
			bmin[2] = Math.min(bmin[2], n.getZ());
			bmax[2] = Math.max(bmax[2], n.getZ());
		}
		//  Enlarge the bounding box
		for (int i = 0; i < 3; i++)
		{
			if (bmin[i] > 0.0)
				bmin[i] *= 0.99;
			else
				bmin[i] *= 1.01;
			if (bmax[i] > 0.0)
				bmax[i] *= 1.01;
			else
				bmax[i] *= 0.99;
		}
		double [] bbox = new double[6];
		for (int i = 0; i < 3; i++)
		{
			bbox[i] = bmin[i];
			bbox[i+3] = bmax[i];
		}
		KdTree<Vertex> octree = new KdTree<Vertex>(bbox);
		HashMap<Vertex, Vertex> map = new HashMap<Vertex, Vertex>();
		int nSubst = 0;
		for (Vertex n: mesh.getNodes())
		{
			if (n.getRef() <= 0)
				continue;
			Vertex p = octree.getNearestVertex(mesh.getMetric(n), n);
			if (p == null || p.getRef() <= 0 || n.sqrDistance3D(p) > tolerance)
				octree.add(n);
			else
			{
				LOGGER.fine("Node "+n+" is removed, it is too close from "+p);
				nSubst++;
				map.put(n, p);
				n.setRef(0);
			}
		}
		LOGGER.fine(""+nSubst+" node(s) are removed");
		for (Triangle t: mesh.getTriangles())
		{
			for (int j = 0; j < 3; j++)
			{
				Vertex n = t.vertex[j];
				Vertex p = map.get(n);
				if (p != null)
				{
					t.vertex[j] = p;
					mesh.remove(n);
				}
			}
		}
	}
}
