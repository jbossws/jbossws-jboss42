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

import org.jboss.wsf.spi.WSFRuntime;
import org.jboss.wsf.spi.ComposableRuntime;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.invocation.RequestHandlerFactory;
import org.jboss.wsf.spi.invocation.InvocationHandlerFactory;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.transport.TransportManagerFactory;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;

/**
 * Lazy assembly required because MC 1.0.x doesn't support IOC
 * across different beans configurations.
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class LazyAssemblyWSFRuntime implements WSFRuntime, ComposableRuntime
{

   private boolean assembled;
   private String runtimeName;
   private TransportManagerFactory tmf;
   private RequestHandlerFactory rhf;
   private InvocationHandlerFactory ihf;
   private DeploymentAspectManager dam;
   private EndpointRegistry reg;

   public void setRuntimeName(String runtimeName)
   {
      this.runtimeName = runtimeName;
   }

   public void create(Deployment deployment)
   {
      assemble();
      dam.create(deployment, this);
   }

   public void start(Deployment deployment)
   {
      assemble();
      dam.start(deployment, this);
   }

   public void stop(Deployment deployment)
   {
      assemble();
      dam.stop(deployment, this);
   }

   public void destroy(Deployment deployment)
   {
      assemble();
      dam.destroy(deployment, this);
   }


   public void setTransportManagerFactory(TransportManagerFactory factory)
   {
      this.tmf = factory;
   }

   public TransportManagerFactory getTransportManagerFactory()
   {
      return this.tmf;
   }

   public void setEndpointRegistry(EndpointRegistry endpointRegistry)
   {
      this.reg = endpointRegistry;
   }

   public EndpointRegistry getEndpointRegistry()
   {
      return this.reg;
   }

   public void setDeploymentAspectManager(DeploymentAspectManager deploymentManager)
   {
      this.dam = deploymentManager;
   }

   public DeploymentAspectManager getDeploymentAspectManager()
   {
      return this.dam;
   }

   public void setRequestHandlerFactory(RequestHandlerFactory factory)
   {
      this.rhf = factory;
   }

   public RequestHandlerFactory getRequestHandlerFactory()
   {
      return this.rhf;
   }

   public void setInvocationHandlerFactory(InvocationHandlerFactory factory)
   {
      this.ihf = factory;
   }

   public InvocationHandlerFactory getInvocationHandlerFactory()
   {
      return this.ihf;
   }

   private void assemble()
   {
      if(!assembled)
      {
         SPIProvider provider = SPIProviderResolver.getInstance().getProvider();
         RequestHandlerFactory rhFactory = provider.getSPI(RequestHandlerFactory.class);
         setRequestHandlerFactory(rhFactory);

         assembled = true;
      }
   }
}
