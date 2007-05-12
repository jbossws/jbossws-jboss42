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

import javax.ejb.EJBContext;
import javax.management.ObjectName;

import org.jboss.aop.Dispatcher;
import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.BeanContextLifecycleCallback;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.stateless.StatelessBeanContext;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.injection.lang.reflect.BeanProperty;
import org.jboss.logging.Logger;
import org.jboss.ws.WSException;
import org.jboss.ws.core.CommonMessageContext;
import org.jboss.ws.core.EndpointInvocation;
import org.jboss.ws.core.jaxrpc.handler.SOAPMessageContextJAXRPC;
import org.jboss.ws.core.jaxws.WebServiceContextEJB;
import org.jboss.ws.core.jaxws.handler.SOAPMessageContextJAXWS;
import org.jboss.ws.core.server.AbstractInvocationHandler;
import org.jboss.ws.core.soap.MessageContextAssociation;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.invocation.InvocationContext;
import org.jboss.ws.metadata.umdm.ServerEndpointMetaData;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB3 extends AbstractInvocationHandler
{
   // provide logging
   private static final Logger log = Logger.getLogger(InvocationHandlerEJB3.class);
   
   private ObjectName objectName;

   /** Initialize the service endpoint */
   @Override
   public void init(Endpoint endpoint)
   {
      super.init(endpoint);
      
      ServerEndpointMetaData sepMetaData = endpoint.getMetaData(ServerEndpointMetaData.class);
      if (sepMetaData == null)
         throw new IllegalStateException("Cannot obtain endpoint meta data");

      String ejbName = sepMetaData.getLinkName();
      if (ejbName == null)
         throw new WSException("Cannot obtain ejb-link from port component");

      UnifiedDeploymentInfo udi = endpoint.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      String nameStr = "jboss.j2ee:name=" + ejbName + ",service=EJB3,jar=" + udi.simpleName;
      if (udi.parent != null)
      {
         nameStr += ",ear=" + udi.parent.simpleName;
      }

      objectName = ObjectNameFactory.create(nameStr.toString());
   }

   /** Load the SEI implementation bean if necessary 
    */
   public Class loadServiceEndpoint()
   {
      Dispatcher dispatcher = Dispatcher.singleton;
      if (dispatcher.getRegistered(objectName.getCanonicalName()) == null)
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

      // invoke on the container
      try
      {
         // setup the invocation
         Method seiMethod = epInv.getJavaMethod();
         Object[] args = epInv.getRequestPayload();

         Dispatcher dispatcher = Dispatcher.singleton;
         StatelessContainer container = (StatelessContainer)dispatcher.getRegistered(objectName.getCanonicalName());
         Class beanClass = container.getBeanClass();

         Method implMethod = getImplMethod(beanClass, seiMethod);
         MethodInfo info = container.getMethodInfo(implMethod);
         
         EJBContainerInvocation<StatelessContainer, StatelessBeanContext> ejb3Inv = new EJBContainerInvocation<StatelessContainer, StatelessBeanContext>(info);
         ejb3Inv.setAdvisor(container);
         ejb3Inv.setArguments(args);
         ejb3Inv.setContextCallback(new ContextCallback());
         
         Object retObj = ejb3Inv.invokeNext();

         epInv.setReturnValue(retObj);
      }
      catch (Throwable th)
      {
         handleInvocationException(th);
      }
   }

   /** Create an instance of the SEI implementation bean if necessary */
   public void destroyServiceEndpointInstance(Object seiImpl)
   {
      // do nothing
   }

   class ContextCallback implements BeanContextLifecycleCallback
   {
      private SOAPMessageContextJAXWS jaxwsMessageContext;
      private SOAPMessageContextJAXRPC jaxrpcMessageContext;

      public ContextCallback()
      {
         CommonMessageContext msgContext = MessageContextAssociation.peekMessageContext();
         if (msgContext instanceof SOAPMessageContextJAXRPC)
         {
            jaxrpcMessageContext = (SOAPMessageContextJAXRPC)msgContext;
            jaxwsMessageContext = new SOAPMessageContextJAXWS(msgContext);
         }
         else if (msgContext instanceof SOAPMessageContextJAXWS)
         {
            jaxwsMessageContext = (SOAPMessageContextJAXWS)msgContext;
            jaxrpcMessageContext = new SOAPMessageContextJAXRPC(msgContext);
         }
      }

      public void attached(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;
         sbc.setMessageContextJAXRPC(jaxrpcMessageContext);
         
         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null)
         {
            EJBContext ejbCtx = beanCtx.getEJBContext(); 
            beanProp.set(beanCtx.getInstance(), new WebServiceContextEJB(jaxwsMessageContext, ejbCtx));
         }
      }

      public void released(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;
         sbc.setMessageContextJAXRPC(null);
         
         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null)
            beanProp.set(beanCtx.getInstance(), null);
      }
   }
}
