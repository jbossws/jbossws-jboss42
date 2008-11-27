/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.lang.reflect.Method;

import javax.xml.ws.WebServiceContext;

import org.jboss.wsf.common.JavaUtils;
import org.jboss.wsf.common.javax.JavaxAnnotationHelper;
import org.jboss.wsf.common.javax.PreDestroyHolder;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;
import org.jboss.wsf.spi.invocation.InvocationHandler;
import org.jboss.wsf.spi.invocation.ResourceInjector;
import org.jboss.wsf.spi.invocation.ResourceInjectorFactory;

/**
 * Handles invocations on JSE endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @author richard.opalka@jboss.com
 * @since 25-Apr-2007
 */
public class DefaultInvocationHandler extends InvocationHandler
{
   private SPIProvider spiProvider;
   private ResourceInjectorFactory resourceInjectorFactory;

   public DefaultInvocationHandler()
   {
      spiProvider = SPIProviderResolver.getInstance().getProvider();
      resourceInjectorFactory = spiProvider.getSPI(ResourceInjectorFactory.class);
   }

   public Invocation createInvocation()
   {
      return new Invocation();
   }

   public void init(Endpoint ep)
   {
   }

   protected Object getTargetBean(Endpoint ep, Invocation epInv) throws Exception
   {
      InvocationContext invCtx = epInv.getInvocationContext();
      Object targetBean = invCtx.getTargetBean();
      if (targetBean == null)
      {
         try
         {
            Class<?> epImpl = ep.getTargetBeanClass();
            targetBean = epImpl.newInstance();
            invCtx.setTargetBean(targetBean);
         }
         catch (Exception ex)
         {
            throw new IllegalStateException("Cannot get target bean instance", ex);
         }
         
         JavaxAnnotationHelper.callPostConstructMethod(targetBean, targetBean.getClass().getClassLoader());
         ep.addAttachment(PreDestroyHolder.class, new PreDestroyHolder(targetBean));
      }
      
      return targetBean;
   }
   
   public void invoke(Endpoint ep, Invocation epInv) throws Exception
   {
      try
      {
         Object targetBean = getTargetBean(ep, epInv);

         InvocationContext invContext = epInv.getInvocationContext();
         WebServiceContext wsContext = invContext.getAttachment(WebServiceContext.class);
         if (wsContext != null)
         {
            ResourceInjector injector = resourceInjectorFactory.newResourceInjector();
            injector.inject(targetBean, wsContext);
         }

         Method method = getImplMethod(targetBean.getClass(), epInv.getJavaMethod());
         Object retObj = method.invoke(targetBean, epInv.getArgs());
         epInv.setReturnValue(retObj);
      }
      catch (Exception e)
      {
         handleInvocationException(e);
      }
   }

   protected Method getImplMethod(Class<?> implClass, Method seiMethod) throws ClassNotFoundException, NoSuchMethodException
   {
      String methodName = seiMethod.getName();
      Class<?>[] paramTypes = seiMethod.getParameterTypes();
      for (int i = 0; i < paramTypes.length; i++)
      {
         Class<?> paramType = paramTypes[i];
         if (JavaUtils.isPrimitive(paramType) == false)
         {
            String paramTypeName = paramType.getName();
            paramType = JavaUtils.loadJavaType(paramTypeName);
            paramTypes[i] = paramType;
         }
      }

      Method implMethod = implClass.getMethod(methodName, paramTypes);
      return implMethod;
   }
}
