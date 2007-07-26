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
import org.jboss.wsf.framework.deployment.WebXMLRewriter;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentContext;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebMetaData;

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

   private ApplicationMetaDataAdapterEJB3 applicationMetaDataAdapterEJB3;
   private AbstractApplicationMetaDataAdapter applicationMetaDataAdapterEJB21;
   private WebMetaDataAdapter webMetaDataAdapter;

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

   public void buildDeploymentInfo(Deployment dep, DeploymentInfo di)
   {
      DeploymentContext ctx = dep.getContext();
      
      ctx.addAttachment(DeploymentInfo.class, di);
      ctx.setProperty(ApplicationMetaDataAdapterEJB3.DEPLOYED_OBJECT, di.deployedObject);

      if (di.metaData instanceof WebMetaData)
      {
         UnifiedWebMetaData webMetaData = webMetaDataAdapter.buildUnifiedWebMetaData(dep, di);
         if (webMetaData != null)
            ctx.addAttachment(UnifiedWebMetaData.class, webMetaData);
         
         ctx.setProperty(WebXMLRewriter.WEBAPP_URL, getDeploymentURL(di));
      }
      else if (dep.getType() == DeploymentType.JAXRPC_EJB3 || dep.getType() == DeploymentType.JAXWS_EJB3)
      {
         UnifiedApplicationMetaData appMetaData = applicationMetaDataAdapterEJB3.buildUnifiedApplicationMetaData(dep);
         if (appMetaData != null)
            ctx.addAttachment(UnifiedApplicationMetaData.class, appMetaData);
      }
      else if (di.metaData instanceof ApplicationMetaData)
      {
         UnifiedApplicationMetaData appMetaData = applicationMetaDataAdapterEJB21.buildUnifiedApplicationMetaData(dep, di);
         if (appMetaData != null)
            ctx.addAttachment(UnifiedApplicationMetaData.class, appMetaData);
      }
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
}
