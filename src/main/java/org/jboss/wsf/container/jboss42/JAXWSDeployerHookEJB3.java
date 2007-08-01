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

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.wsf.framework.deployment.URLLoaderAdapter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;

/**
 * A deployer JAXWS EJB3 Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXWSDeployerHookEJB3 extends AbstractDeployerHookEJB
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXWS_EJB3;
   }

   @Override
   public Deployment createDeployment(DeploymentInfo di)
   {
      ArchiveDeployment dep = newDeployment(di);
      dep.setRootFile(new URLLoaderAdapter(di.localUrl));
      dep.setRuntimeClassLoader(di.ucl);
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      Ejb3ModuleMBean ejb3Module = EJBArchiveMetaDataAdapterEJB3.getEJB3Module(di.deployedObject);
      for (Object manager : ejb3Module.getContainers().values())
      {
         if (manager instanceof EJBContainer)
         {
            EJBContainer container = (EJBContainer)manager;
            if (isWebServiceBean(container))
            {
               String ejbName = container.getEjbName();
               String epBean = container.getBeanClassName();

               // Create the endpoint
               Endpoint ep = newEndpoint(epBean);
               ep.setShortName(ejbName);
               service.addEndpoint(ep);
            }
         }
      }

      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      boolean isWebserviceDeployment = false;

      // Check if the ejb3 contains annotated endpoints
      Ejb3ModuleMBean ejb3Module = EJBArchiveMetaDataAdapterEJB3.getEJB3Module(unit.deployedObject);
      for (Object manager : ejb3Module.getContainers().values())
      {
         if (manager instanceof EJBContainer)
         {
            EJBContainer container = (EJBContainer)manager;
            if (isWebServiceBean(container))
            {
               isWebserviceDeployment = true;
               break;
            }
         }
      }

      return isWebserviceDeployment;
   }

   private boolean isWebServiceBean(EJBContainer container)
   {
      boolean isWebServiceBean = false;
      if (container instanceof StatelessContainer)
      {
         boolean isWebService = container.resolveAnnotation(WebService.class) != null;
         boolean isWebServiceProvider = container.resolveAnnotation(WebServiceProvider.class) != null;
         isWebServiceBean = isWebService || isWebServiceProvider;
      }
      return isWebServiceBean;
   }
}