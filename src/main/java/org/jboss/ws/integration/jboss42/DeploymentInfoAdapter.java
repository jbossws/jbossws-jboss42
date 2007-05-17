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
package org.jboss.ws.integration.jboss42;

// $Id$

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.ws.integration.ResourceLoaderAdapter;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.deployment.WSDeploymentException;
import org.jboss.ws.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedBeanMetaData;

/**
 * Build container independent deployment info. 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class DeploymentInfoAdapter
{
   // logging support
   private static Logger log = Logger.getLogger(DeploymentInfoAdapter.class);

   private WebMetaDataAdapter webMetaDataAdapter;
   private AbstractApplicationMetaDataAdapter appMetaDataAdapter;

   public void setAppMetaDataAdapter(AbstractApplicationMetaDataAdapter applicationMetaDataAdapter)
   {
      this.appMetaDataAdapter = applicationMetaDataAdapter;
   }

   public void setWebMetaDataAdapter(WebMetaDataAdapter webMetaDataAdapter)
   {
      this.webMetaDataAdapter = webMetaDataAdapter;
   }

   public UnifiedDeploymentInfo buildDeploymentInfo(UnifiedDeploymentInfo udi, DeploymentInfo di)
   {
      udi.addAttachment(DeploymentInfo.class, di);

      if (di.parent != null)
      {
         udi.parent = new UnifiedDeploymentInfo(null);
         buildDeploymentInfo(udi.parent, di.parent);
      }

      udi.vfRoot = new ResourceLoaderAdapter(di.localCl);
      udi.name = di.getCanonicalName();
      udi.simpleName = di.shortName;
      udi.url = getDeploymentURL(di);
      udi.classLoader = di.annotationsCl;
      udi.deployedObject = di.deployedObject;

      buildMetaData(udi, di.metaData);

      log.debug("UnifiedDeploymentInfo:\n" + udi);
      return udi;
   }

   private URL getDeploymentURL(DeploymentInfo di)
   {
      URL deploymentURL = (di.localUrl != null ? di.localUrl : di.url);
      if ("file".equals(deploymentURL.getProtocol()))
      {
         String path = deploymentURL.getPath();
         if (new File(path).isFile())
         {
            try
            {
               deploymentURL = new URL("jar:file:" + path + "!/");
            }
            catch (MalformedURLException e)
            {
               // ignore
            }
         }
      }
      return deploymentURL;
   }

   private void buildMetaData(UnifiedDeploymentInfo udi, Object metaData)
   {
      if (metaData instanceof WebMetaData)
      {
         udi.metaData = webMetaDataAdapter.buildUnifiedWebMetaData(udi, (WebMetaData)metaData);
         udi.webappURL = udi.url;
      }
      else if (metaData instanceof ApplicationMetaData)
      {
         udi.metaData = appMetaDataAdapter.buildUnifiedApplicationMetaData(udi, (ApplicationMetaData)metaData);
      }
      else if (udi.deployedObject != null)
      {
         Ejb3ModuleMBean ejb3Module = getEJB3Module(udi.deployedObject);

         ArrayList<UnifiedBeanMetaData> beans = new ArrayList<UnifiedBeanMetaData>();
         for (Object container : ejb3Module.getContainers().values())
         {
            if (container instanceof StatelessContainer)
            {
               StatelessContainer slc = (StatelessContainer)container;
               UnifiedBeanMetaData uslc = new UnifiedBeanMetaData();
               uslc.setEjbName(slc.getEjbName());
               uslc.setEjbClass(slc.getBeanClassName());
               beans.add(uslc);
            }
         }

         UnifiedApplicationMetaData appMetaData = new UnifiedApplicationMetaData();
         appMetaData.setEnterpriseBeans(beans);
         udi.metaData = appMetaData;
      }
   }

   public static Ejb3ModuleMBean getEJB3Module(ObjectName objectName)
   {
      Ejb3ModuleMBean ejb3Module;
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         ejb3Module = (Ejb3ModuleMBean)MBeanProxy.get(Ejb3ModuleMBean.class, objectName, server);
         if (ejb3Module == null)
            throw new WSDeploymentException("Cannot obtain EJB3 module: " + objectName);

         return ejb3Module;
      }
      catch (MBeanProxyCreationException ex)
      {
         throw new WSDeploymentException("Cannot obtain proxy to EJB3 module");
      }
   }
}
