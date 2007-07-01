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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.J2eeApplicationMetaData;
import org.jboss.deployment.J2eeModuleMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.WebSecurityMetaData;
import org.jboss.metadata.WebSecurityMetaData.WebResourceCollection;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebMetaData.PublishLocationAdapter;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebSecurityMetaData.UnifiedWebResourceCollection;

/**
 * Build container independent web meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class WebMetaDataAdapter
{
   public UnifiedWebMetaData buildUnifiedWebMetaData(Deployment dep, UnifiedDeploymentInfo udi, DeploymentInfo di)
   {
      String contextRoot = null;
      
      WebMetaData wmd = (WebMetaData)di.metaData;
      dep.getContext().addAttachment(WebMetaData.class, wmd);
      
      if (di.parent != null)
      {
         J2eeApplicationMetaData appmd = (J2eeApplicationMetaData)di.parent.metaData;
         Iterator it = appmd.getModules();
         while (it.hasNext())
         {
            J2eeModuleMetaData module = (J2eeModuleMetaData)it.next();
            if (module.getFileName().equals(udi.simpleName))
               contextRoot = module.getWebContext();
         }
      }
      
      if (contextRoot == null)
         contextRoot = wmd.getContextRoot();
      
      UnifiedWebMetaData umd = new UnifiedWebMetaData();
      umd.setContextRoot(contextRoot);
      umd.setServletMappings(wmd.getServletMappings());
      umd.setServletClassNames(getServletClassMap(wmd));
      umd.setConfigName(wmd.getConfigName());
      umd.setConfigFile(wmd.getConfigFile());
      umd.setSecurityDomain(wmd.getSecurityDomain());
      umd.setPublishLocationAdapter(getPublishLocationAdpater(wmd));
      umd.setSecurityMetaData(getSecurityMetaData(wmd.getSecurityContraints()));

      dep.getContext().addAttachment(UnifiedWebMetaData.class, umd);
      return umd;
   }

   private PublishLocationAdapter getPublishLocationAdpater(final WebMetaData wmd)
   {
      return new PublishLocationAdapter()
      {
         public String getWsdlPublishLocationByName(String name)
         {
            return wmd.getWsdlPublishLocationByName(name);
         }
      };
   }

   private List<UnifiedWebSecurityMetaData> getSecurityMetaData(final Iterator securityConstraints)
   {
      ArrayList<UnifiedWebSecurityMetaData> unifiedsecurityMetaData = new ArrayList<UnifiedWebSecurityMetaData>();

      while (securityConstraints.hasNext())
      {
         WebSecurityMetaData securityMetaData = (WebSecurityMetaData)securityConstraints.next();

         UnifiedWebSecurityMetaData current = new UnifiedWebSecurityMetaData();
         unifiedsecurityMetaData.add(current);

         current.setTransportGuarantee(securityMetaData.getTransportGuarantee());

         HashMap resources = securityMetaData.getWebResources();
         for (Object webResourceObj : resources.values())
         {
            WebResourceCollection webResource = (WebResourceCollection)webResourceObj;
            UnifiedWebResourceCollection currentResource = current.addWebResource(webResource.getName());
            for (String currentPattern : webResource.getUrlPatterns())
            {
               currentResource.addPattern(currentPattern);
            }
         }

      }

      return unifiedsecurityMetaData;
   }

   private Map<String, String> getServletClassMap(WebMetaData wmd)
   {
      Map<String, String> mappings = new HashMap<String, String>();
      Iterator it = wmd.getServletClassMap().entrySet().iterator();
      while (it.hasNext())
      {
         Map.Entry entry = (Entry)it.next();
         String servletName = (String)entry.getKey();
         String servletClass = (String)entry.getValue();
         // Skip JSPs
         if (servletClass != null)
            mappings.put(servletName, servletClass);
      }
      return mappings;
   }
}
