/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

import org.jcae.mesh.amibe.metrics.Location;

public interface ElementFactoryInterface
{
	public Vertex createVertex(double u, double v);
	public Vertex createVertex(double x, double y, double z);
	public Vertex createVertex(Location x);
	public Triangle createTriangle(Vertex v0, Vertex v1, Vertex v2);
	public Triangle createTriangle(Vertex [] v);
	public Triangle createTriangle(Triangle that);
	public boolean hasAdjacency();
}
