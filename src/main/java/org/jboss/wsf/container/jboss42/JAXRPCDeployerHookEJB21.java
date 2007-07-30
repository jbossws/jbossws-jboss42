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

import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.BeanMetaData;
import org.jboss.ws.integration.URLLoaderAdapter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * A deployer JAXRPC EJB21 Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXRPCDeployerHookEJB21 extends AbstractDeployerHookEJB
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXRPC_EJB21;
   }

   @Override
   public Deployment createDeployment(DeploymentInfo di)
   {
      ArchiveDeployment dep = newDeployment(di);
      dep.setRootFile(new URLLoaderAdapter(di.localUrl));
      dep.setRuntimeClassLoader(di.ucl);
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      ApplicationMetaData appmd = (ApplicationMetaData)di.metaData;
      if (appmd == null)
         throw new IllegalStateException("Deployment unit does not contain application meta data");

      WebservicesMetaData wsMetaData = getWebservicesMetaData(di, null);
      if (wsMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain webservices meta data");

      // Copy the attachments
      dep.addAttachment(WebservicesMetaData.class, wsMetaData);
      dep.addAttachment(ApplicationMetaData.class, appmd);

      for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions())
      {
         for (PortComponentMetaData pcmd : wsd.getPortComponents())
         {
            String ejbLink = pcmd.getEjbLink();
            if (ejbLink == null)
               throw new IllegalStateException("ejb-link cannot be null");

            BeanMetaData beanMetaData = appmd.getBeanByEjbName(ejbLink);
            if (beanMetaData == null)
               throw new IllegalStateException("Cannot obtain bean meta data for: " + ejbLink);

            String ejbClass = beanMetaData.getEjbClass();
            
            // Create the endpoint
            Endpoint ep = newEndpoint(ejbLink, ejbClass);
            service.addEndpoint(ep);
         }
      }
      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      if ((unit.metaData instanceof ApplicationMetaData) == false)
         return false;

      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit, "META-INF/webservices.xml");
      return wsMetaData != null;
   }
}