/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class is a REST controller that returns information about the client user.
 * E.g. the client's IP address.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@RequestMapping(value = "/api/userinfo")
@RestController
public class ClarinUserInfoController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * This method returns the client's IP address.
     * @param request The HttpServletRequest object.
     * @return The client's IP address.
     */
    @RequestMapping(method = RequestMethod.GET, path = "/ipaddress")
    public ResponseEntity<Object> getUserIPAddress(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Get client's IP address
        String ipAddress = request.getRemoteAddr();
        if (StringUtils.isBlank(ipAddress)) {
            String errorMessage = "Cannot get user's IP address";
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }

        // Create JSON object using Jackson's ObjectNode
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("ipAddress", ipAddress);

        return ResponseEntity.ok().body(jsonObject);
    }
}
