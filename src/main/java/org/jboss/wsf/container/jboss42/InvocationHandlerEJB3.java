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
import org.jboss.wsf.common.ObjectNameFactory;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.ExtensibleWebServiceContext;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationType;
import org.jboss.wsf.spi.invocation.WebServiceContextFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB3 extends AbstractInvocationHandler
{
   private ObjectName objectName;

   InvocationHandlerEJB3()
   {
   }

   public Invocation createInvocation()
   {
      return new Invocation();
   }

   public void init(Endpoint ep)
   {
      String ejbName = ep.getShortName();
      ArchiveDeployment dep = (ArchiveDeployment)ep.getService().getDeployment();
      String nameStr = "jboss.j2ee:name=" + ejbName + ",service=EJB3,jar=" + dep.getSimpleName();
      if (dep.getParent() != null)
      {
         nameStr += ",ear=" + dep.getParent().getSimpleName();
      }

      objectName = ObjectNameFactory.create(nameStr.toString());

      Dispatcher dispatcher = Dispatcher.singleton;
      if (dispatcher.getRegistered(objectName.getCanonicalName()) == null)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);
   }

   public void invoke(Endpoint ep, Invocation epInv) throws Exception
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

      public CallbackImpl(Invocation epInv)
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
            SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
            WebServiceContextFactory factory = spiProvider.getSPI(WebServiceContextFactory.class);
            ExtensibleWebServiceContext wsContext = factory.newWebServiceContext(InvocationType.JAXWS_EJB3, jaxwsMessageContext);
            wsContext.addAttachment(EJBContext.class, ejbCtx);
            beanProp.set(beanCtx.getInstance(), wsContext);
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