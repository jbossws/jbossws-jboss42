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

// $Id$

import java.lang.reflect.Method;

import org.jboss.logging.Logger;
import org.jboss.ws.core.EndpointInvocation;
import org.jboss.ws.core.server.legacy.AbstractServiceEndpointInvoker;
import org.jboss.ws.core.server.legacy.ServiceEndpointInvoker;
import org.jboss.ws.core.utils.ThreadLocalAssociation;

/**
 * Handles invocations on MDB endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 21-Mar-2006
 */
public class ServiceEndpointInvokerMDB extends AbstractServiceEndpointInvoker implements ServiceEndpointInvoker
{
   // provide logging
   private Logger log = Logger.getLogger(ServiceEndpointInvokerMDB.class);

   /** Load the SEI implementation bean if necessary
    */
   public Class loadServiceEndpoint() throws ClassNotFoundException
   {
      return null;
   }

   // The dispatcher sets the target bean object
   public void setTargetBeanObject(Object targetMDB)
   {
      ThreadLocalAssociation.localInvokerMDBAssoc().set(targetMDB);
   }

   /** Create an instance of the SEI implementation bean if necessary
    */
   public Object createServiceEndpointInstance(Object endpointContext, Class seiImplClass) throws InstantiationException, IllegalAccessException
   {
      return ThreadLocalAssociation.localInvokerMDBAssoc().get();
   }

   /** Invoke an instance of the SEI implementation bean */
   public void invokeServiceEndpointInstance(Object seiImpl, EndpointInvocation epInv) throws Exception
   {
      log.debug("invokeServiceEndpoint: " + epInv.getJavaMethod().getName());
      try
      {
         Class implClass = seiImpl.getClass();
         Method seiMethod = epInv.getJavaMethod();
         Method implMethod = getImplMethod(implClass, seiMethod);

         Object[] args = epInv.getRequestPayload();
         Object retObj = implMethod.invoke(seiImpl, args);
         epInv.setReturnValue(retObj);
      }
      catch (Exception e)
      {
         handleInvocationException(e);
      }
      finally
      {
         // cleanup thread local
         setTargetBeanObject(null);
      }
   }

   /** Destroy an instance of the SEI implementation bean if necessary */
   public void destroyServiceEndpointInstance(Object seiImpl)
   {
   }
}
