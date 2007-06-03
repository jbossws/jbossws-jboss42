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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.ws.integration.ResourceLoaderAdapter;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;

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
   
   private ApplicationMetaDataAdapterEJB3 applicationMetaDataAdapterEJB3 = new ApplicationMetaDataAdapterEJB3();
   private AbstractApplicationMetaDataAdapter applicationMetaDataAdapterEJB21 = new ApplicationMetaDataAdapterEJB21();
   private WebMetaDataAdapter webMetaDataAdapter = new WebMetaDataAdapter();

   public void setApplicationMetaDataAdapterEJB21(AbstractApplicationMetaDataAdapter adapter)
   {
      this.applicationMetaDataAdapterEJB21 = adapter;
   }

   public void setApplicationMetaDataAdapterEJB3(ApplicationMetaDataAdapterEJB3 adapter)
   {
      this.applicationMetaDataAdapterEJB3 = adapter;
   }

   public void setWebMetaDataAdapter(WebMetaDataAdapter adapter)
   {
      this.webMetaDataAdapter = adapter;
   }

   public UnifiedDeploymentInfo buildDeploymentInfo(Deployment dep, UnifiedDeploymentInfo udi, DeploymentInfo di)
   {
      dep.getContext().addAttachment(DeploymentInfo.class, di);

      if (di.parent != null)
      {
         udi.parent = new UnifiedDeploymentInfo(null);
         buildDeploymentInfo(dep, udi.parent, di.parent);
      }

      udi.vfRoot = new ResourceLoaderAdapter(di.localCl);
      udi.name = di.getCanonicalName();
      udi.simpleName = di.shortName;
      udi.url = getDeploymentURL(di);
      udi.classLoader = di.annotationsCl;
      udi.deployedObject = di.deployedObject;

      buildMetaData(dep, udi, di);

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

   private void buildMetaData(Deployment dep, UnifiedDeploymentInfo udi, DeploymentInfo di)
   {
      if (di.metaData instanceof WebMetaData)
      {
         udi.metaData = webMetaDataAdapter.buildUnifiedWebMetaData(dep, udi, (WebMetaData)di.metaData);
         udi.webappURL = udi.url;
      }
      else if (di.metaData instanceof ApplicationMetaData)
      {
         udi.metaData = applicationMetaDataAdapterEJB21.buildUnifiedApplicationMetaData(dep, udi, (ApplicationMetaData)di.metaData);
      }
      else if (udi.deployedObject != null)
      {
         udi.metaData = applicationMetaDataAdapterEJB3.buildUnifiedApplicationMetaData(dep, udi);
         ;
      }
   }
}
