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
package org.jboss.ws.integration.jboss42.jbossws;

// $Id$

import java.lang.reflect.Method;
import java.security.Principal;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ejb.EjbModule;
import org.jboss.ejb.Interceptor;
import org.jboss.ejb.StatelessSessionContainer;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.InvocationKey;
import org.jboss.invocation.InvocationType;
import org.jboss.invocation.PayloadKey;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.security.SecurityAssociation;
import org.jboss.ws.WSException;
import org.jboss.ws.core.CommonMessageContext;
import org.jboss.ws.core.EndpointInvocation;
import org.jboss.ws.core.jaxrpc.handler.HandlerCallback;
import org.jboss.ws.core.jaxrpc.handler.SOAPMessageContextJAXRPC;
import org.jboss.ws.core.server.AbstractInvocationHandler;
import org.jboss.ws.core.soap.MessageContextAssociation;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.invocation.InvocationContext;
import org.jboss.ws.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.ws.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;
import org.jboss.ws.metadata.umdm.ServerEndpointMetaData;
import org.jboss.ws.metadata.umdm.EndpointMetaData.Type;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * Handles invocations on EJB21 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB21 extends AbstractInvocationHandler
{
   // provide logging
   private static final Logger log = Logger.getLogger(InvocationHandlerEJB21.class);

   private String jndiName;
   private MBeanServer server;
   private ObjectName objectName;

   /** Initialize the service endpoint */
   @Override
   public void init(Endpoint endpoint)
   {
      super.init(endpoint);

      server = MBeanServerLocator.locateJBoss();

      ServerEndpointMetaData sepMetaData = endpoint.getMetaData(ServerEndpointMetaData.class);
      if (sepMetaData == null)
         throw new IllegalStateException("Cannot obtain endpoint meta data");

      String ejbName = sepMetaData.getLinkName();
      if (ejbName == null)
         throw new WSException("Cannot obtain ejb-link from port component");

      UnifiedDeploymentInfo udi = endpoint.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      UnifiedApplicationMetaData applMetaData = (UnifiedApplicationMetaData)udi.metaData;
      UnifiedBeanMetaData beanMetaData = (UnifiedBeanMetaData)applMetaData.getBeanByEjbName(ejbName);
      if (beanMetaData == null)
         throw new WSException("Cannot obtain ejb meta data for: " + ejbName);

      // verify the service endpoint
      String seiName = sepMetaData.getServiceEndpointInterfaceName();
      if (sepMetaData.getType() == Type.JAXRPC && seiName != null)
      {
         String bmdSEI = beanMetaData.getServiceEndpointInterface();
         if (seiName.equals(bmdSEI) == false)
            throw new WSException("Endpoint meta data defines SEI '" + seiName + "', <service-endpoint> in ejb-jar.xml defines '" + bmdSEI + "'");
      }

      // get the bean's JNDI name
      jndiName = beanMetaData.getContainerObjectNameJndiName();
      if (jndiName == null)
         throw new WSException("Cannot obtain JNDI name for: " + ejbName);

      objectName = ObjectNameFactory.create("jboss.j2ee:jndiName=" + jndiName + ",service=EJB");

      // Dynamically add the service endpoint interceptor
      // http://jira.jboss.org/jira/browse/JBWS-758
      try
      {
         EjbModule ejbModule = (EjbModule)server.getAttribute(objectName, "EjbModule");
         StatelessSessionContainer container = (StatelessSessionContainer)ejbModule.getContainer(ejbName);

         boolean injectionPointFound = false;
         Interceptor prev = container.getInterceptor();
         while (prev != null && prev.getNext() != null)
         {
            Interceptor next = prev.getNext();
            if (next.getNext() == null)
            {
               log.debug("Inject service endpoint interceptor after: " + prev.getClass().getName());
               ServiceEndpointInterceptor sepInterceptor = new ServiceEndpointInterceptor();
               prev.setNext(sepInterceptor);
               sepInterceptor.setNext(next);
               injectionPointFound = true;
            }
            prev = next;
         }
         if (injectionPointFound == false)
            log.warn("Cannot service endpoint interceptor injection point");
      }
      catch (Exception ex)
      {
         log.warn("Cannot add service endpoint interceptor", ex);
      }
   }

   /** Load the SEI implementation bean if necessary 
    */
   public Class loadServiceEndpoint()
   {
      if (server.isRegistered(objectName) == false)
         throw new WSException("Cannot find service endpoint target: " + objectName);

      return null;
   }

   /** Create an instance of the SEI implementation bean if necessary */
   @Override
   protected Object createServiceEndpointInstance(Class seiImplClass, InvocationContext context) throws Exception
   {
      return null;
   }

   /** Invoke an instance of the SEI implementation bean */
   public void invokeServiceEndpointInstance(Object seiImpl, EndpointInvocation epInv) throws Exception
   {
      log.debug("invokeServiceEndpoint: " + epInv.getJavaMethod().getName());

      // these are provided by the ServerLoginHandler
      Principal principal = SecurityAssociation.getPrincipal();
      Object credential = SecurityAssociation.getCredential();

      CommonMessageContext msgContext = MessageContextAssociation.peekMessageContext();

      // invoke on the container
      try
      {
         // setup the invocation
         Method method = epInv.getJavaMethod();
         Object[] args = epInv.getRequestPayload();
         Invocation inv = new Invocation(null, method, args, null, principal, credential);

         // EJB2.1 endpoints will only get an JAXRPC context 
         if ((msgContext instanceof javax.xml.rpc.handler.MessageContext) == false)
            msgContext = new SOAPMessageContextJAXRPC(msgContext);

         inv.setValue(InvocationKey.SOAP_MESSAGE_CONTEXT, msgContext);
         inv.setValue(InvocationKey.SOAP_MESSAGE, msgContext.getSOAPMessage());
         inv.setType(InvocationType.SERVICE_ENDPOINT);

         // Set the handler callback and endpoint invocation
         ServerEndpointMetaData sepMetaData = endpoint.getMetaData(ServerEndpointMetaData.class);
         inv.setValue(HandlerCallback.class.getName(), new HandlerCallbackImpl(sepMetaData), PayloadKey.TRANSIENT);
         inv.setValue(EndpointInvocation.class.getName(), epInv, PayloadKey.TRANSIENT);

         String[] sig = { Invocation.class.getName() };
         Object retObj = server.invoke(objectName, "invoke", new Object[] { inv }, sig);
         epInv.setReturnValue(retObj);
      }
      catch (Exception e)
      {
         handleInvocationException(e);
      }
   }

   /** Create an instance of the SEI implementation bean if necessary */
   public void destroyServiceEndpointInstance(Object seiImpl)
   {
      // do nothing
   }

   /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
   @Override
   public boolean callRequestHandlerChain(ServerEndpointMetaData sepMetaData, HandlerType type)
   {
      if (type == HandlerType.PRE)
         return delegate.callRequestHandlerChain(sepMetaData, type);
      else return true;
   }

   /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
   public boolean callResponseHandlerChain(ServerEndpointMetaData sepMetaData, HandlerType type)
   {
      if (type == HandlerType.PRE)
         return delegate.callResponseHandlerChain(sepMetaData, type);
      else return true;
   }

   /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
   public boolean callFaultHandlerChain(ServerEndpointMetaData sepMetaData, HandlerType type, Exception ex)
   {
      if (type == HandlerType.PRE)
         return delegate.callFaultHandlerChain(sepMetaData, type, ex);
      else return true;
   }

   // The ServiceEndpointInterceptor calls the methods in this callback
   public class HandlerCallbackImpl implements HandlerCallback
   {
      private ServerEndpointMetaData sepMetaData;

      public HandlerCallbackImpl(ServerEndpointMetaData sepMetaData)
      {
         this.sepMetaData = sepMetaData;
      }

      /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
      public boolean callRequestHandlerChain(HandlerType type)
      {
         if (type == HandlerType.PRE)
            return true;
         else return delegate.callRequestHandlerChain(sepMetaData, type);
      }

      /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
      public boolean callResponseHandlerChain(HandlerType type)
      {
         if (type == HandlerType.PRE)
            return true;
         else return delegate.callResponseHandlerChain(sepMetaData, type);
      }

      /** Handlers are beeing called through the HandlerCallback from the EJB interceptor */
      public boolean callFaultHandlerChain(HandlerType type, Exception ex)
      {
         if (type == HandlerType.PRE)
            return true;
         else return delegate.callFaultHandlerChain(sepMetaData, type, ex);
      }
   }
}
