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

import java.lang.reflect.Method;

import javax.ejb.EJBContext;
import javax.management.ObjectName;
import javax.xml.ws.WebServiceException;

import org.jboss.aop.Dispatcher;
import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.BeanContextLifecycleCallback;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.stateless.StatelessBeanContext;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.injection.lang.reflect.BeanProperty;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.invocation.EndpointInvocation;
import org.jboss.wsf.spi.invocation.WebServiceContextEJB;
import org.jboss.wsf.spi.utils.ObjectNameFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB3 extends AbstractInvocationHandler
{
   private ObjectName objectName;

   public void create(Endpoint ep)
   {
      super.create(ep);

      String ejbName = ep.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
      if (ejbName == null)
         throw new WebServiceException("Cannot obtain ejb-link");

      UnifiedDeploymentInfo udi = ep.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      String nameStr = "jboss.j2ee:name=" + ejbName + ",service=EJB3,jar=" + udi.simpleName;
      if (udi.parent != null)
      {
         nameStr += ",ear=" + udi.parent.simpleName;
      }

      objectName = ObjectNameFactory.create(nameStr.toString());
   }

   public void start(Endpoint ep)
   {
      super.start(ep);

      Dispatcher dispatcher = Dispatcher.singleton;
      if (dispatcher.getRegistered(objectName.getCanonicalName()) == null)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);
   }

   public void invoke(Endpoint ep, Object beanInstance, EndpointInvocation epInv) throws Exception
   {
      try
      {
         Dispatcher dispatcher = Dispatcher.singleton;
         StatelessContainer container = (StatelessContainer)dispatcher.getRegistered(objectName.getCanonicalName());
         Class beanClass = container.getBeanClass();

         Method method = getImplMethod(beanClass, epInv.getJavaMethod());
         Object[] args = epInv.getArgs();

         MethodInfo info = container.getMethodInfo(method);
         EJBContainerInvocation<StatelessContainer, StatelessBeanContext> ejb3Inv = new EJBContainerInvocation<StatelessContainer, StatelessBeanContext>(info);
         ejb3Inv.setAdvisor(container);
         ejb3Inv.setArguments(args);
         ejb3Inv.setContextCallback(new CallbackImpl(epInv));

         Object retObj = ejb3Inv.invokeNext();

         epInv.setReturnValue(retObj);
      }
      catch (Throwable th)
      {
         handleInvocationException(th);
      }
   }

   static class CallbackImpl implements BeanContextLifecycleCallback
   {
      private javax.xml.ws.handler.MessageContext jaxwsMessageContext;
      private javax.xml.rpc.handler.MessageContext jaxrpcMessageContext;

      public CallbackImpl(EndpointInvocation epInv)
      {
         jaxrpcMessageContext = epInv.getInvocationContext().getAttachment(javax.xml.rpc.handler.MessageContext.class);
         jaxwsMessageContext = epInv.getInvocationContext().getAttachment(javax.xml.ws.handler.MessageContext.class);
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