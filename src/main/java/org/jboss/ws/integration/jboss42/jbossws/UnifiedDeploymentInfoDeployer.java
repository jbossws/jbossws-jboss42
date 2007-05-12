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
package org.jboss.ws.integration.jboss42.jbossws;

//$Id$

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.ws.WSException;
import org.jboss.ws.integration.deployment.AbstractDeployer;
import org.jboss.ws.integration.deployment.Deployment;
import org.jboss.ws.integration.deployment.JAXRPCDeployment;
import org.jboss.ws.integration.deployment.JAXWSDeployment;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.deployment.Deployment.DeploymentType;
import org.jboss.ws.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.ws.metadata.webservices.WebservicesMetaData;

/**
 * A deployer that builds the UnifiedDeploymentInfo 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class UnifiedDeploymentInfoDeployer extends AbstractDeployer
{
   private DeploymentInfoAdapter deploymentInfoAdapter;

   public void setDeploymentInfoAdapter(DeploymentInfoAdapter deploymentInfoAdapter)
   {
      this.deploymentInfoAdapter = deploymentInfoAdapter;
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
            deploymentInfoAdapter.buildDeploymentInfo(udi, unit);
         }
         else
         {
            WebservicesMetaData wsMetaData = dep.getContext().getAttachment(WebservicesMetaData.class);
            udi = new JAXRPCDeployment(type, wsMetaData);
            deploymentInfoAdapter.buildDeploymentInfo(udi, unit);
         }

         dep.getContext().addAttachment(UnifiedDeploymentInfo.class, udi);
      }
   }
}