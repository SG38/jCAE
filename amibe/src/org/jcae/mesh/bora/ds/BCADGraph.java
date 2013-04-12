/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.bora.ds;

import gnu.trove.map.hash.TCustomHashMap;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADSolid;
import org.jcae.mesh.cad.CADVertex;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mesh graph.
 */
public class BCADGraph
{
	private static final Logger LOGGER=Logger.getLogger(BCADGraph.class.getName());
	// Backward link to the model
	private final BModel model;
	// Cell root
	private final BCADGraphCell root;
	// Map between topological elements and graph cells
	private final Map<CADShape, BCADGraphCell> cadShapeToGraphCell =
			new TCustomHashMap<CADShape, BCADGraphCell>(KeepOrientationHashingStrategy.getInstance());
	// Map between indices and graph cells or user-defined groups
	private final TIntObjectHashMap<BCADGraphCell> indexToCell = new TIntObjectHashMap<BCADGraphCell>();
	// First free index
	private final int freeIndex;

	/**
	 * Creates a root mesh.
	 */
	BCADGraph (BModel m, CADShape shape)
	{
		model = m;
		if (shape instanceof CADSolid)
			root = new BCADGraphCell(this, shape, CADShapeEnum.SOLID);
		else if (shape instanceof CADFace)
			root = new BCADGraphCell(this, shape, CADShapeEnum.FACE);
		else if (shape instanceof CADEdge)
			root = new BCADGraphCell(this, shape, CADShapeEnum.EDGE);
		else if (shape instanceof CADVertex)
			root = new BCADGraphCell(this, shape, CADShapeEnum.VERTEX);
		else
			root = new BCADGraphCell(this, shape, CADShapeEnum.COMPOUND);

		// Build the whole graph
		THashMap<CADShape, CADShape> seen = new THashMap<CADShape, CADShape>();
		CADShapeFactory factory = CADShapeFactory.getFactory();
		CADExplorer exp = factory.newExplorer();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			for (exp.init(shape, cse); exp.more(); exp.next())
			{
				CADShape sub = exp.current();
				if (cadShapeToGraphCell.containsKey(sub))
					continue;
				BCADGraphCell cell = new BCADGraphCell(this, sub, cse);
				cadShapeToGraphCell.put(sub, cell);
				CADShape r = seen.get(sub);
				if (r != null)
				{
					BCADGraphCell rev = cadShapeToGraphCell.get(r);
					rev.bindReversed(cell);
				}
				seen.put(sub, sub);
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, "  Add submesh: "+sub+" "+cell);
			}
		}
		// Add indices
		int i = 1;
		CADExplorer exp2 = factory.newExplorer();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.COMPOUND, CADShapeEnum.VERTEX))
		{
			for (exp.init(shape, cse); exp.more(); exp.next())
			{
				CADShape s = exp.current();
				BCADGraphCell c = cadShapeToGraphCell.get(s);
				if (c != null && c.getId() <= 0)
				{
					c.setId(i);
					indexToCell.put(i, c);
					i++;
				}
			}
		}
		freeIndex = i;

		// Add backward links
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.COMPOUND, CADShapeEnum.VERTEX))
		{
			for (exp.init(shape, cse); exp.more(); exp.next())
			{
				CADShape s = exp.current();
				BCADGraphCell c = cadShapeToGraphCell.get(s);
				if (c == null)
					continue;

				for (CADShapeEnum cse2 :CADShapeEnum.iterable(cse, CADShapeEnum.VERTEX))
				{
					for (exp2.init(s, cse2); exp2.more(); exp2.next())
					{
						CADShape s2 = exp2.current();
						BCADGraphCell c2 = cadShapeToGraphCell.get(s2);
						if (c2 != null)
							c2.addParent(c);
					}
				}
			}
		}
	}

	public final BModel getModel()
	{
		return model;
	}

	public final BCADGraphCell getRootCell()
	{
		return root;
	}

	public final int getFreeIndex()
	{
		return freeIndex;
	}

	/**
	 * Gets a graph cell by its shape
	 *
	 * @param  s  CAD shape
	 * @return  graph cell representing s
	 */
	public final BCADGraphCell getByShape(CADShape s)
	{
		return cadShapeToGraphCell.get(s);
	}

	/**
	 * Gets a graph cell by its identifier
	 *
	 * @param  i  cell index
	 * @return  graph cell with identifier i
	 */
	public final BCADGraphCell getById(int i)
	{
		return indexToCell.get(i);
	}

	/**
	 * Returns the list of cells for a given dimension.
	 */
	public final Collection<BCADGraphCell> getCellList(CADShapeEnum cse)
	{
		CADExplorer exp = CADShapeFactory.getFactory().newExplorer();
		Collection<BCADGraphCell> ret = new LinkedHashSet<BCADGraphCell>();
		for (exp.init(root.getShape(), cse); exp.more(); exp.next())
		{
			CADShape s = exp.current();
			BCADGraphCell c = cadShapeToGraphCell.get(s);
			if (ret.contains(c))
				continue;
			ret.add(c);
		}
		return ret;
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	public final void printShapes()
	{
		System.out.println("List of geometrical entities");
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			printShapes(cse, root.shapesExplorer(cse));
		}
		System.out.println("End list");
	}

	private static void printShapes(CADShapeEnum cse, Iterator<BCADGraphCell> it)
	{
		while (it.hasNext())
		{
			BCADGraphCell s = it.next();
			if (cse == CADShapeEnum.VERTEX)
			{
				CADVertex v = (CADVertex) s.getShape();
				double [] coord = v.pnt();
				System.out.println("Shape "+s+ " ("+coord[0]+", "+coord[1]+", "+coord[2]+")");
			}
			else
			{
				System.out.println("Shape "+s+" ("+Integer.toHexString(s.hashCode())+"):");
				for (Iterator<BCADGraphCell> it2 = s.allShapesIterator(); it2.hasNext(); )
				{
					BCADGraphCell c = it2.next();
					System.out.println(" +> shape "+c+" ("+Integer.toHexString(c.hashCode())+")");
				}
			}
		}
	}

}
