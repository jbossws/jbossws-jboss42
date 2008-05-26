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

//$Id$

import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.WSFRuntime;
import org.jboss.wsf.spi.WSFRuntimeLocator;
import org.jboss.wsf.spi.deployment.AbstractExtensible;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentModelFactory;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.http.HttpContext;
import org.jboss.wsf.spi.http.HttpContextFactory;
import org.jboss.wsf.spi.http.HttpServer;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

/**
 * A HTTP Server that uses DeploymentAspects
 *
 * @author Thomas.Diesler@jboss.org
 * @since 07-Jul-2006
 */
public class DeploymentAspectHttpServer extends AbstractExtensible implements HttpServer
{
   private String runtimeName;

   /* MC injected property */
   public void setRuntimeName(String runtimeName)
   {
      this.runtimeName = runtimeName;
   }

   /** Start an instance of this HTTP server */
   public void start()
   {
      // verify required properties
   }

   /** Create an HTTP context */
   public HttpContext createContext(String contextRoot)
   {
      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      HttpContext httpContext = spiProvider.getSPI(HttpContextFactory.class).newHttpContext(this, contextRoot);
      return httpContext;
   }

   /** Publish an JAXWS endpoint to the HTTP server */
   public void publish(HttpContext context, Endpoint endpoint)
   {
      Class implClass = getImplementorClass(endpoint);

      try
      {
         // Get the deployment model factory
         SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
         DeploymentModelFactory depModelFactory = spiProvider.getSPI(DeploymentModelFactory.class);

         // Create/Setup the deployment
         Deployment dep = depModelFactory.newDeployment("endpoint-deployment", implClass.getClassLoader());
         dep.setRuntimeClassLoader(dep.getInitialClassLoader());

         // Create/Setup the service
         Service service = dep.getService();
         service.setContextRoot(context.getContextRoot());

         // Create/Setup the endpoint
         org.jboss.wsf.spi.deployment.Endpoint ep = depModelFactory.newEndpoint(implClass.getName());
         service.addEndpoint(ep);

         // Deploy using deployment aspects
         WSFRuntimeLocator locator = spiProvider.getSPI(WSFRuntimeLocator.class);
         WSFRuntime runtime = locator.locateRuntime(runtimeName);

         runtime.create(dep);
         runtime.start(dep);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new WebServiceException(ex);
      }
   }

   /** Destroys an JAXWS endpoint on the HTTP server */
   public void destroy(HttpContext context, Endpoint endpoint)
   {
      try
      {
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new WebServiceException(ex);
      }
   }

   private Class getImplementorClass(Endpoint endpoint)
   {
      Object implementor = endpoint.getImplementor();
      Class implClass = (implementor instanceof Class ? (Class)implementor : implementor.getClass());
      return implClass;
   }
}