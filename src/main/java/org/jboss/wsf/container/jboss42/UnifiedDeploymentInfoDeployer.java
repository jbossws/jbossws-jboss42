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
import org.jboss.wsf.spi.deployment.AbstractDeployer;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.JAXRPCDeployment;
import org.jboss.wsf.spi.deployment.JAXWSDeployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * A deployer that builds the UnifiedDeploymentInfo 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class UnifiedDeploymentInfoDeployer extends AbstractDeployer
{
   private DeploymentInfoAdapter deploymentInfoAdapter;
   
   public void setDeploymentInfoAdapter(DeploymentInfoAdapter adapter)
   {
      this.deploymentInfoAdapter = adapter;
   }

   @Override
   public void create(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
      {
         DeploymentInfo unit = dep.getContext().getAttachment(DeploymentInfo.class);
         if (unit == null)
            throw new IllegalStateException("Cannot obtain deployment unit");

         DeploymentType type = dep.getType();
         if (type.toString().startsWith("JAXWS"))
         {
            udi = new JAXWSDeployment(type);
            deploymentInfoAdapter.buildDeploymentInfo(dep, udi, unit);
         }
         else
         {
            WebservicesMetaData wsMetaData = dep.getContext().getAttachment(WebservicesMetaData.class);
            udi = new JAXRPCDeployment(type, wsMetaData);
            deploymentInfoAdapter.buildDeploymentInfo(dep, udi, unit);
         }

         dep.getContext().addAttachment(UnifiedDeploymentInfo.class, udi);
      }
   }
}