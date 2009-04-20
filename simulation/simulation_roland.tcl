#bla bla
set ns [new Simulator]
set nf [open out.nam w]
$ns namtrace-all $nf

#set colors [list Blue Red Green Pink Orange]
#for {set i 1} {i < [llength $colors]} {incr i} {
#}
$ns color 0 Blue
$ns color 1 Red
$ns color 2 Green
$ns color 3 Pink
$ns color 4 Orange

proc finish {} {
	global ns nf
	$ns flush-trace
	close $nf
#	exec python analyze.py out.nam plot.dat 0
#	exec gnuplot plot.dat &
#	exec nam out.nam &
	exit 0
}

#------------------ Nodes erstellen ------------------
set server [$ns node]
set router [$ns node]
set client [list [$ns node] [$ns node] [$ns node] [$ns node] [$ns node]]

#------------------ verbindungen erstellen ------------------
$ns duplex-link	 $router  $server 0.1Mb 100ms DropTail
$ns duplex-link-op $router  $server orient right
$ns duplex-link-op $router  $server queuePos 0.5

#Fehlerhafte verbindung:
set lossModel [new ErrorModel]
$lossModel set rate_ 0.1
$ns link-lossmodel $lossModel $router $server

set orient {{down} {right-down} {right} {right-up} {up}}
for {set i 0} {$i < [llength $client]} {incr i} {
	$ns duplex-link [lindex $client $i] $router 0.1Mb 100ms DropTail
	$ns duplex-link-op [lindex $client $i] $router orient [lindex $orient $i]
	#$ns duplex-link-op $client1 $router queuePos 1
}


#------------------ Agents erstellen ------------------

set sink [list [new Agent/TCPSink] [new Agent/TCPSink] [new Agent/TCPSink] [new Agent/TCPSink] [new Agent/TCPSink]]
set agent [list [new Agent/TCP] [new Agent/TCP] [new Agent/TCP] [new Agent/TCP] [new Agent/TCP]]
set app [list [new Application/FTP] [new Application/FTP] [new Application/FTP] [new Application/FTP] [new Application/FTP]]
for {set i 0} {$i < [llength $client]} {incr i} {
	$ns attach-agent [lindex $client $i] [lindex $sink $i]
	[lindex $agent $i] set fid_ [expr {$i + 1}]
	$ns attach-agent $server [lindex $agent $i]
	[lindex $app $i] attach-agent [lindex $agent $i]
	$ns connect [lindex $agent $i] [lindex $sink $i]

	#Events
	$ns at [expr {10 * ($i + 1)}] [format {[lindex $app %d] start} $i]
	$ns at 490.0 [format {[lindex $app %d] stop} $i]
}

$ns at 500.0 "finish"


#------------------ Und ab ------------------
$ns run
