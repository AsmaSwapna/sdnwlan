package net.bcsw.sdnwlan.storage;

import net.bcsw.sdnwlan.config.AccessPointConfig;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.MacAddress;
import org.onosproject.core.CoreService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.StorageService;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


/**
 * Implements the distributed store for the sdnWLAN Service
 * <p>
 * <p>
 * TODO:  THIS IS A PLACEHOLDER FILE ONLY.  ONCE WE NEED PERSISTENT STORAGE, IT WILL NEED SOME WORK
 */
@Component(immediate = true)
@Service
public class DistributedStore extends AbstractStore implements SDNWLANStore {

    private static final String APP_NAME = "net.bcsw.sdnwlan.storage";
    private static Logger log = LoggerFactory.getLogger(DistributedStore.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ConcurrentMap<MacAddress, AccessPointConfig> accessPoints;

    @Activate
    public void activate() {
        log.info("Started");

        accessPoints = Maps.newConcurrentMap();

        // TODO test this out to see if it saves the entry correctly and see if there are ways to improve serialization
        //        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
        //                .register(KryoNamespaces.API)
        //                .register(AccessPointConfig.class)
        //                .register(MacAddress.class);
        //
        //        accessPoints = storageService.<MacAddress, AccessPointConfig>consistentMapBuilder()
        //                .withSerializer(Serializer.using(serializer.build()))
        //                .withName("sdnwlan-access-points")
        //                .withApplicationId(appId)
        //                .withPurgeOnUninstall()
        //                .build();

        // TODO add a MapEvent Listener subscription here (and implement interface near bottom of file

        // TODO Also look at DHCP app and see how they do things..

        // speed if need/required.
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");

    }

    /**
     * Save an access Point
     *
     * @param accessPoint TODO: Document
     */
    public void putAccessPoint(AccessPointConfig accessPoint) {
        // TODO Implement
    }

    /**
     * Remove an access Point
     *
     * @param accessPoint TODO: Document
     */
    public void removeAccessPoint(AccessPointConfig accessPoint) {
        // TODO Implement
    }

    /**
     * Get a list of current access points saved to the store
     *
     * @return list of access points
     */
    public List<AccessPointConfig> getAccessPoints() {

        // TODO Implement

        return null;
    }


    // TODO: Add put/remove/get for mobile hosts as well if needed
}
