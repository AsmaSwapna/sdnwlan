=============================================================================
===                                                                       ===
===                           sdnWLAN POC Application                     ===
===                                                                       ===
=============================================================================
===  Revision History                                                     ===
=============================================================================
Version: 2.0.91-SNAPSHOT
   Date: 3/19/2016
Changes: o Cleanup of package names (convert to net.bcsw)
         o Update to build under 1.6.0-SNAPSHOT of ONOS
         o First push to github repository.  See commit notes for any further
           revision information after this initial commit.

-----------------------------------------------------------------------------
Version: 1.0.1-SNAPSHOT
   Date: 9/16/2015
Changes: o Worked on cli (one simple command, no auto-complete yet...)
         o Worked on REST interface (GET list of APs, POST & DELETE not tested)

-----------------------------------------------------------------------------

Version: 1.0.0-SNAPSHOT          (also coded in pom.xml version property)
   Date: 9/15/2015
Changes: o Initial checking of simple framework.
         o Stubs for CLI, Config File, and REST provisioning added
         o Tested 'config file' skeleton and it worked
         o CLI not wired in completely (TODO)
         o Have not tested REST commands yet

=============================================================================
===  Building and Running                                                 ===
=============================================================================
This section outlines how to build, install, activate, and test this ONOS
application. These should always apply to the latest checked-in code.  If
You need to run with an older configuration, please check the revision
history in perforce.

----------------------------------------------------------------------
Building
--------

Open a terminal and change your directory to the ROOT location for this
application (this is where this file is located) and enter the following
command:

   $ mvn clean install                 ( or the shortcut: mci )

----------------------------------------------------------------------
Installing
----------

The best way to install and test the application is to set up a cell file
in your ${ONOS_ROOT}/tools/test/cells directory and use the 'cell' command
to source the file.  For this example, create a file name 'wvlan-poc' in
that directory with the following contents (change the IP addresses and any
username/group as is appropriate).

    ################################################################
    # BCSW sdnWLAN POC test cell

    ONOS_CELL=sdnwlan-poc

    # the address of the VM to install the package onto
    export OC1="192.168.0.14"

    # the default address used by ONOS utilities when none are supplied
    export OCI="192.168.0.14"

    # the ONOS apps to load at startup
    export ONOS_APPS="openflow,proxyarp,gui"

    # pattern to specify which addr to use for inter-ONOS node communication
    #(not used with single-instance core)
    export ONOS_NIC="192.168.0.*"

    export ONOS_USER=stack
    export ONOS_GROUP=stack
    #
    ################################################################

Save the file and then issue the following command:

    $ cell sdnwlan-poc

Once that is set, each time you build and want to install the new image, issue
the following command:

    $ onos-app $OC1 install target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar
or
    $ onos-app $OC1 reinstall target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar

    (replace the version number as needed)

If you wish to install and immediately activate the application, issue the
following command (exclamation point after install/reinstall command) :

    $ onos-app $OC1 install! target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar
or
    $ onos-app $OC1 reinstall! target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar

----------------------------------------------------------------------
Running
-------

If you have a JSON configuration file, you may need to manually copy it to
your ONOS machine to the following directory:  /opt/onos/config

Once you have installed, you may need to start the by issuing the following
commands:

    $ onos $OC1
    ...
    onos> app activate net.bcsw.sdnwlan
    onos> ^D    (to exit)

Once the application is running, start up the mininet script (make sure you
set the OpenFlow controller IP address to point to your ONOS installation) by
issuing the following command:

   $ sudo python ./mobility.py

----------------------------------------------------------------------
REST API
-------
The REST API is rooted at /bcsw/sdnwlan and currently supports listing (GET) of
configured Access Points. Until a unique identifier for an AP is defined, the
creation or selective listing of an Access Point is not yet supported. Some
sample code is written as a placeholder but it has not been tested at all.

To list all sdnWLAN Access Points, use the following curl command:

    $ curl --user onos:rocks  sdan-onos:8181/bcsw/sdnwlan/list

=============================================================================
===  Debugging                                                            ===
=============================================================================

For more information please refer to:
  https://wiki.onosproject.org/display/ONOS/Debugging+ONOS+with+IntelliJ+IDEA

You may also want to log into your machine/VM with ONOS and modify the
/opt/onos/options file and make sure 'debug' is listed on the line with ONOS_OPTS

=============================================================================
=============================================================================
=============================================================================
Notes after this point are deprecated and will eventually be cleaned up

TODO: Add information on how to build and deploy

TODO: Look into how to obfuscate the code (via mvn) if this will not be open sourced

TODO: Update comment headers of all files with appropriate license information

================================

Initial idea for objects/interfaces:

AP Manager - manages list of AP objects
             REST interface for configuration (future)
             Config file reader/writer.  For first demo and easy way to seed if no REST is available
             Backs info to persistent storage and for cluster sharing

AP Impl - An individual AP
          List of CIDR's that this AP manages.  Fixed default GW?
          List of mobile hosts
          NB/SB flow provider (which to use)
          Geo coordinates.  For UI and possibly for range calculations to limit pre-created flows

Mobile Host - An individual host
              Ref back to home AP
              Ref to current AP
              Stats
              Ref to NB/SB flow

NB / SB Flow - Interface from which specific Imples are created.  Mainly for test purpose
               so we can swap out various impls for testing.
               Should have stats interface. Rx/Tx, active time?, ...

CLI - A cli interface for configuring would be good.
      Can we tie into the wipe-out command for quick scrubbing

REST interface - For as much as makes sense.
Config file - JSON file.  Useful for demo/test purposes
Properties - A few global values may be needed but would be nice to tweak
             Default NB/SB flow to use
             Poll or timeout value defaults
             Enable/disable/control range calculating for pre-create of flows