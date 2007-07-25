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

import org.jboss.wsf.spi.invocation.*;

import javax.xml.ws.handler.MessageContext;

/**
 * @author Heiko.Braun@jboss.com
 *         Created: Jul 19, 2007
 */
public class InvocationModelFactoryImpl extends InvocationModelFactory
{
   public InvocationHandler createInvocationHandler(InvocationType type)
   {
     InvocationHandler handler = null;

      switch(type)
      {
         case JAXRPC_JSE:
            handler = new DefaultInvocationHandlerJAXRPC();
            break;
         case JAXRPC_EJB21:
            handler = new InvocationHandlerEJB21();
            break;
         case JAXRPC_MDB21:
            handler = new InvocationHandlerMDB21();
            break;
         case JAXWS_JSE:
            handler = new DefaultInvocationHandlerJAXWS();
            break;
         case JAXWS_EJB21:
            handler = new InvocationHandlerEJB21();
            break;
         case JAXWS_EJB3:
            handler = new InvocationHandlerEJB3();
      }

      if(null == handler)
         throw new IllegalArgumentException("Unable to resolve spi.invocation.InvocationHandler for type " +type);

      return handler;
   }
   
   public ExtendableWebServiceContext createWebServiceContext(InvocationType type, MessageContext messageContext)
   {
      ExtendableWebServiceContext context = null;

      if(type.toString().indexOf("EJB")!=-1 || type.toString().indexOf("MDB")!=-1)
         context = new WebServiceContextEJB(messageContext);
      else
         context = new WebServiceContextJSE(messageContext);

      return context;
   }
}
