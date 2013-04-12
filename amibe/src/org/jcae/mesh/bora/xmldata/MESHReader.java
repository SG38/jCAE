/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France
 
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

package org.jcae.mesh.bora.xmldata;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.logging.Logger;


public class MESHReader
{

	private static final Logger LOGGER = Logger.getLogger(MESHReader.class.getName());
	
	public static void readMesh(Mesh mesh, String file)
	{
		TIntObjectHashMap<Vertex> nodesmap = null;
		String line = "";
		try
		{
			FileInputStream in = new FileInputStream(file);
			BufferedReader rd=new BufferedReader(new InputStreamReader(in));
			while ((line=rd.readLine())!=null)
			{
				if (line.trim().equals("Vertices"))
					nodesmap = readNodes(mesh, rd);
				else if (line.trim().equals("Tetrahedra"))
					readFace(rd, mesh, nodesmap);
			}
			in.close();
		}
		catch(Exception e)
		{
				e.printStackTrace();
		}
	}

	private static TIntObjectHashMap<Vertex> readNodes(Mesh m, BufferedReader rd)
	{
		LOGGER.fine("Reading nodes");
		TIntObjectHashMap<Vertex> nodesmap = null;
		double x,y,z;
		String line = "";
		int nrNodes = 0;
		try
		{
			line = rd.readLine();
			nrNodes = Integer.valueOf(line).intValue();
			m.ensureCapacity(2*nrNodes);
			nodesmap = new TIntObjectHashMap<Vertex>(nrNodes);
			for (int i = 1; i <= nrNodes; i++)
			{
				line = rd.readLine();
				//line contains coord x,y,z
				StringTokenizer st = new StringTokenizer(line);
				String x1 = st.nextToken();
				String y1 = st.nextToken();
				String z1 = st.nextToken();
				x1 = x1.replace('D','E');
				y1 = y1.replace('D','E');
				z1 = z1.replace('D','E');
				x = Double.parseDouble(x1);
				y = Double.parseDouble(y1);
				z = Double.parseDouble(z1);
				Vertex n = m.createVertex(x,y,z);
				if (m.hasNodes())
					m.add(n);
				nodesmap.put(i, n);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		LOGGER.fine("Found "+nrNodes+" nodes");
		return nodesmap;
	}

	private static void readFace(BufferedReader rd, Mesh mesh, TIntObjectHashMap<Vertex> nodesmap)
	{
		LOGGER.fine("Reading tetrahedra");
		String line = "";
		int nrTetrahedra = 0;
		Vertex [] v = new Vertex[4];
		try
		{
			line = rd.readLine();
			nrTetrahedra = Integer.valueOf(line).intValue();
			mesh.ensureCapacity(nrTetrahedra);
			for (int i = 0; i < nrTetrahedra; i++)
			{
				line = rd.readLine();
				StringTokenizer st = new StringTokenizer(line);
				for (int j = 0; j < 4; j++)
				{
					int p = Integer.valueOf(st.nextToken()).intValue();
					v[j] = nodesmap.get(p);
					if (v[j] == null)
						throw new RuntimeException();
				}
				Triangle f = mesh.createTetrahedron(v);
				mesh.add(f);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		LOGGER.fine("Found "+nrTetrahedra+" tetrahedra");
	}
	
}
