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
package org.jboss.wsf.container.jboss42;

// $Id$

import org.jboss.ejb.EjbModule;
import org.jboss.ejb.Interceptor;
import org.jboss.ejb.StatelessSessionContainer;
import org.jboss.invocation.InvocationKey;
import org.jboss.invocation.InvocationType;
import org.jboss.invocation.PayloadKey;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.common.ObjectNameFactory;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.invocation.*;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.SPIProvider;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.security.Principal;

/**
 * Handles invocations on EJB21 endpoints.
 * Used with jboss40 and jboss42.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB21 extends InvocationHandler
{
   // provide logging
   private static final Logger log = Logger.getLogger(InvocationHandlerEJB21.class);

   private String jndiName;
   private MBeanServer server;
   private ObjectName objectName;

   /**
    * Used from both 40 and 42.
    * Therefore it's not package protected...    
    */
   public InvocationHandlerEJB21()
   {
   }

   public Invocation createInvocation()
   {
      return new Invocation();
   }

   public void init(Endpoint ep)
   {
      String ejbName = ep.getShortName();
      UnifiedDeploymentInfo udi = ep.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      UnifiedApplicationMetaData applMetaData = (UnifiedApplicationMetaData)udi.metaData;
      UnifiedBeanMetaData beanMetaData = (UnifiedBeanMetaData)applMetaData.getBeanByEjbName(ejbName);
      if (beanMetaData == null)
         throw new WebServiceException("Cannot obtain ejb meta data for: " + ejbName);

      // get the bean's JNDI name
      jndiName = beanMetaData.getContainerObjectNameJndiName();
      if (jndiName == null)
         throw new WebServiceException("Cannot obtain JNDI name for: " + ejbName);

      server = MBeanServerLocator.locateJBoss();
      objectName = ObjectNameFactory.create("jboss.j2ee:jndiName=" + jndiName + ",service=EJB");
      if (server.isRegistered(objectName) == false)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);

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

   public void invoke(Endpoint ep, Invocation inv) throws Exception
   {
      log.debug("Invoke: " + inv.getJavaMethod().getName());

      // invoke on the container
      try
      {
         // setup the invocation
         org.jboss.invocation.Invocation jbInv = getMBeanInvocation(inv);

         String[] sig = { org.jboss.invocation.Invocation.class.getName() };
         Object retObj = server.invoke(objectName, "invoke", new Object[] { jbInv }, sig);
         inv.setReturnValue(retObj);
      }
      catch (Exception e)
      {
         handleInvocationException(e);
      }
   }

   private org.jboss.invocation.Invocation getMBeanInvocation(Invocation inv)
   {
      // EJB2.1 endpoints will only get an JAXRPC context 
      MessageContext msgContext = inv.getInvocationContext().getAttachment(MessageContext.class);
      if (msgContext == null)
         throw new IllegalStateException("Cannot obtain MessageContext");

      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      SecurityAdaptor securityAdaptor = spiProvider.getSPI(SecurityAdaptorFactory.class).createSecurityAdapter();
      Principal principal = securityAdaptor.getPrincipal();
      Object credential = securityAdaptor.getCredential();

      Method method = inv.getJavaMethod();
      Object[] args = inv.getArgs();
      org.jboss.invocation.Invocation jbInv = new org.jboss.invocation.Invocation(null, method, args, null, principal, credential);

      HandlerCallback callback = inv.getInvocationContext().getAttachment(HandlerCallback.class);
      if (callback == null)
         throw new IllegalStateException("Cannot obtain HandlerCallback");

      jbInv.setValue(InvocationKey.SOAP_MESSAGE_CONTEXT, msgContext);
      jbInv.setValue(InvocationKey.SOAP_MESSAGE, ((SOAPMessageContext)msgContext).getMessage());
      jbInv.setType(InvocationType.SERVICE_ENDPOINT);
      jbInv.setValue(HandlerCallback.class.getName(), callback, PayloadKey.TRANSIENT);
      jbInv.setValue(Invocation.class.getName(), inv, PayloadKey.TRANSIENT);

      return jbInv;
   }
}
