/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 */
public class DeleteConnectionAction implements Callable {

    private static Logger logger = Logger.getLogger(DeleteConnectionAction.class);
    
    private DeleteConnection req;
    private MgcpController controller;
    private MgcpUtils utils = new MgcpUtils();
    
    protected DeleteConnectionAction(MgcpController controller, DeleteConnection req) {
        this.controller = controller;
        this.req = req;
    }
    
    private JainMgcpResponseEvent endpointDeleteConnections(String localName) {
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(localName, true);
        } catch (Exception e) {
            return new DeleteConnectionResponse(controller, ReturnCode.Endpoint_Unknown);
        }
        
        endpoint.deleteAllConnections();
        
        Collection<ConnectionActivity> activities = controller.getActivities(localName);
        for (ConnectionActivity activity : activities) {
            activity.close();
        }
        
        DeleteConnectionResponse response = new DeleteConnectionResponse(controller, ReturnCode.Transaction_Executed_Normally);
        return response;
    }
    
    private JainMgcpResponseEvent deleteConnection(String localName, String connectionID) {
        Endpoint endpoint = null;
        try {
            endpoint = controller.getServer().lookup(localName, true);
        } catch (Exception e) {
        	if(logger.isEnabledFor(Level.ERROR))
        	{
        		logger.error("Failed on endpoint lookup: "+localName,e);
        	}
            return new DeleteConnectionResponse(controller, ReturnCode.Endpoint_Unknown);
        }

        ConnectionActivity activity = controller.getActivity(localName, connectionID);
        
        ConnectionParm[] parms = new ConnectionParm[3];
        parms[0] = new RegularConnectionParm(RegularConnectionParm.OCTETS_RECEIVED, (int)activity.connection.getBytesReceived());
        parms[1] = new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, (int)activity.connection.getBytesTransmitted());
        parms[2] = new RegularConnectionParm(RegularConnectionParm.JITTER, (int)(activity.connection.getJitter() * 1000));
        
        endpoint.deleteConnection(activity.connection.getId());
        activity.close();
        
        DeleteConnectionResponse response = new DeleteConnectionResponse(controller, ReturnCode.Transaction_Executed_Normally);
        response.setConnectionParms(parms);
        
        return response;
    }
    
    public JainMgcpResponseEvent call() throws Exception {
        int txID = req.getTransactionHandle();
        CallIdentifier callID = req.getCallIdentifier();
        EndpointIdentifier endpointID = req.getEndpointIdentifier();
        ConnectionIdentifier connectionID = req.getConnectionIdentifier();
        
        logger.info("Request TX= " + txID + 
                ", CallID = " +  callID + 
                ", Endpoint = " + endpointID + 
                ", Connection = " + connectionID);
        

        JainMgcpResponseEvent response = null;
        if (endpointID != null && callID == null && connectionID == null) {
            response = this.endpointDeleteConnections(endpointID.getLocalEndpointName());
        } else if (endpointID != null && callID != null && connectionID == null) {
        	//TODO : Delete all connection of Endpoint that belong to given callId
        	response = this.endpointDeleteConnections(endpointID.getLocalEndpointName());
        } else if (endpointID != null && callID != null && connectionID != null) {
            response = this.deleteConnection(endpointID.getLocalEndpointName(), connectionID.toString());
        } else {
        	//This is error condition        	
            response = new DeleteConnectionResponse(controller, ReturnCode.Protocol_Error);
        } 
        //Otherwise it wont be sent.
        response.setTransactionHandle(txID);
        logger.info("Response TX=" + txID + ", response=" + response.getReturnCode());
        return response;
    }
}