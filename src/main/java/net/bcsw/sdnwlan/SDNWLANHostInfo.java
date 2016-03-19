package net.bcsw.sdnwlan;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceAdminService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Common Host information for Mobile Hosts and Default Gateway Hosts
 */
public class SDNWLANHostInfo {

    protected InterfaceAdminService interfaceService = null;

    protected Host hostInfo = null;

    protected final Date date         = new Date();
    protected long       createTime   = date.getTime(); // When this gateway was first created
    protected long       lastMoveTime = createTime;     // When this gateway last moved
    protected long       removedTime  = 0;              // Last HOST_REMOVED event
    protected int        timesMoved   = 0;
    protected String     lastVlanHostName = "";

    protected SDNWLANHostInfo() {
    }

    protected SDNWLANHostInfo(Host info) {
        this.hostInfo = info;
    }

    /**
     * ONOS Host information for this mobile host
     *
     * @return Host information
     */
    public Host getHostInfo() {
        return hostInfo;
    }

    /**
     * Update host information
     *
     * @param info New information
     */
    public void updateHost(Host info) {
        // TODO: Implement


    }

    /**
     * Get the unique MAC address for this host
     *
     * @return MAC address
     */
    public MacAddress getMacAddress() {
        return hostInfo.mac();
    }

    /**
     * Register a host or gateway with the interface service so that ARPs can be answered
     *
     *   If the ingress VLANs between a mobile host and default gateway are on separate VLANs
     *   as seen from the SDN network, we need to register they with the Interface service
     *   so that ARPs can be resolved.
     *
     */
    public void registerHost(IpPrefix ipPrefix, List<VlanId> vids) {

        if (interfaceService == null) {
            ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
            interfaceService = serviceDirectory.get(InterfaceAdminService.class);
        }
        if ((hostInfo != null) && !hostInfo.ipAddresses().isEmpty()){
            ConnectPoint point = new ConnectPoint(hostInfo.location().elementId(), hostInfo.location().port());

            Set<InterfaceIpAddress> intfIps = Sets.newHashSetWithExpectedSize(hostInfo.ipAddresses().size());

            hostInfo.ipAddresses().forEach(ip ->{
                intfIps.add(new InterfaceIpAddress(ip, ipPrefix));
            });
            // TODO: Only support single tagging

            VlanId vlan = vids.isEmpty() ? VlanId.NONE : vids.get(0);

            // Create a host name, use MAC+VLAN

            lastVlanHostName = String.format("%s/%d", hostInfo.mac().toString(), hostInfo.vlan().toShort());

            // Add the interface

            Interface intf = new Interface(lastVlanHostName, point, intfIps, hostInfo.mac(), vlan);

            interfaceService.add(intf);
        }
    }

    public void unregisterHost() {

        if (!lastVlanHostName.isEmpty() && (hostInfo != null)) {

            ConnectPoint point = new ConnectPoint(hostInfo.location().elementId(), hostInfo.location().port());

            interfaceService.remove(point, lastVlanHostName);

            lastVlanHostName = "";
        }
    }

    /**
     * Time when 'HOST_REMOVED' last received
     * @return
     */
    public long getRemovedTime() {
        return removedTime;
    }

    public void setRemovedTime() {
        Date dt = new Date();
        removedTime = dt.getTime();
    }

    public void setRemovedTime(long time) {
        removedTime = time;
    }
    // TODO: Add the following three to the APHostCommand output
    public long getCreationTime() { return createTime; }
    public long getLastMovedTime() { return lastMoveTime; }
    public int  getTimesMoved() { return timesMoved; }


    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s/%d, %s%d",
                hostInfo.mac().toString(), hostInfo.vlan().toShort(),
                hostInfo.location().deviceId().toString(),
                hostInfo.location().port().toLong()));

        return builder.toString();
    }
}
