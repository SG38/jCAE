/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007,2010, by EADS France

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

package org.jcae.mesh.amibe.ds;

import java.util.Iterator;

import static org.junit.Assert.*;
import org.junit.Ignore;

@Ignore("Utility class")
public class AbstractHalfEdgeTest
{
	Mesh mesh;
	Vertex [] v;
	private Triangle [] T;
	private int vertexLabel = 0;
	private int originalVertexCount = 0;
	private boolean alterned = true;
	
	private void create3x4Shell()
	{
		/*
		 *  v9        v10        v11
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \  T9 | T10 /   |
		 *   |     \   |   /     |
		 *   |  T8   \ | /   T11 |
		 *v6 +---------+---------+ v8
		 *   | \       |v7     / |
		 *   |   \  T5 | T6  /   |
		 *   |     \   |   /     |
		 *   |  T4   \ | /   T7  |
		 *v3 +---------+---------+ v5
		 *   | \       |v4     / |
		 *   |   \  T1 | T2  /   |
		 *   |     \   |   /     |
		 *   |  T0   \ | /   T3  |
		 *   +---------+---------+
		 *   v0        v1       v2
		 *
		 */
		v = new Vertex[12];
		for (int i = 0; i < 4; i++)
		{
			v[3*i]   = mesh.createVertex(-1.0, i, 0.0);
			v[3*i+1] = mesh.createVertex(0.0, i, 0.0);
			v[3*i+2] = mesh.createVertex(1.0, i, 0.0);
		}
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);
		vertexLabel = v.length;
		originalVertexCount = v.length;

		T = new Triangle[12];
		for (int i = 0; i < 3; i++)
		{
			T[4*i]   = mesh.createTriangle(v[3*i], v[3*i+1], v[3*i+3]);
			T[4*i+1] = mesh.createTriangle(v[3*i+1], v[3*i+4], v[3*i+3]);
			T[4*i+2] = mesh.createTriangle(v[3*i+5], v[3*i+4], v[3*i+1]);
			T[4*i+3] = mesh.createTriangle(v[3*i+1], v[3*i+2], v[3*i+5]);
			v[3*i].setLink(T[4*i]);
			v[3*i+1].setLink(T[4*i]);
			v[3*i+2].setLink(T[4*i+3]);
		}
		v[9].setLink(T[9]);
		v[10].setLink(T[9]);
		v[11].setLink(T[11]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
	}
	
	// m Vertex on rows, n Vertex on columns
	private void createMxNShell(int m, int n)
	{
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
			for (int i = 0; i < m; i++)
				v[m*j+i] = mesh.createVertex(i, j, 0.0);
		for (int i = 0; i < v.length; i++)
		{
			v[i].setLabel(vertexLabel);
			vertexLabel++;
		}
		vertexLabel = v.length;
		originalVertexCount = v.length;
		T = createMxNTriangles(m, n, v);
	}

	private Triangle [] createMxNTriangles(int m, int n, Vertex [] vv)
	{
		/*   v3       v4        v5 
		 *   +---------+---------+
		 *   | \       | \       |
		 *   |   \  T1 |   \  T3 |
		 *   |     \   |     \   |
		 *   |  T0   \ |  T2   \ |
		 *   +---------+---------+
		 *   v0        v1       v2
		 * or
		 *   v3       v4        v5 
		 *   +---------+---------+
		 *   |       / |       / |
		 *   | T0  /   | T2  /   |
		 *   |   /     |   /     |
		 *   | /   T1  | /   T3  |
		 *   +---------+---------+
		 *   v0        v1       v2
		 */
		Triangle [] tt = new Triangle[2*(m-1)*(n-1)];
		for (int j = 0; j < n-1; j++)
		{
			if (j%2 == 0 || !alterned)
			{
				for (int i = 0; i < m-1; i++)
				{
					tt[2*(m-1)*j+2*i]   = mesh.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
					tt[2*(m-1)*j+2*i+1] = mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
					vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
				}
				vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
			}
			else
			{
				for (int i = 0; i < m-1; i++)
				{
					tt[2*(m-1)*j+2*i]   = mesh.createTriangle(vv[m*j+i], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
					tt[2*(m-1)*j+2*i+1] = mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*j+i]);
					vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
				}
				vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
			}
		}
		// Last row
		for (int i = 0; i < m-1; i++)
			vv[m*(n-1)+i].setLink(tt[2*(m-1)*(n-2)+2*i]);
		vv[m*n-1].setLink(tt[2*(m-1)*(n-1)-1]);
		int cnt = mesh.getTriangles().size();
		for (Triangle t: tt)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		return tt;
	}
	
	final Triangle [] rotateMxNShellAroundY(int m, int n, double angle)
	{
		// Create new vertices and append them to current mesh
		assert originalVertexCount == m*n;
		double [] xyz = new double[3];
		Vertex [] vy = new Vertex[originalVertexCount];
		double ct = Math.cos(angle*Math.PI / 180.0);
		double st = Math.sin(angle*Math.PI / 180.0);
		for (int i = 0; i < vy.length; i++)
		{
			if (i%m == 0)
				vy[i]   = v[i];
			else
			{
				v[i].get(xyz);
				vy[i]   = mesh.createVertex(ct*xyz[0]+st*xyz[2], xyz[1], -st*xyz[0]+ct*xyz[2]);
				vy[i].setLabel(vertexLabel);
				vertexLabel++;
			}
		}
		Vertex [] newV = new Vertex[v.length+vy.length];
		System.arraycopy(v, 0, newV, 0, v.length);
		System.arraycopy(vy, 0, newV, v.length, vy.length);
		v = newV;
		return createMxNTriangles(m, n, vy);
	}
	
	private void invertTriangles(Triangle [] tArray)
	{
		for (Triangle t: tArray)
		{
			Vertex temp = t.vertex[1];
			t.vertex[1] = t.vertex[2];
			t.vertex[2] = temp;
		}
	}

	final void buildMesh()
	{
		createMxNShell(3, 2);
		mesh.buildAdjacency();
	}
	
	final void buildMesh2()
	{
		create3x4Shell();
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
	final void buildMeshTopo()
	{
		/*
		 *    This mesh is a prism, it can be unrolled as:
		 *  v4        v3        v2        v4 
		 *   +---------+---------+---------+
		 *   | \       |       / |       / |
		 *   |   \  T2 | T1  /   | T4  /   |
		 *   |     \   |   /     |   /     |
		 *   |  T3   \ | /   T0  | /  T5   |
		 *   +---------+---------+---------+
		 *   v5        v0       v1        v5
		 *   Edge (v0,v1) cannot be collapsed because T3
		 *   and T5 would then be glued together and
		 *   edge (v4,v0) would become non-manifold.
		 */
		v = new Vertex[6];
		v[0] = mesh.createVertex(0.0, 0.0, 0.0);
		v[1] = mesh.createVertex(1.0, 0.0, 0.0);
		v[2] = mesh.createVertex(1.0, 1.0, 0.0);
		v[3] = mesh.createVertex(0.0, 1.0, 0.0);
		v[4] = mesh.createVertex(0.0, 1.0, 1.0);
		v[5] = mesh.createVertex(0.0, 0.0, 1.0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[6];
		T[0] = mesh.createTriangle(v[0], v[1], v[2]);
		T[1] = mesh.createTriangle(v[2], v[3], v[0]);
		T[2] = mesh.createTriangle(v[4], v[0], v[3]);
		T[3] = mesh.createTriangle(v[5], v[0], v[4]);
		T[4] = mesh.createTriangle(v[4], v[2], v[1]);
		T[5] = mesh.createTriangle(v[5], v[4], v[1]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[0]);
		v[3].setLink(T[2]);
		v[4].setLink(T[2]);
		v[5].setLink(T[3]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		mesh.buildAdjacency();
	}
	
	final void buildMeshNM(int m, int n, boolean alt)
	{
		/*  (2, 4, true)           (4, 4, true)
		 *  v6         v7      v12       v13       v14       v15
		 *   +---------+        +---------+---------+---------+
		 *   | \       |        | \     v5| \     v6| \       |
		 *   |   \  T5 |        |   \ T13 |   \ T15 |   \ T17 |
		 *   |     \   |        |     \   |     \   |     \   |
		 *   |  T4   \ |        |  T12  \ |  T14  \ |  T16  \ |
		 * v4+---------+ v5   v8+---------+---------+---------+v11
		 *   |       / |        |       / |v9     / |v10    / |
		 *   | T2  /   |        | T6  /   | T8  /   | T10 /   |
		 *   |   /     |        |   /     |   /     |   /     |
		 *   | /   T3  |        | /   T7  | /   T9  | /   T11 |
		 * v2+---------+ v3   v4+---------+---------+---------+v7
		 *   | \       |        | \     v5| \     v6| \       |
		 *   |   \  T1 |        |   \  T1 |   \  T3 |   \  T5 |
		 *   |     \   |        |     \   |     \   |     \   |
		 *   |  T0   \ |        |  T0   \ |  T2   \ |  T4   \ |
		 *   +---------+        +---------+---------+---------+
		 *  v0         v1      v0         v1        v2        v3
		 */
		this.alterned = alt;
		createMxNShell(m, n);
		rotateMxNShellAroundY(m, n, 90);
		rotateMxNShellAroundY(m, n, 180);
		Triangle [] tt = rotateMxNShellAroundY(m, n, 270);
		// Invert a set of triangles so that triangles in fans have
		// different orientations.
		invertTriangles(tt);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
	final void buildMesh3()
	{
		createMxNShell(3, 2);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	final void buildMesh3NM()
	{
		createMxNShell(3, 2);
		Vertex vTemp = T[0].vertex[0];
		T[0].vertex[0] = T[0].vertex[1];
		T[0].vertex[1] = vTemp;
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	final AbstractHalfEdge find(Vertex v1, Vertex v2)
	{
		if (v1.isManifold())
		{
			AbstractHalfEdge ret = findSameFan(v1, v2, (Triangle) v1.getLink());
			if (ret == null)
				throw new RuntimeException();
			return ret;
		}
		Triangle [] tArray = (Triangle []) v1.getLink();
		for (Triangle start: tArray)
		{
			AbstractHalfEdge f = findSameFan(v1, v2, start);
			if (f != null)
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					return f.sym();
				return f;
			}
		}
		throw new RuntimeException();
	}

	private AbstractHalfEdge findSameFan(Vertex v1, Vertex v2, Triangle start)
	{
		AbstractHalfEdge ret = start.getAbstractHalfEdge();
		if (ret == null)
			throw new RuntimeException();
		if (ret.destination() == v1)
			ret = ret.next();
		else if (ret.apex() == v1)
			ret = ret.prev();
		assertTrue(ret.origin() == v1);
		Vertex d = ret.destination();
		if (d == v2)
			return ret;
		do
		{
			ret = ret.nextOriginLoop();
			if (ret.destination() == v2)
				return ret;
		}
		while (ret.destination() != d);
		return null;
	}
	
	void nextOriginLoop()
	{
		// Loop around v0
		AbstractHalfEdge e = find(v[0], v[1]);
		assertTrue(e.origin() == v[0]);
		assertTrue(e.getTri() == T[0]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.destination() == v[3]);
		assertTrue(e.apex() == mesh.outerVertex);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.destination() == mesh.outerVertex);
		assertTrue(e.apex() == v[1]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.getTri() == T[0]);
		// Loop around v1
		e = find(v[1], v[2]);
		for (int i = 3; i >= 0; i--)
		{
			assertTrue(e.origin() == v[1]);
			assertTrue(e.getTri() == T[i]);
			e = e.nextOriginLoop();
		}
		assertTrue(e.origin() == v[1]);
		assertTrue(e.destination() == v[0]);
		assertTrue(e.apex() == mesh.outerVertex);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[1]);
		assertTrue(e.destination() == mesh.outerVertex);
		assertTrue(e.apex() == v[2]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[1]);
		assertTrue(e.getTri() == T[3]);
		// Loop around v4
		e = e.nextOrigin().prev();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[2]);
		for (int i = 7; i >= 4; i--)
		{
			e = e.nextOriginLoop();
			assertTrue(e.origin() == v[4]);
			assertTrue(e.getTri() == T[i]);
		}
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[1]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[2]);
	}
	
	final void canCollapse(Vertex o, Vertex d, Vertex n, boolean expected)
	{
		AbstractHalfEdge e = find(o, d);
		assertTrue(expected == e.canCollapse(mesh, n));
	}

	final AbstractHalfEdge collapse(Vertex o, Vertex d, Vertex n)
	{
		AbstractHalfEdge e = find(o, d);
		Vertex a = e.apex();
		e = e.collapse(mesh, n);
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Error in origin", n == e.origin());
		assertTrue("Error in destination", a == e.apex());
		return e;
	}

	final void swap(Vertex o, Vertex d)
	{
		AbstractHalfEdge e = find(o, d);
		Vertex a = e.apex();
		e = e.swap(mesh);
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Error in origin", o == e.origin());
		assertTrue("Error in apex", a == e.apex());
	}

	protected final AbstractHalfEdge split(Vertex o, Vertex d, Vertex n)
	{
		AbstractHalfEdge e = find(o, d);
		boolean reversed = (e.origin() != o);
		e = e.split(mesh, n);
		assertTrue("Mesh is not valid", mesh.isValid());
		if (reversed)
		{
			assertTrue("Error in origin", d == e.origin());
			assertTrue("Error in destination", n == e.destination());
		}
		else
		{
			assertTrue("Error in origin", o == e.origin());
			assertTrue("Error in destination", n == e.destination());
		}
		return e;
	}

	final void countVertexLinks(Vertex o, int count)
	{
		int n = 0;
		if (o.isManifold())
			n = 1;
		else
			n = ((Triangle []) o.getLink()).length;
		assertTrue("Found "+n+" links instead of "+count, n == count);
	}

	final void countEdgeLinks(Vertex o, Vertex d, int count)
	{
		AbstractHalfEdge e = find(o, d);
		int n = 0;
		if (!e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			n++;
			if (!e.hasAttributes(AbstractHalfEdge.BOUNDARY))
				n++;
		}
		else
		{
			if (e.hasAttributes(AbstractHalfEdge.OUTER))
				e = e.sym();
			for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); it.next())
				n++;
		}
		assertTrue("Found "+n+" incident triangles instead of "+count, n == count);
	}

	final void countFanIterator(Vertex o, Vertex d, int count)
	{
		AbstractHalfEdge e = find(o, d);
		int n = 0;
		for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); it.next())
			n++;
		assertTrue("Found "+n+" fans instead of "+count, n == count);
	}
}
