package org.janelia.it.workstation.browser.api.http;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.janelia.it.workstation.browser.api.exceptions.RemoteServiceException;
import org.slf4j.Logger;

public class RESTClientBase {

    protected final Logger log;
    
    protected RESTClientBase(Logger log) {
        this.log = log;
    }

    // TODO: eliminate this method, and use the other one which logs the service URL
    protected boolean checkBadResponse(int responseStatus, String failureError) {
        if (responseStatus<200 || responseStatus>=300) {
            log.error("ERROR RESPONSE: " + responseStatus);
            log.error(failureError);
            return true;
        }
        return false;
    }
    
    protected void checkBadResponse(WebTarget target, Response response) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Request for {} returned {} {}", target.getUri(), responseStatus, status);
            throw new RemoteServiceException("Remote service returned "+response+" response");
        }
    }
    
}
