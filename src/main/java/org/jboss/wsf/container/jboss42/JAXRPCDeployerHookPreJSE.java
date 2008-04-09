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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.common.URLLoaderAdapter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * A deployer JAXRPC JSE Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXRPCDeployerHookPreJSE extends AbstractDeployerHookJSE
{

   public void undeploy(DeploymentInfo unit)
   {
      // the post hook deals with undeployment 
   }

   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXRPC_JSE;
   }

   /**
    * Create an endpoint for every servlet-link in webservices.xml
    */
   @Override
   public Deployment createDeployment(DeploymentInfo di)
   {
      ArchiveDeployment dep = newDeployment(di);
      dep.setRootFile(new URLLoaderAdapter(di.localUrl));
      dep.setRuntimeClassLoader(null);
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      WebMetaData webMetaData = (WebMetaData)di.metaData;
      if (webMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain web meta data");

      WebservicesMetaData wsMetaData = getWebservicesMetaData(di, "WEB-INF/webservices.xml");
      if (wsMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain webservices meta data");

      // Copy the attachments
      dep.addAttachment(WebservicesMetaData.class, wsMetaData);
      dep.addAttachment(WebMetaData.class, webMetaData);

      for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions())
      {
         for (PortComponentMetaData pcmd : wsd.getPortComponents())
         {
            String servletLink = pcmd.getServletLink();
            if (servletLink == null)
               throw new IllegalStateException("servlet-link cannot be null");

            Servlet servlet = getServletForName(webMetaData, servletLink);
            String servletClass = servlet.getServletClass();

            try
            {
               ClassLoader loader = dep.getInitialClassLoader();
               Class<?> epBean = loader.loadClass(servletClass.trim());

               // If this is a servlet we defer the the bean creation 
               if (javax.servlet.Servlet.class.isAssignableFrom(epBean))
                  servletClass = null;
            }
            catch (ClassNotFoundException ex)
            {
               log.warn("Cannot load servlet class: " + servletClass);
            }

            // Create the endpoint
            Endpoint ep = newEndpoint(servletClass);
            ep.setShortName(servletLink);
            service.addEndpoint(ep);
         }
      }

      return dep;
   }

   private Servlet getServletForName(WebMetaData wmd, String servletLink)
   {
      Iterator it = wmd.getServletClassMap().entrySet().iterator();
      while (it.hasNext())
      {
         Map.Entry entry = (Entry)it.next();
         String servletName = (String)entry.getKey();
         String servletClass = (String)entry.getValue();
         if (servletLink.equals(servletName))
         {
            return new Servlet(servletName, servletClass);
         }
      }
      throw new IllegalStateException("Cannot find servlet for link: " + servletLink);
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      if (super.isWebServiceDeployment(unit) == false
        || unit.context.get("org.jboss.ws.ejbwebapp")!=null) // Reject EJB im-memory deployments)
         return false;

      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit, "WEB-INF/webservices.xml");
      return wsMetaData != null;
   }
}
