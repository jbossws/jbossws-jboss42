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

import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.spi.deployment.BasicDeployment;
import org.jboss.wsf.spi.deployment.BasicEndpoint;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.WSDeploymentException;

/**
 * An abstract web service deployer.
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public abstract class AbstractDeployerHook implements DeployerHook
{
   // provide logging
   protected final Logger log = Logger.getLogger(getClass());

   protected DeploymentAspectManager deploymentAspectManager;
   private List<ObjectName> phaseOneInterceptors;
   private List<ObjectName> phaseTwoInterceptors;
   private String deploymentClass = BasicDeployment.class.getName();
   private String endpointClass = BasicEndpoint.class.getName();

   public void setDeploymentAspectManager(DeploymentAspectManager manager)
   {
      this.deploymentAspectManager = manager;
   }

   public void setDeploymentClass(String deploymentClass)
   {
      this.deploymentClass = deploymentClass;
   }

   public void setEndpointClass(String endpointClass)
   {
      this.endpointClass = endpointClass;
   }

   public Deployment createDeployment()
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = loader.loadClass(deploymentClass);
         return (Deployment)clazz.newInstance();
      }
      catch (Exception ex)
      {
         throw new WSDeploymentException("Cannot load Deployment class: " + deploymentClass);
      }
   }

   public Endpoint createEndpoint()
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = loader.loadClass(endpointClass);
         return (Endpoint)clazz.newInstance();
      }
      catch (Exception ex)
      {
         throw new WSDeploymentException("Cannot load Endpoint class: " + endpointClass);
      }
   }

   public void setPhaseOneInterceptors(List<ObjectName> phaseOneInterceptors)
   {
      this.phaseOneInterceptors = phaseOneInterceptors;
   }

   public void setPhaseTwoInterceptors(List<ObjectName> phaseTwoInterceptors)
   {
      this.phaseTwoInterceptors = phaseTwoInterceptors;
   }

   /** Return true if this deployment should be ignored
    */
   public boolean ignoreDeployment(DeploymentInfo unit)
   {
      return false;
   }

   /** Add the hooks to the interceptors
    */
   public void start()
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      try
      {
         if (phaseOneInterceptors != null)
         {
            for (ObjectName oname : phaseOneInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.addPhaseOneHook(this);
            }
         }
         
         if (phaseTwoInterceptors != null)
         {
            for (ObjectName oname : phaseTwoInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.addPhaseTwoHook(this);
            }
         }
      }
      catch (MBeanProxyCreationException e)
      {
         throw new WSDeploymentException(e);
      }
   }
   
   /** Add the hooks to the interceptors
    */
   public void stop()
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      try
      {
         if (phaseOneInterceptors != null)
         {
            for (ObjectName oname : phaseOneInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.removePhaseOneHook(this);
            }
         }
         
         if (phaseTwoInterceptors != null)
         {
            for (ObjectName oname : phaseTwoInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.removePhaseTwoHook(this);
            }
         }
      }
      catch (MBeanProxyCreationException e)
      {
         throw new WSDeploymentException(e);
      }
   }
}
