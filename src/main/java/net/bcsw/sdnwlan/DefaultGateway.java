package net.bcsw.sdnwlan;

import net.bcsw.sdnwlan.config.GatewayConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains information on a default gateway in use by mobile hosts in the
 * sdnWLAN SDN network.
 *
 * Unlike Mobile Hosts, Default gateways are initially known only by IP address
 * and the location needs to be learned and monitored by the system.
 *
 * If a particular system has multiple gateway router IP addresses registered to
 * the same MAC Address, one DefaultGateway object per IP address will be created.
 */
public class DefaultGateway extends SDNWLANHostInfo {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // The Gateway Configuration
    private final GatewayConfig gatewayConfig;

    // Default flows installed?

    private AtomicBoolean defaultFlowsInstalled = new AtomicBoolean(false);

    // The access points this gateway is associated with
    // TODO: Currently only support one access point per default gateway
    private List<AccessPoint> accessPoints = Lists.newArrayList();

    private static final ProviderId unknownProviderId = ProviderId.NONE;
    private static final MacAddress unknownMACAddress = MacAddress.NONE;
    private static final HostLocation unknownHostLocation = HostLocation.NONE;

    /**
     * Construct a new DefaultGateway object
     *
     * @param gwConfig The Gateway Configuration
     */
    public DefaultGateway(GatewayConfig gwConfig) {
        super(new DefaultHost(unknownProviderId,
                HostId.hostId(unknownMACAddress,
                        (getSdnViewOfGatewayVlans(gwConfig.getIngressVlans()).size() > 0) ?
                                getSdnViewOfGatewayVlans(gwConfig.getIngressVlans()).get(0) :
                                VlanId.NONE),
                unknownMACAddress,
                (getSdnViewOfGatewayVlans(gwConfig.getIngressVlans()).size() > 0) ?
                        getSdnViewOfGatewayVlans(gwConfig.getIngressVlans()).get(0) :
                        VlanId.NONE,
                unknownHostLocation,  Sets.newConcurrentHashSet()));

        this.gatewayConfig = gwConfig;
    }

    public static List<VlanId> getSdnViewOfGatewayVlans(IngressVlans gwVlans) {
        List<VlanId> vids = Lists.newArrayList();

        // Determine what the Gateway ConnectPoint does to or expects of any incoming frames

        if ((gwVlans == null) || (gwVlans.getVlanList().size() == 0)) {
            vids.add(VlanId.NONE);
        } else {
            vids.addAll(gwVlans.getVlanList());
        }
        return vids;
    }

    /**
     * Get the single IP Gateway address and CIDR that this object monitors
     *
     * @return IPGateway address and mask
     */
    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public boolean isDefaultFlowsInstalled() {
        return defaultFlowsInstalled.get();
    }

    /**
     * Sets the value of the flow-installed atomic to the given value.  If the value
     * was already the 'installed' value you want to set, then 'false' returned
     *
     * @param installed  Value to set the atomic too
     *
     * @return true if the existing value was not what we wanted set, false if
     *         the existing value matches the new value you wanted.
     */
    public boolean setDefaultFlowsInstalled(boolean installed) {

        return defaultFlowsInstalled.compareAndSet(!installed, installed);
    }

    /**
     *
     * Add an access point (BSAP) serviced by this default gateway
     *
     * @param point Access point to add
     *
     * @return True if access point added, False if it already existed in the list
     */
    public boolean addAccessPoint(AccessPoint point) {

        boolean status = accessPoints.add(point);

        // TODO: Currently only support a single access point

        if (accessPoints.size() > 1) {
            log.error("{}: TODO: Default gateway access point count is now {}",
                    this.toString(), accessPoints.size());
        }
        return status;
    }

    /**
     * Remove an access point (BSAP) serviced by this default gateway
     *
     * @param point Access Point to remove
     *
     * @return True if access point located and removed
     */
    public boolean removeAccessPoint(AccessPoint point) {

        return accessPoints.remove(point);
    }

    /**
     * Get a list of access points (BSAPs) serviced by this default gateway
     *
     * @return List of access points
     */
    public List<AccessPoint> getAccessPoints() {
        return Collections.unmodifiableList(accessPoints);
    }

    /**
     * Update host information
     *
     *   This information can be called when the gateway is first discovered or when
     *   some host information changes.
     *
     * @param info New information
     */
    @Override
    public void updateHost(Host info) {

        // Snapshot old host information and set then set to new

        Host oldHostInfo = this.hostInfo;
        this.hostInfo = info;

        // TODO: This area needs work and cleaning up once we test things

        boolean newLocation     = !oldHostInfo.location().equals(info.location());
        boolean newMacAddress   = !oldHostInfo.mac().equals(info.mac());

        if (newLocation || newMacAddress) {

            boolean validLocation   = !hostInfo.location().equals(unknownHostLocation);
            boolean validMacAddress = !hostInfo.mac().equals(unknownMACAddress);
            boolean oldWasUnknown   = oldHostInfo.location().equals(unknownHostLocation)
                                   || oldHostInfo.mac().equals(unknownMACAddress);

            // Either (or both) the MAC and Location changed and both MAC and Location are
            // valid.  Start up any AP's that reference this default gateway

            if (validLocation && validMacAddress) {
                if (oldWasUnknown) {
                    log.info("updateHost: starting up related access points");

                    this.getAccessPoints().forEach(ap -> ap.onStartup());
                } else {
                    // TODO: existing 'fully known' default gateway has changed

                    log.warn("ODO: existing 'fully known' default gateway has changed");
                }
            } else if (!oldWasUnknown) {
                // TODO: existing 'fully known' default gateway is no longer fully known

                log.warn("TODO: existing 'fully known' default gateway is no longer fully known");
            }
            // TODO: any other cases or a better way to do the above?
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && obj instanceof DefaultGateway) {
            DefaultGateway other = (DefaultGateway) obj;
            return this.gatewayConfig.equals(other.gatewayConfig);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{this.gatewayConfig.getGatewayAndMask()});
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

//  TODO:      builder.append(String.format("%s -> %s",
//                gatewayAndMask.toString(), super.toString()));
        builder.append(String.format("%s",
                gatewayConfig.getGatewayAndMask().toString()));

        return builder.toString();
    }

    // TODO: May want to move the installation of any default gateway flows between BSAPs and the default gateway to this object
}
