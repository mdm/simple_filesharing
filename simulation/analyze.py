import sys
import os

fin = open(sys.argv[1], "r")
datafile = sys.argv[2] + ".data"
fout = open(datafile, "w")
try:
	smoothness = int(sys.argv[3])
except IndexError:
	smoothness = 1

data = []
for i in range(5):
	data.append([])

for line in fin.readlines():
	tokens = line.split(" ")
	if (tokens[0] == "r") and (tokens[6] == tokens[16]):
		data[int(tokens[16])].append((float(tokens[2]), int(tokens[10])))

total_bps = [] 
for i in range(5):
	bits = 0
	for j in data[i]:
		bits += j[1] * 8
	total_bps.append(bits / (490 - 10.0 * (i + 1)))

for i in range(5):
	fout.write("# client %d\n" % (i,)) 
	if (smoothness == 0):
		last = 0.0
		for j in data[i]:
			fout.write("%f %f\n" % (j[0], 8 * j[1] / (j[0] - last)))
			last = j[0]
	else:
		t = 0
		for j in range(500/smoothness):
			bps = 0
			sec  = 1.0 * smoothness * (j + 1)
			try:
				while (data[i][t][0] < sec):
					bps += data[i][t][1] * 8
					t += 1
			except IndexError:
				bps = 0
			fout.write("%f %f\n" % (sec, bps * 1.0 / smoothness))
	fout.write("\n\n")

gp = os.popen("gnuplot", "w")
print >> gp, "set terminal postscript color"
print >> gp, "set output \"plots_%s_%d.ps\"" % (sys.argv[2], smoothness)
print >> gp, "set xlabel \"Simulation time [s]\""
print >> gp, "set ylabel \"Throughput [bps]\""
print >> gp, "set xrange [] writeback"
print >> gp, "set yrange [] writeback"
#print >> gp, "set boxwidth 1.0 absolute"
for i in range(5):
	print >> gp, "set title \"Stream Analysis for Client %d\"" % (i,)
	if (smoothness == 0):
		title = "raw"
	else:
		title = "resampled (interval: %d s)" % (smoothness)
	print >> gp, "plot \"%s\" index %d title \"%s\" with boxes fs solid, %f title \"total (%.2f bps)\" with lines" % (datafile, i, title, total_bps[i], total_bps[i])
	print >> gp, "set xrange restore"
	print >> gp, "set yrange restore"

