/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.cad.BrepNode;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.BoraToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableMesh;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.cookies.ViewCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;

/**
 * Contains the submeshnode(s)
 */
public class MeshNode extends DataNode implements ViewCookie
{
	private AbstractNode subMeshNode;
	
	public MeshNode(DataObject arg0)
	{
		super(arg0, new Children.Array());
		setIconBaseWithExtension("org/jcae/netbeans/mesh/MeshNode.png");
		getCookieSet().add(this);
		updateSubmeshNode();
		refreshGroups();
	}


//	protected Property[] getExpertProperties()
//	{
//		try
//		{
//			return new Property[]{
//				new BeanProperty(getBModel(), "geometryFile"),
//				new BeanProperty(getBModel(), "meshFile"),
//				new BeanProperty(getBModel(), "boraFile")
//			};
//		}
//		catch (Exception e)
//		{
//			ErrorManager.getDefault().notify(e);
//			return new Property[0];
//		}
//	}
//
//	@Override
//	public PropertySet[] getPropertySets() {
//		return new PropertySet[]{
//					new PropertySet() {
//
//						public Property[] getProperties() {
//							return MeshNode.this.getExpertProperties();
//						}
//
//						@Override
//						public String getName() {
//							return "Expert";
//						}
//
//						@Override
//						public boolean isExpert() {
//							return true;
//						}
//					}
//				};
//	}

	public void view() {
		if (subMeshNode != null && getBModel() != null) {
			SubmeshNode sNode = (SubmeshNode) subMeshNode;
			sNode.viewMesh();
		}
	}

	public static void view(String vName, Map<String, Collection<BDiscretization>> meshData) {
		if (meshData == null || meshData.isEmpty())
			return;
		View v = ViewManager.getDefault().getCurrentView();
		BoraToMesh toMesh = new BoraToMesh(meshData);
		ViewableMesh vMesh = new ViewableMesh(toMesh.getMesh());
		vMesh.setName(vName);
		v.add(vMesh);
	}

	/**
	 * @return Returns the groups
	 */
	public void refreshGroups()
	{
		SubmeshNode subNode = (SubmeshNode)subMeshNode;
		if (subNode != null)
			subNode.refreshGroupsNode(true);
	}

	public BModel getBModel() {
		return getCookie(MeshDataObject.class).getBModel();
	}

	public BModel getBModel(String geomFile) {
		return getCookie(MeshDataObject.class).getBModel(geomFile);
	}
	
	@Override
	public String getHtmlDisplayName() {
		return "<font color='0000FF'>" + getName() + "</font>";
	}

	@Override
	public void setName(String arg0)
	{	
		try
		{
			String o=getName();
			getDataObject().rename(arg0);
			if (getBModel() != null) {
				getCookie(MeshDataObject.class).updateBModelDir();
			}
			fireDisplayNameChange(o, arg0);
			fireNameChange(o, arg0);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}
	
	@Override
	public String getName()
	{
		return getDataObject().getName();
	}
	
	@Override
	protected void createPasteTypes(Transferable t, List<PasteType> ls)
	{
		final Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1) {
			final BrepNode n=ns[0].getCookie(BrepNode.class);
			if (n != null) {
				n.save();
				ls.add(new PasteType() {

					public Transferable paste() {
						getBModel(getGeomFile(n)).newMesh();
						getBModel().save();
						updateSubmeshNode();
						firePropertyChange(null, null, null);
						return null;
					}
				});
			}
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}

	/**
	 * @param n the BrepNode of the geometry
	 * @return the absolute path of a file (ex : /home/toto/Geom.brep)
	 */
	private static String getGeomFile(BrepNode n) {
		String ref = FileUtil.toFile(n.getDataObject().getPrimaryFile().getParent()).getPath();
		String nameExt = n.getDataObject().getPrimaryFile().getNameExt();
		return Utilities.absoluteFileName(nameExt, ref);
	}
	
	private void updateSubmeshNode()
	{
		if(subMeshNode!=null) {
			getChildren().remove(new Node[]{subMeshNode});
			try {
				subMeshNode.destroy();
			}
			catch (IOException io) {
				io.printStackTrace();
			}
		}
		if (getBModel() != null) {
			subMeshNode = new SubmeshNode(getCADName(getBModel().getCADFile()), getBModel());
			getChildren().add(new Node[] { subMeshNode } );
		}
			
	}

	private static String getCADName(String cadFile) {
		return cadFile.substring(cadFile.lastIndexOf(File.separator) + 1, cadFile.lastIndexOf("."));
	}
		
	@Override
	public Action getPreferredAction()
	{
		return SystemAction.get(PropertiesAction.class);
	}
	
	@Override
	public Action[] getActions(boolean b)
	{
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(ComputeMeshAction.class));
		l.add(SystemAction.get(ViewAction.class));
		l.add(SystemAction.get(DeleteAction.class));
		l.add(SystemAction.get(RenameAction.class));
		return l.toArray(new Action[l.size()]);
	}	
}