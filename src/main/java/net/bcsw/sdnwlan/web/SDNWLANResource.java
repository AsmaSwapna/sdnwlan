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
package net.bcsw.sdnwlan.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.bcsw.sdnwlan.AccessPoint;
import net.bcsw.sdnwlan.APManager;
import net.bcsw.sdnwlan.SDNWLANService;
import net.bcsw.sdnwlan.config.AccessPointConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST services for the sdnWLAN Applicaiton
 */
@Path("sdnwlan")
public class SDNWLANResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    /**
     * Create a new sdnWLAN Access Point
     *
     * @param stream input stream
     * @return response to the request
     */
    @POST
    @Path("add")
    public String accessPointAddNotification(InputStream stream) {

        log.info("Received sdnWLAN Access Point create request");

        if (stream == null) {
            log.info("Parameters can not be null");
            return "";
        }
        APManager sdnwlanService = get(APManager.class);

        try {
            AccessPointConfig entry = jsonToAccessPoint(stream);
            // Create the access point (TODO Look into what we want to return)
            //String returnData = sdnwlanService.createAccessPoint(entry);
            String returnData = null;   // TODO not yet supported
            if (returnData != null) {
                return returnData;
            }
        } catch (IOException e) {
            log.error("Failed to deserialize AccessPoint JSON {}", e.toString());
        }
        return "";
    }

    /**
     * Convert received JSON into an access point entry
     *
     * @param stream
     * @return
     * @throws IOException //TODO add custom decode exception later?
     */
    private AccessPointConfig jsonToAccessPoint(InputStream stream) throws IOException {

        throw new IOException("TODO: Not yet supported");

    }

    /**
     * Delete a virtual BNG connection.
     *
     * @param mac MAC Address of the host
     * @return version string for new (TODO) What do we really want
     */
    @DELETE
    @Path("{mac}")
    public String accessPointDeleteNotification(@PathParam("mac") MacAddress mac) {
        if (mac == null) {
            log.info("mac to delete is null");
            return "";
        }
        log.info("Received sdnWLAN Access Point delete request: hostname= {}", mac.toString());

        APManager sdnwlanService = get(APManager.class);

        // Delete the access point (TODO Look into what we want to return)

        //String returnData = sdnwlanService.deleteAccessPoint(mac);
        String returnData = null;

        if (returnData != null) {
            return returnData;
        } else {
            return "";
        }
    }

    /**
     * Query virtual BNG map.
     *
     * @return IP Address map
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response accessPointGetNotification() {

        log.info("Received WLAN AccessPoint GET list request");

        SDNWLANService service = get(SDNWLANService.class);

        Iterator<AccessPoint> it = service.getAccessPoints().values().iterator();

        List<AccessPointConfig> list = Lists.newArrayList();

        list.addAll(service.getAccessPoints().values().stream().map(pt ->
                (AccessPointConfig) pt).collect(Collectors.toList()));

        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("list", new SDNWLANAccessPointCodec().encode(list, this));

        return ok(result.toString()).build();
    }
    // TODO quite a few other operations and URIs may be needed for this application
}

