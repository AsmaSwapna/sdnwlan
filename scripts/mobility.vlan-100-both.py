#!/usr/bin/python

"""
sdnWLAN Mobility script - modified from mininet mobility sample.

This script sets up a 5 switch network with 3 hosts to simulate Kevin Schneider's
Roaming Demo slide.

For AP # 1 and it's gateway, both will send traffic into the SDN single-tagged with
VLAN 100.

The 5 switches are as follows:

    AP #1    - A WIFI device. Modeled here as a switch that Host 1/2 can
               attach to with one fixed port connected to SDN SW A.
    AP #2    - A WIFI device. Modeled here as a switch that Host 1/2 can
               attach to with one fixed port connected to SDN SW B.
    SDN SW A - Connected to AP #1, SDN SW B, and Router.  Connection to router handles
               all the 192.168.0.0 traffic
    SDN SW B - Connected to AP #2, SDN SW A, and Router.  Connection to router handles
               all the 192.168.0.1 traffic
    Router   - Has two ports connected to the SDN switches and one to the video server

The 3 Hosts are as follows:

    Host #1 - Runs on the 192.168.0.0/24 network
    Host #2 - Runs on the 192.168.0.1/24 network
    Server  -

o Modify the CONTROLLER_IP address below for your OpenFlow controller configuration

o Modify the DEBUG_FLOWS flag below (to True) if you want to control debug output and
  require manual input before performing any 'pingall's or host moves.

"""

from mininet.net import Mininet
from mininet.node import OVSSwitch, Host
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel, info, output, warn
import time

CONTROLLER_IP ='10.0.3.27'      # Home

DEBUG_FLOWS   = True            # True/False

AP1_VLAN = 100
AP2_VLAN = 200

class VLANHost( Host ):
    "Host connected to VLAN interface"

    def config( self, vlan=100, **params ):
        """Configure VLANHost according to (optional) parameters:
           vlan: VLAN ID for default interface"""

        r = super( VLANHost, self ).config( **params )

        intf = self.defaultIntf()
        # remove IP from default, "physical" interface
        self.cmd( 'ifconfig %s inet 0' % intf )
        # create VLAN interface
        self.cmd( 'vconfig add %s %d' % ( intf, vlan ) )
        # assign the host's IP to the VLAN interface
        self.cmd( 'ifconfig %s.%d inet %s' % ( intf, vlan, params['ip'] ) )
        # update the intf name and host's intf map
        newName = '%s.%d' % ( intf, vlan )
        # update the (Mininet) interface to refer to VLAN interface name
        intf.name = newName
        # add VLAN interface to host's name to intf map
        self.nameToIntf[ newName ] = intf

        return r

class MobilitySwitch( OVSSwitch ):
    "Switch that can reattach and rename interfaces"

    def delIntf( self, intf ):
        "Remove (and detach) an interface"
        port = self.ports[ intf ]
        del self.ports[ intf ]
        del self.intfs[ port ]
        del self.nameToIntf[ intf.name ]

    def addIntf( self, intf, rename=False, **kwargs ):
        "Add (and reparent) an interface"
        OVSSwitch.addIntf( self, intf, **kwargs )
        intf.node = self
        if rename:
            self.renameIntf( intf )

    def attach( self, intf ):
        "Attach an interface and set its port"
        port = self.ports[ intf ]
        if port:
            #if self.isOldOVS():
            #    self.cmd( 'ovs-vsctl add-port', self, intf )
            #else:
            self.cmd( 'ovs-vsctl add-port', self, intf,
                      '-- set Interface', intf,
                      'ofport_request=%s' % port )
            self.validatePort( intf )

    def validatePort( self, intf ):
        "Validate intf's OF port number"
        ofport = int( self.cmd( 'ovs-vsctl get Interface', intf,
                                'ofport' ) )
        if ofport != self.ports[ intf ]:
            warn( 'WARNING: ofport for', intf, 'is actually', ofport,
                  '\n' )

    def renameIntf( self, intf, newname='' ):
        "Rename an interface (to its canonical name)"
        intf.ifconfig( 'down' )
        if not newname:
            newname = '%s-eth%d' % ( self.name, self.ports[ intf ] )
        intf.cmd( 'ip link set', intf, 'name', newname )
        del self.nameToIntf[ intf.name ]
        intf.name = newname
        self.nameToIntf[ intf.name ] = intf
        intf.ifconfig( 'up' )

    def moveIntf( self, intf, switch, port=None, rename=True ):
        "Move one of our interfaces to another switch"
        self.detach( intf )
        self.delIntf( intf )
        switch.addIntf( intf, port=port, rename=rename )
        switch.attach( intf )

def printConnections( switches ):
    if DEBUG_FLOWS:
        "Compactly print connected nodes to each switch"
        for sw in switches:
            output( '%s: ' % sw )
            for intf in sw.intfList():
                link = intf.link
                if link:
                    intf1, intf2 = link.intf1, link.intf2
                    remote = intf1 if intf1.node != sw else intf2
                    output( '%s(%s) ' % ( remote.node, sw.ports[ intf ] ) )
            output( '\n' )

def moveHost( host, oldSwitch, newSwitch, newPort=None ):
    "Move a host from old switch to new switch"
    hintf, sintf = host.connectionsTo( oldSwitch )[ 0 ]
    oldSwitch.moveIntf( sintf, newSwitch, port=newPort )
    return hintf, sintf

def pause(secs):
    for tick in range(secs):
        print('.'),
        time.sleep(1)
    print ''

def mobilityTest():
    net = Mininet( topo=None, switch=MobilitySwitch, build=False)

    # Create mobile nodes

    h1 = net.addHost( 'h1', cls=VLANHost, vlan=AP1_VLAN, mac='00:00:00:00:00:02', ip='192.168.0.2/24')

    # Create 'video' server.

    video = net.addHost( 'video', cls=VLANHost, vlan=AP1_VLAN, mac='00:00:00:00:00:01', ip='192.168.0.1/24')

    # Create AP and SDN switches
    ap1    = net.addSwitch( 'ap1', listenPort=6634, mac='00:a1:a1:a1:a1:a1', dpid='a1a1a1a1a1a1a1a1')
    ap2    = net.addSwitch( 'ap2', listenPort=6634, mac='00:a2:a2:a2:a2:a2', dpid='a2a2a2a2a2a2a2a2')
    sdnA   = net.addSwitch( 'sdnA', listenPort=6634, mac='00:0A:0A:0A:0A:0A', dpid='0A0A0A0A0A0A0A0A')
    sdnB   = net.addSwitch( 'sdnB', listenPort=6634, mac='00:0B:0B:0B:0B:0B',  dpid='0B0B0B0B0B0B0B0B')
    router = net.addSwitch( 'router', listenPort=6634, mac='00:05:05:05:05:05', dpid='0505050505050505')

    print "*** Creating links"
    net.addLink(h1, ap1)

    net.addLink(ap1, sdnA, port1 = 2, port2=2)
    net.addLink(ap2, sdnB, port1 = 2, port2=2)
    net.addLink(sdnB, sdnA)

    net.addLink(sdnA, router)
    net.addLink(sdnB, router)
    net.addLink(video, router)

    # Add Controllers
    ctrl = net.addController( 'c0', controller=RemoteController, ip=CONTROLLER_IP, port=6633)

    net.build()

    # Connect switches to controller and/or perform ovs-ofctl commands to set up hard coded flows
    ap1.start( [ctrl] )
    ap2.start( [ctrl] )
    sdnA.start( [ctrl] )
    sdnB.start( [ctrl] )
    router.start( [ctrl] )

    ################################################################################################

    if DEBUG_FLOWS:
        print '* Initial network (before pingall):'
        printConnections( net.switches )
        _dummy = raw_input("\nInitial network setup.  Press [RETURN] for first 'pingall' command")
    # PingAll
    net.pingAll()

    if DEBUG_FLOWS:
        time.sleep(1)
        print '\n* Initial network (after first pingall):'
        printConnections( net.switches )

    ################################################################################################
    # Pause to allow UI verification

    if DEBUG_FLOWS:
        _dummy = raw_input("\nPress [RETURN] to migrate hosts")
    else:
        print 'Sleep 5 seconds before moving both hosts'
        pause(5)

    ################################################################################################

    print 'Moving host(s) to other AP'
    moveHost(h1, ap1, ap2, newPort=1)

    if DEBUG_FLOWS:
        _dummy = raw_input("\nHosts moved.  Press [RETURN] for next 'pingall' command")
    else:
        print 'Pinging again in 5 seconds'
        pause(5)

    net.pingAll()

    if DEBUG_FLOWS:
        _dummy = raw_input("\nPress [RETURN] to migrate host(s) back to original AP")
    else:
        print 'Sleep 5 seconds before moving both host(s) back to original AP'
        pause(5)

    print 'Moving host(s) back to their Home-AP'
    moveHost(h1, ap2, ap1, newPort=1)

    if DEBUG_FLOWS:
        _dummy = raw_input("\nHosts moved back to original home access point.  Press [RETURN] for next 'pingall' command")
    else:
        print 'Pinging again in 5 seconds'
        pause(5)

    net.pingAll()

    print('Done.  Use CLI for additional commands...')
    CLI( net )
    net.stop()


if __name__ == '__main__':
    setLogLevel( 'info' )
    mobilityTest()