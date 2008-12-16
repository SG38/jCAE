
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Triangle
from org.jcae.mesh.amibe.validation import *
from org.jcae.mesh.xmldata import MeshReader, MeshExporter
from gnu.trove import TIntHashSet

# Java
from java.lang import Class

# Python
import sys, os
from optparse import OptionParser
import jarray

"""
   Sample class to print quality statistics about an amibe mesh.
   Example: to print histogram about minimum angles:
    amibebatch report -c MinAngleFace -s .0174532925199432957 -b 6,12,18,24,30,36,42,48,54
"""

def list_criteria(option, opt, value, parser):
	listStr = QualityProcedure.getListSubClasses()
	print("List of available criteria for -c option:")
	while len(listStr) > 0:
		print " ", listStr.pop(0)
		print "   ", listStr.pop(0)
	sys.exit(0)

parser = OptionParser(usage="amibebatch report [OPTIONS] <dir>\n\nPrint statistics about mesh quality", prog="report")
parser.add_option("-b", "--bounds", metavar="LIST",
                  action="store", type="string", dest="bounds",
                  help="comma separated list of values, implies -H (default: 0.2,0.4,0.6,0.8)")
parser.add_option("-H", "--histogram", action="store_true", dest="histogram",
                  help="prints histogram")
parser.add_option("-d", "--detailed", action="store_true", dest="detailed",
                  help="reports statistics by face")
parser.add_option("-f", "--from-face", metavar="NUMBER",
                  action="store", type="int", dest="ifacemin",
                  help="meshing had been started from this patch number")
parser.add_option("-o", "--output", metavar="BASE",
                  action="store", type="string", dest="outBasename",
                  help="creates <BASE>.mesh and <BASE>.bb MEDIT files")
parser.add_option("-c", "--criterion", metavar="CLASS",
                  action="store", type="string", dest="crit",
                  help="criterion (default: MinAngleFace)")
parser.add_option("-C", "--list-criteria", action="callback", callback=list_criteria,
                  help="lists all available criteria")
parser.add_option("-s", "--scale", metavar="NUMBER",
                  action="store", type="float", dest="scaleFactor",
                  help="scale factor (default: 1.0)")
parser.set_defaults(crit="MinAngleFace", scaleFactor=1.0, ifacemin=1)

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
if options.bounds:
	strBounds = options.bounds
	options.histogram = True
else:
	strBounds = "0.2,0.4,0.6,0.8"

bounds = [float(i) for i in strBounds.split(",")]          
qprocFactory = QualityProcedureFactory("org.jcae.mesh.amibe.validation."+options.crit)
qproc = qprocFactory.buildQualityProcedure()
mesh = qprocFactory.buildMesh()
MeshReader.readObject3D(mesh, xmlDir)
# Compute mesh quality
nrFaces = 1
if options.detailed:
	groups = TIntHashSet(mesh.getTriangles().size())
	for f in mesh.getTriangles():
		if f.isWritable():
			i = f.getGroupId() + 1 - options.ifacemin
			if i >= 0:
				groups.add(i)
	nrFaces = groups.size()
mean = mesh.getTriangles().size() / nrFaces
data = jarray.zeros(nrFaces, QualityFloat)
for i in xrange(len(data)):
	data[i] = QualityFloat(mean)
	data[i].setQualityProcedure(qproc)
	data[i].setTarget(options.scaleFactor)
for f in mesh.getTriangles():
	if f.isWritable():
		i = f.getGroupId() + 1 - options.ifacemin
		if i < 0 or not options.detailed:
			i = 0
		data[i].compute(f)

for i in xrange(len(data)):
	data[i].finish()
	if options.detailed:
		print("Face "+str(i+1))
	if options.histogram:
		# Prints histogram on console
		data[i].split(bounds)
		data[i].printLayers()
	else:
		data[i].printStatistics()

if None != options.outBasename:
	# Prints triangle quality into a .bb file to be displayed by MEDIT
	if options.detailed:
		ids = jarray.zeros(1, "i")
		for i in xrange(len(data)):
			ids[0] = i + options.ifacemin - 1
			data[i].printMeshBB(options.outBasename+"-"+ids[0]+".bb")
			MeshExporter.MESH(File(xmlDir), ids).write(options.outBasename+"-"+ids[0]+".mesh")
	else:
		data[0].printMeshBB(options.outBasename+".bb")
		MeshExporter.MESH(xmlDir).write(options.outBasename+".mesh")
