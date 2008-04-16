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

import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.DeploymentException;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * @author Heiko.Braun@jboss.com
 * @version $Revision$
 */
public abstract class DeployerHookPostJSE extends AbstractDeployerHookJSE
{
   /**
    * The deployment should be created in phase 1.
    */
   @Override
   public Deployment createDeployment(DeploymentInfo di)
   {
      Deployment deployment = (Deployment)di.context.get(Deployment.class);
      if (null == deployment)
         throw new IllegalArgumentException("spi.Deployment missing. Should be created in Phase 1");

      return deployment;
   }

   /**
    * A phase 2 deployer hook needs to reject first-place
    * JSE deployments and wait for those that are re-written.
    * We rely on the fact that spi.Deployment is created in phase 1.    
    */
   @Override
   public boolean isWebServiceDeployment(DeploymentInfo di)
   {
      if (super.isWebServiceDeployment(di) == false)
         return false;

      Deployment deployment = (Deployment)di.context.get(Deployment.class);
      return deployment != null;
   }

}
