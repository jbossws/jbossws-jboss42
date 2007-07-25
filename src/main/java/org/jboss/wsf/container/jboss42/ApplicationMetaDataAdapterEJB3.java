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

// $Id$

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedSessionMetaData;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 14-Apr-2007
 */
public class ApplicationMetaDataAdapterEJB3
{
   // logging support
   private static Logger log = Logger.getLogger(ApplicationMetaDataAdapterEJB3.class);

   public UnifiedApplicationMetaData buildUnifiedApplicationMetaData(Deployment dep, UnifiedDeploymentInfo udi)
   {
      ObjectName deployedObject = (ObjectName)dep.getContext().getProperty("DeployedObject");
      Ejb3ModuleMBean ejb3Module = getEJB3Module(deployedObject);

      ArrayList<UnifiedBeanMetaData> beans = new ArrayList<UnifiedBeanMetaData>();
      for (Object container : ejb3Module.getContainers().values())
      {
         if (container instanceof StatelessContainer)
         {
            StatelessContainer slc = (StatelessContainer)container;
            UnifiedBeanMetaData usmd = new UnifiedSessionMetaData();
            usmd.setEjbName(slc.getEjbName());
            usmd.setEjbClass(slc.getBeanClassName());
            beans.add(usmd);
         }
      }

      UnifiedApplicationMetaData umd = new UnifiedApplicationMetaData();
      umd.setEnterpriseBeans(beans);

      dep.getContext().addAttachment(UnifiedApplicationMetaData.class, umd);
      return umd;
   }

   static Ejb3ModuleMBean getEJB3Module(ObjectName objectName)
   {
      Ejb3ModuleMBean ejb3Module;
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         ejb3Module = (Ejb3ModuleMBean)MBeanProxy.get(Ejb3ModuleMBean.class, objectName, server);
         if (ejb3Module == null)
            throw new WSFDeploymentException("Cannot obtain EJB3 module: " + objectName);

         return ejb3Module;
      }
      catch (MBeanProxyCreationException ex)
      {
         throw new WSFDeploymentException("Cannot obtain proxy to EJB3 module");
      }
   }
}
