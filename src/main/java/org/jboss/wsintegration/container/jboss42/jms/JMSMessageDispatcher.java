/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.integration.jboss42.jms;

// $Id:JMSMessageDispatcher.java 915 2006-09-08 08:40:45Z thomas.diesler@jboss.com $

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.soap.SOAPMessage;

import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.util.NotImplementedException;
import org.jboss.ws.WSException;
import org.jboss.ws.core.CommonMessageContext;
import org.jboss.ws.core.MessageAbstraction;
import org.jboss.ws.core.jaxrpc.handler.SOAPMessageContextJAXRPC;
import org.jboss.ws.core.server.legacy.ServiceEndpoint;
import org.jboss.ws.core.server.legacy.ServiceEndpointInvoker;
import org.jboss.ws.core.server.legacy.ServiceEndpointManager;
import org.jboss.ws.core.server.legacy.ServiceEndpointManagerFactory;
import org.jboss.ws.core.soap.MessageContextAssociation;
import org.jboss.ws.metadata.umdm.ServerEndpointMetaData;

/**
 * A dispatcher for SOAPMessages
 *  
 * @author Thomas.Diesler@jboss.org
 */
public class JMSMessageDispatcher implements JMSMessageDispatcherMBean
{
   // logging support
   protected Logger log = Logger.getLogger(JMSMessageDispatcher.class);

   /** Dispatch the message to the underlying SOAP engine
    */
   public SOAPMessage dipatchMessage(String fromName, Object targetBean, InputStream reqMessage) throws RemoteException
   {
      ServiceEndpointManagerFactory factory = ServiceEndpointManagerFactory.getInstance();
      ServiceEndpointManager epManager = factory.getServiceEndpointManager();
      ObjectName sepID = getServiceEndpointForDestination(epManager, fromName);

      if (sepID == null)
         throw new WSException("Cannot find serviceID for: " + fromName);

      log.debug("dipatchMessage: " + sepID);

      // Setup the MDB invoker
      ServiceEndpoint sep = epManager.getServiceEndpointByID(sepID);
      ServerEndpointMetaData sepMetaData = sep.getServiceEndpointInfo().getServerEndpointMetaData();

      ServiceEndpointInvoker invoker = sep.getServiceEndpointInfo().getInvoker();
      if (invoker instanceof ServiceEndpointInvokerMDB)
      {
         ServiceEndpointInvokerMDB mdbInvoker = (ServiceEndpointInvokerMDB)invoker;
         mdbInvoker.setTargetBeanObject(targetBean);
      }

      // Associate a message context with the current thread
      CommonMessageContext msgContext = new SOAPMessageContextJAXRPC();
      MessageContextAssociation.pushMessageContext(msgContext);
      msgContext.setEndpointMetaData(sepMetaData);
      
      SOAPMessage resMessage = null;
      try
      {
         // Process the request message and return the already serialized response
         // Legacy implementations of the JMSTransportSupport dont provide a msg
         // context which is needed for serialization.
         resMessage = (SOAPMessage)sep.processRequest(null, null, reqMessage);
         resMessage.writeTo(new ByteArrayOutputStream());
         return resMessage;
      }
      catch (Exception ex)
      {
         WSException.rethrow("Cannot process SOAP request", ex);
      }
      finally
      {
         MessageContextAssociation.popMessageContext();
      }
      
      return resMessage;
   }

   /** Dispatch the message to the underlying SOAP engine
    */
   public SOAPMessage delegateMessage(String serviceID, InputStream soapMessage) throws RemoteException
   {
      throw new NotImplementedException();
   }

   // The destination jndiName is encoded in the service object name under key 'jms'
   private ObjectName getServiceEndpointForDestination(ServiceEndpointManager epManager, String fromName)
   {
      ObjectName sepID = null;
      for (ObjectName aux : epManager.getServiceEndpoints())
      {
         String jmsProp = aux.getKeyProperty("jms");
         if (jmsProp != null && jmsProp.equals(fromName))
         {
            sepID = aux;
            break;
         }
      }
      return sepID;
   }

   public void create() throws Exception
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      if (server != null)
      {
         server.registerMBean(this, OBJECT_NAME);
      }
   }

   public void destroy() throws Exception
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      if (server != null)
      {
         server.unregisterMBean(OBJECT_NAME);
      }
   }
}
