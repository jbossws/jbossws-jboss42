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

//$Id: UnifiedDeploymentInfoDeployer.java 3407 2007-06-03 16:06:36Z thomas.diesler@jboss.com $

import org.jboss.deployment.DeploymentInfo;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * A deployer that builds the UnifiedDeploymentInfo 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class ContainerMetaDataDeploymentAspect extends DeploymentAspect
{
   private ContainerMetaDataAdapter metaDataAdapter;

   public void setMetaDataAdapter(ContainerMetaDataAdapter adapter)
   {
      this.metaDataAdapter = adapter;
   }

   @Override
   public void create(Deployment dep)
   {
      DeploymentInfo di = dep.getAttachment(DeploymentInfo.class);
      if (di == null)
         throw new IllegalStateException("Cannot obtain deployment info");

      metaDataAdapter.buildContainerMetaData(dep, di);
   }
}