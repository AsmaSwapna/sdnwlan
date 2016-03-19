/*
 * Copyright 2015-2016 Boling Consulting Solutions, bcsw.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bcsw.sdnwlan;

import net.bcsw.sdnwlan.config.AccessPointConfig;
import net.bcsw.sdnwlan.config.SDNWLANConfig;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.*;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Timer;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Skeletal BCSW sdnWLAN Host Mobility component.
 *
 * This class provides the overall management for the BCSW sdnWLAN SDN Application.
 */
@Component(immediate = true)
@Service
public class APManager implements SDNWLANService {

    public static final String APP_NAME = "net.bcsw.sdnwlan";

    // TODO: Do we want to make the AP Manager a device provider?
    //private static final ProviderId PID = new ProviderId("of", "net.bcsw.sdnwlan", true);

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private final InternalConfigListener cfgListener = new InternalConfigListener();

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, SDNWLANConfig>(APP_SUBJECT_FACTORY,
                    SDNWLANConfig.class,
                    "sdnwlan") {
                @Override
                public SDNWLANConfig createConfig() {
                    return new SDNWLANConfig();
                }
            }
    );
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    static protected ApplicationId  appId;
    private HostListener   hostListener;
    private DeviceListener deviceListener;
    protected Timeout      deletedHostTimeout;

    // List of Access Points
    private Map<MacAddress, AccessPoint> accessPoints =  Maps.newConcurrentMap();

    // Set of default gateways being monitored.
    private Map<IpAddress, DefaultGateway> defaultGateways = Maps.newConcurrentMap();

    protected static int gatewayFlowPriority    = SDNWLANConfig.DEFAULT_GATEWAY_FLOW_PRIORITY;
    protected static int roamingUnicastPriority = SDNWLANConfig.DEFAULT_ROAMING_UNICAST_FLOW_PRIORITY;
    protected static int hostRemovedTimeout     = SDNWLANConfig.DEFAULT_REMOVED_HOST_TIMEOUT_SECONDS;
    protected static int removeDelay            = SDNWLANConfig.DEFAULT_REMOVED_HOST_DELAY_GRANULARITY;

    // TODO: Useful stats would be nice.  Separate interface/class?

    // TODO: Should support a REST interface
    // TODO: Should save information to persistent storage for restart purposes
    // TODO: Should support a clustered environment for scalability
    // TODO: For DefaultGateways, store last known location (MacAddress, Location, VIDs) for faster initial flow setup on restart

    /**
     * Activate the sdnWLAN Service
     *
     *
     * 1. Restore information from the persistent storage location (may be empty)
     *
     * 2. Register any appropriate host and device listeners
     *
     * 3. Read in any configuration file.  Using a unique identifier for each item
     *    being restored (say MAC address for an Access Point), updateConfig any existing
     *    data (perhaps from step 1) with this data.
     *
     *    For this purposes, it is suggested that most AP/host information in a
     *    configuration file be empty for an actual deployment.
     *
     * 4. Walk existing topology and look for any new access points or hosts of
     *    interest.  Devices are of greatest importance since we do not know if
     *    a host in the topology is a mobile host until we have read its access
     *    point (device) in.
     *
     *    During this period, hosts and device events may occur.  The fun here will
     *    be experimenting and figuring out the best thing to do.
     *
     * 5. We are initialized.  Treat host/device events appropriately.  Hopefully
     *    few Access Point device add/remove events will occur compared to mobile
     *    host even
     */
    @Activate
    protected void activate() {

        appId = coreService.registerApplication(APP_NAME);

        log.info("Starting");

        // Handle application configuration

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        cfgListener.reconfigureNetwork(cfgService.getConfig(appId, SDNWLANConfig.class));

        // Create host and device listeners

        hostListener   = new InternalHostListener();
        deviceListener = new InternalDeviceListener();

        // 1. Restore data from persistent storage.  This useful for restoring
        //    state after a restart/deactivation
        // TODO implement this.  Make sure it works within a cluster environment

        // 2. Start listening for host and device events

        hostService.addListener(hostListener);
        deviceService.addListener(deviceListener);

        deletedHostTimeout = Timer.getTimer().newTimeout(new PurgeHostTask(), removeDelay, TimeUnit.SECONDS);

        // 5.  We are now active

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopping");

        // Cancel any running timers

        deletedHostTimeout.cancel();

        // Remove listeners

        cfgService.removeListener(cfgListener);
        hostService.removeListener(hostListener);
        deviceService.removeListener(deviceListener);

        factories.forEach(cfgService::unregisterConfigFactory);

        // Stop monitoring any default gateways

        defaultGateways.keySet().forEach(ip -> hostService.stopMonitoringIp(ip));

        // Drop all flows on all APs

        accessPoints.values().forEach(ap -> ap.onShutdown());

        log.info("Stopped");
    }

    /**
     * Get a map of the current WLAN AccessPoints
     *
     * @return list of access points
     */
    @Override
    public Map<MacAddress, AccessPoint> getAccessPoints() {
        return Collections.unmodifiableMap(accessPoints);
    }

    /**
     * Get a map of the default gateways
     *
     * @return collection of default gateways
     */
    @Override
    public Map<IpAddress, DefaultGateway> getDefaultGateways() {
        return Collections.unmodifiableMap(defaultGateways);
    }

    /**
     * Creates a sdnWLAN Access Point
     *
     * Primarily here in case we want to be able to add Access Points via CLI or REST
     *
     * @param apMac
     * @return The new access point
     */
    @Override
    public AccessPoint createAccessPoint(MacAddress apMac) {
        log.info("createAccessPoint: {}", apMac.toString());
        // TODO Implement this (or delete the method)
        return null;
    }

    /**
     * Deletes a sdnWLAN Access Point
     *
     * Primarily here in case we want to be able to remove Access Points via CLI or REST
     *
     * @param accessPoint The access point to delete
     * @return true if the access point was found and removed from the system
     */
    @Override
    public boolean deleteAccessPoint(AccessPoint accessPoint) {

        log.info("deleteAccessPoint: {}", accessPoint.toString());

        // TODO Implement this (or delete the method)

        return false;
    }

    /**
     * Add an access point to the network.
     *
     *   An access point it the closest SDN connection to a Remote Gateway/Access Point.
     *
     * @param accessPoint  Access Point to add
     */
    public void onAddAccessPoint(AccessPoint accessPoint) {

        log.info("Add AccessPoint: {}", accessPoint.toString());

        if (accessPoints.put(accessPoint.getMacAddress(), accessPoint) != null) {
            log.error("Duplicate MAC Address: An existing AccessPoint for MAC Address {} already exists",
                    accessPoint.getMacAddress().toString());
            return;
        }
        // Set up a host monitor on the for the default gateway

        for (DefaultGateway gateway : accessPoint.getDefaultGateways().values()) {

            IpAddress ipAddr  = gateway.getGatewayConfig().getGatewayAndMask().gatewayAddress();
            boolean   addMonitor = false;

            synchronized (defaultGateways) {
                if (!defaultGateways.containsKey(ipAddr)) {
                    addMonitor = true;
                    defaultGateways.put(ipAddr, gateway);
                }
            }
            gateway.addAccessPoint(accessPoint);

            if (addMonitor) {
                log.info("Starting host monitor for Default Gateway IP Address: {}", ipAddr);
                hostService.startMonitoringIp(ipAddr);
            }
        }
        // Perform startup procedures

        accessPoint.onStartup();
    }

    /**
     * Remove an access point to the network.
     *
     *   An access point it the closest SDN connection to a Remote Gateway/Access Point.
     *
     * @param accessPoint  Access Point to add
     */
    public void onRemoveAccessPoint(AccessPoint accessPoint) {
        log.info("Remove AccessPoint: {}", accessPoint.toString());

        // Clean up default gateways

        List<DefaultGateway> dropList = Lists.newArrayList();

        defaultGateways.values().stream().filter(gw -> gw.getAccessPoints().contains(this)).forEach(gw -> {
            gw.removeAccessPoint(accessPoint);
            if (gw.getAccessPoints().isEmpty()) {
                dropList.add(gw);
            }
        });
        // TODO: look into concurrent map and see if we really ever need to syncronize our add/remove operations
        dropList.forEach(gw -> {
            synchronized (defaultGateways) {
                defaultGateways.remove(gw);
            }
            IpAddress ipAddr = gw.getGatewayConfig().getGatewayAndMask().gatewayAddress();

            log.info("Halting host monitor for Default Gateway IP Address: {}", ipAddr);
            hostService.stopMonitoringIp(ipAddr);
        });
    }

    /**
     * Remove a host completely from the system
     *
     * @param host Mobile host to remove
     */
    @Override
    public void purgeHost(MobileHost host) {
        log.info("purgeHost: {}", host.toString());

        accessPoints.values().forEach(ap -> ap.removeHost(host));
    }

    /**
     * Handle configuration change events
     */
    private class InternalConfigListener implements NetworkConfigListener {
        /**
         * Reconfigures the DHCP Server according to the configuration parameters passed.
         *
         * @param cfg configuration object
         */
        private void reconfigureNetwork(SDNWLANConfig cfg) {
            if (cfg == null) {
                return;
            }
            gatewayFlowPriority    = cfg.getDefaultGatewayFlowPriority();
            roamingUnicastPriority = cfg.getRoamingUnicastFlowPriority();
            hostRemovedTimeout     = cfg.getDefaultRemovedHostTimeout();

            // Walk new list of access points and updateConfig existing ones and then add new ones

            Map<MacAddress, AccessPointConfig> newPoints = cfg.getAccessPoints();

            // Remove any APs no longer configured. Use an immutable copy of the access point map
            // as the backing collections since we may be removing items from it.

            getAccessPoints().values().stream().filter(ap -> !newPoints.containsKey(ap.getMacAddress())).forEach(ap -> {
                onRemoveAccessPoint(ap);
            });
            // Update existing ones

            newPoints.values().stream().filter(newConfig -> accessPoints.containsKey(newConfig.getMacAddress())).forEach(newConfig -> {
                accessPoints.get(newConfig.getMacAddress()).updateConfig(newConfig);
            });
            // Then add any new ones

            newPoints.values().stream().filter(newConfig -> !accessPoints.containsKey(newConfig.getMacAddress())).forEach(newConfig -> {
                onAddAccessPoint(new AccessPoint(newConfig));
            });
        }

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                 event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                 event.configClass().equals(SDNWLANConfig.class)) {

                SDNWLANConfig cfg = cfgService.getConfig(appId, SDNWLANConfig.class);
                reconfigureNetwork(cfg);
                log.info("Reconfigured: {}", event.type().toString());
            }
            // TODO: Also may want to support NetworkConfigEvent.Type.CONFIG_REMOVED.  Investigate this.
        }
    }

    /**
     * Handle Host events. This application will monitor all default gateway hosts as well
     * as mobile hosts
     *
     * Cases to handle.    Host ADD/REMOVE/MOVE
     *                     Modified -> Location or MAC address
     *
     * The following is a list of observed events during various debug sessions besides the obvious
     * ones we expect to handle (add/remove/move)
     *
     * --- Initial Startup and Shutdown Related events ---
     *
     * If mininet topology is running when Application is started. Nothing is displayed until I did a
     * pingall, at that time I received:
     *
     *   o HOST_ADDED events.  Contained MAC addresses, vlans, ip, location, ...
     *
     * If application is started and then mininet is started: (Did not need to do ping all)
     *
     *   o Received HOST_ADDED messages (without IP) for what I think is the 'host' port of three of
     *     the fabric switches.  These show up as 'unknown' in the toplogy gui.  I think we need to
     *     ignore these.
     *
     *   o After doing a pingall -> HOST_ADDED events.  Contained MAC addresses, vlans, ip, location, ...
     *     just like before
     *
     * If Mininet is shutdown (but devices/hosts not 'removed') while application is running:
     *
     *   o Get a HOST_REMOVED event for each host.  The 'unknown' device hosts show as removed as
     *     well but have no IP address. The 'real' hosts in the topology have an IP address in the
     *     event message.
     *
     * --- Mobile Host Move events Related events ---
     *
     * If mininet up and running with host on 'home' Access Point.  Then move to a second access point
     * with the 'mobility host' application (may be slightly different than real life move).
     *
     *   o
     */
    public class InternalHostListener
            implements HostListener
    {
        /**
         * Handle Host events
         *
         * @param event  The event
         */
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
            case HOST_ADDED:
                onHostAdded(event.subject());
                break;

            case HOST_REMOVED:
                log.info("HOST_REMOVED {}", MobileHost.hostToString(event.subject()));
                onHostRemoved(event.subject());
                break;

            case HOST_UPDATED:
                log.info("HOST_UPDATED {}, Previous: {}", MobileHost.hostToString(event.subject()),
                        MobileHost.hostToString(event.prevSubject()));

                onHostUpdated(event.subject(), event.prevSubject());
                // TODO: Handle this, or not...
                //
                // Get this after move and a ping occurs.  The 'prevSubject' is identical but the prev
                // did not have a mac accress

                break;

            case HOST_MOVED:
                log.info("HOST_MOVED {}", MobileHost.hostToString(event.subject()));
                onHostMoved(event.subject());
                // TODO: Handle this, or not...
                break;
            //
            // We currently know we do not want to handle any of the following
            //
            //
            default:
                break;
            }
        }

        /**
         * Get any access point at the specified host network edge location
         * @param location Location to look up
         * @return Any associated access point or null
         */
        private AccessPoint getAccessPointByHostLocation(HostLocation location) {

            // TODO: We may want to support more than one AccessPoint at an edge location.  Change if needed.
            if (location.equals(HostLocation.NONE)) {
                return null;
            }
            for (AccessPoint accessPoint : accessPoints.values()) {
                if (accessPoint.getConnections().contains(location)) {
                    return accessPoint;
                }
            }
            return null;
        }
        /*
         * Get all access points whose default gateway handles the given IP Address(es)
         *
         * @param addrs Set of IP Address
         * @return Set of access points
         */
        private Set<AccessPoint> getAccessPointsBySubnet(Set<IpAddress> addrs) {

            Set<AccessPoint> points = Sets.newHashSet();

            accessPoints.values().forEach(accessPoint ->
                accessPoint.getDefaultGateways().keySet().forEach(cidr ->
                    addrs.forEach(ip -> {
                        if (cidr.contains(ip)) {
                            points.add(accessPoint);
                        }
                    })));

            // TODO: Currently we only support a single AccessPoint per subnet.
            // we will return all of them here but the rest of our logic uses only the first in the set

            return points;
        }

        /**
         * Search all AP's for an existing mobile host
         * @param host  Host to search for
         * @return Mobile host or null if not found
         */
        protected MobileHost getMobileHost(Host host) {

            MacAddress hostMac = host.mac();

            for (AccessPoint accessPoint : accessPoints.values()) {
                for (MobileHost apHost : accessPoint.getLocalHosts().values()) {
                    if (hostMac.equals(apHost.getMacAddress())) {
                        return apHost;
                    }
                }
            }
            return null;
        }

        /**
         * Called when a new host is discovered.  This may be any of the following
         *
         *  o A new mobile host
         *  o An existing mobile host that was removed (HOST_REMOVED) but not yet purged
         *  o A different kind of host (but not Default gateway) that we do not care about
         *
         * @param host
         */
        private void onMobileHostAdded(Host host) {
            AccessPoint      locationAP = getAccessPointByHostLocation(host.location());
            MobileHost       mobileHost = getMobileHost(host);
            Set<AccessPoint> homeAPs    = (mobileHost == null) ?
                                          getAccessPointsBySubnet(host.ipAddresses()) :
                                          mobileHost.getHomeAccessPoints();

            // Is the new host related to any of our access points and is the subnet handled?

            if ((locationAP != null) && (homeAPs.size() > 0)) {
                log.info("onHostAdded: Found Matching Access Point: {}", locationAP.toString());

                // Does a mobile host already exist?

                if (mobileHost != null) {

                    log.info("onHostAdded: {}, This was a host pending purge", MobileHost.hostToString(host));

                    // Stop any remove timeout purge for this host
                    mobileHost.setRemovedTime(0);
                    mobileHost.setCurrentAccessPoint(locationAP);
                }
                else {
                    // Is the add on the home AP or is the Host starting off roaming?

                    mobileHost = (homeAPs.contains(locationAP)) ?
                                 new MobileHost(host, homeAPs) :
                                 new MobileHost(host, homeAPs, locationAP);

                    // Add host to it's home Access Point.  The flows from this point are already set up
                    // (default gateway flows)

                    AccessPoint homeAP = homeAPs.iterator().next();

                    homeAP.addHost(mobileHost);

                    // If location is not home, set up roaming by adding to the new location
                    //
                    // TODO: Currently we only support one AccessPoint servicing a subnet.
                    // The addHost logic currently assumes this.  If this changes, we need to update
                    // much of the code related to roaming

                    if (locationAP != homeAP) {
                        locationAP.addHost(mobileHost);
                    }
                }
                // TODO: Handle roaming and returns to home
            } else {
                // Either (or both) locationAP or homeAP was null.  This may just some other host on
                // the network or attached to it.

                log.info("onHostAdded: Ignoring host, LocationAP is {}null, HomeAP set is {}sempty",
                        locationAP == null ? "" : "not ",
                        homeAPs.size() == 0 ? "" : "not ");

                if ((mobileHost != null) && (locationAP == null)) {
                    // Stop any remove timeout purge for this host
                    mobileHost.setRemovedTime(0);
                    mobileHost.setCurrentAccessPoint(null);
                }
            }
        }

        /**
         * Handle learning new information on a configured default gateway
         *
         *   This handles learning of a new default gateway or the change in an existing one
         *
         * @param host Host from event that relates to the gateway
         */
        private void onDefaultGatewayModified(Host host) {

            // A particular host may have more than one Default Gateway address assigned
            // to the MAC.

            for (Iterator<IpAddress> ipItor = host.ipAddresses().iterator(); ipItor.hasNext();) {
                DefaultGateway gateway = defaultGateways.get(ipItor.next());

                if (gateway != null) {
                    gateway.updateHost(host);
                }
            }
        }

        /**
         * A new host was discovered (or a previously deleted one came back)
         *
         * @param host Host information from event
         */
        private void onHostAdded(Host host) {

            log.info("HOST_ADDED: {}", MobileHost.hostToString(host));

            // Is it a known default gateway. Do by seeing if any host IPs are in common
            // with our set of known default gateways.

            if (!Collections.disjoint(host.ipAddresses(), defaultGateways.keySet())) {
                onDefaultGatewayModified(host);

            } else {
                onMobileHostAdded(host);
            }
        }

        /**
         * Host information has changed
         *
         * @param host Host information from event
         */
        private void onHostUpdated(Host host, Host previous) {

            log.info("onHostUpdated: {}, previous: {}",
                    MobileHost.hostToString(host), MobileHost.hostToString(previous));

            if (!Collections.disjoint(host.ipAddresses(), defaultGateways.keySet())) {
                onDefaultGatewayModified(host);

            } else {
                // Is it related to any of our access points.  This is where mobile hosts are associated

                AccessPoint accessPoint = getAccessPointByHostLocation(host.location());
                MobileHost  mobileHost  = getMobileHost(host);

                if (accessPoint != null) {
                    // TODO: Implement
                    // Look into this more closely.  Could be an IP Address change.
                    // What about host showing up on a different VLAN if we are running with ingress vlans?

                } else {
                    // TODO: Do 'other' search
                }
            }
            // TODO: Anything else?
        }

        /**
         * An existing host moved
         *
         * @param host Host information from event
         */
        private void onHostMoved(Host host) {

            log.info("onHostMoved: {}", MobileHost.hostToString(host));

            if (!Collections.disjoint(host.ipAddresses(), defaultGateways.keySet())) {
                onDefaultGatewayModified(host);

            } else {
                // Is it related to any of our access points.  This is where mobile hosts are associated

                AccessPoint accessPoint = getAccessPointByHostLocation(host.location());
                MobileHost  mobileHost  = getMobileHost(host);

                if (accessPoint != null) {

                    // Is this an existing mobile host?

                    if (mobileHost != null) {
                        mobileHost.setCurrentAccessPoint(accessPoint);
                    }
                    // TODO: match, do something here, it is a host we may need to monitor

                } else {
                    // TODO: Anything else?
                }
            }
            // TODO: Anything else?
        }

        /**
         * An existing host disappeard
         *
         * @param host Host information from event
         */
        private void onHostRemoved(Host host) {

            log.info("onHostRemoved: {}", MobileHost.hostToString(host));

            // Is it related to any of our access points.  This is where mobile hosts are associated

            MobileHost mobileHost = getMobileHost(host);

            if (host != null) {
                // Set the time this was removed.

                mobileHost.setRemovedTime();

                // Note that we do not tear down any flows or set the current access point.  This
                // will be done if the remove timeout occurs.  We may get an 'ADD_HOST' very soon
                // if this is actually just moving...
            }
            // TODO: Anything else?  What about gateways 'going' away?
            // May want to set up a timed event to flush out any gateway flows if they do and do not
            // come back within a reasonable time.
        }
    }

    /**
     * Handle device events.  The following is a list of observed events during various debug sessions
     * besides obvious ones of
     *
     * If mininet topology is running when Application is started.
     *
     *   o Get DEVICE_AVAILABILITY_CHANGED events for each switch to indicate that it has come online
     *   o Get some DEVICE_UPDATED events. These exclude availability events so currently these are not
     *     of much interest.
     *
     * If application is started and then mininet is started:
     *   o Get a DEVICE_ADDED event followed by several PORT_ADDED event for each switch in the topology.
     *
     * If Mininet is shutdown  (but devices/hosts not 'removed') while application is running:
     *   o Get a DEVICE_AVAILABILITY event with 'avalable' set to False
     *   o Get a PORT_UPDATED event for each port
     *   o NOTE that we 'do not' get a DEVICE_REMOVED event
     *
     * If run mn -c to remove mininet devices while application is running:
     *   o We 'do not' get a DEVICE_REMOVED event:
     *
     * If run 'wipe-out please' in the ONOS CLI while application is running:
     *   o We get a DEVICE_REMOVED event for each switch
     */
    public class InternalDeviceListener implements DeviceListener
    {
        /***
         * Handle a device event
         *
         * @param event The event
         */
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                    onDeviceAdded(event);
                    break;

                case DEVICE_REMOVED:
                    onDeviceRemoved(event);
                    break;

                case DEVICE_SUSPENDED:
                    log.info("DEVICE_SUSPENDED {}", event.subject().id().toString());
                    // TODO: Handle this, or not...
                    break;

                case DEVICE_AVAILABILITY_CHANGED:
                    log.info("DEVICE_AVAILABILITY_CHANGED: {} / {}",
                            event.subject().id().toString(),
                            deviceService.isAvailable(event.subject().id()) ? "true" : "false");
                    // TODO: Handle this, or not...
                    break;

                case PORT_ADDED:
                    log.info("PORT_ADDED: {}/{}", event.subject().id().toString(),
                            event.port().number().toLong());
                    // TODO: Handle this, or not...
                    break;

                case PORT_UPDATED:
                    log.info("PORT_UPDATED: {}/{}, enabled: {}", event.subject().id().toString(),
                            event.port().number().toLong(),
                            event.port().isEnabled() ? "True" : "False");
                    // TODO: Handle this, or not...  event.port -> number, isEnabled
                    break;

                case PORT_REMOVED:
                    log.info("PORT_REMOVED: {}/{}", event.subject().id().toString(),
                            event.port().number().toLong());
                    // TODO: Handle this, or not...
                    break;

                case PORT_STATS_UPDATED:
                    log.debug("PORT_STATS_UPDATED: {}", event.subject().id().toString());
                    // TODO: Handle this, or not...
                    break;

                // We currently know we do not want to handle any of the following.
                case DEVICE_UPDATED:
                    log.info("DEVICE_UPDATED: {}", event.subject().id().toString());
                    break;   // TODO: Comment out later once we know all events we will ignore

                default:
                    break;
            }
        }

        /**
         * A new device has shown up in our topology
         *
         * @param event Event information
         */
        private void onDeviceAdded(DeviceEvent event) {
            log.info("DEVICE_ADDED: {}", event.subject().id().toString());

            // TODO: Do anything?
        }

        /**
         * A new device has shown up in our topology
         *
         * @param event Event information
         */
        private void onDeviceRemoved(DeviceEvent event) {
            log.info("DEVICE_REMOVED: {}", event.subject().id().toString());

            // TODO: Do anything?
        }
    }

    /**
     * Timer task that is used to purge hosts that were the subject of HOST_REMOVED
     * events.  We pause a small amount of time before completely removing them since it
     * could be a host that left one AP and then showed up at another one shortly afterwards.
     */
    private class PurgeHostTask implements TimerTask {
        @Override
        public void run(Timeout to) {

            Date dateNow = new Date();

            for (AccessPoint accessPoint : accessPoints.values()) {
                for (MobileHost apHost : accessPoint.getLocalHosts().values()) {

                    long hostTime = apHost.getRemovedTime();

                    if ((hostTime != 0) &&
                            ((dateNow.getTime() - hostTime) > (hostRemovedTimeout * 1000))) {

                        // Remove the host...

                        purgeHost(apHost);

                        log.info("PurgeHostTask: Deleted Host purged: {}", apHost);
                    }
                }
            }
            deletedHostTimeout = Timer.getTimer().newTimeout(new PurgeHostTask(), removeDelay, TimeUnit.SECONDS);
        }
    }
}
