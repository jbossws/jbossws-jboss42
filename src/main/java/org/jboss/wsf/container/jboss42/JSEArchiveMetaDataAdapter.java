/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSESecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData.PublishLocationAdapter;
import org.jboss.wsf.spi.metadata.j2ee.JSESecurityMetaData.JSEResourceCollection;

/**
 * Build container independent web meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class JSEArchiveMetaDataAdapter
{
   public JSEArchiveMetaData buildUnifiedWebMetaData(Deployment dep, DeploymentInfo di)
   {
      String contextRoot = null;
      
      WebMetaData wmd = (WebMetaData)di.metaData;
      dep.addAttachment(WebMetaData.class, wmd);
      
      if (di.parent != null)
      {
         J2eeApplicationMetaData appmd = (J2eeApplicationMetaData)di.parent.metaData;
         if(appmd!=null) // An ESB case, They deploy a *.war inside *.esb 
         {
            Iterator it = appmd.getModules();
            while (it.hasNext())
            {
               J2eeModuleMetaData module = (J2eeModuleMetaData)it.next();
               if (module.getFileName().equals(dep.getSimpleName()))
                  contextRoot = module.getWebContext();
            }
         }
      }
      
      if (contextRoot == null)
         contextRoot = wmd.getContextRoot();
      
      JSEArchiveMetaData webMetaData = new JSEArchiveMetaData();
      webMetaData.setContextRoot(contextRoot);
      webMetaData.setServletMappings(wmd.getServletMappings());
      webMetaData.setServletClassNames(getServletClassMap(wmd));
      webMetaData.setConfigName(wmd.getConfigName());
      webMetaData.setConfigFile(wmd.getConfigFile());
      Map contextParams = wmd.getContextParams();
      if (contextParams.containsKey("jbossws-config-name"))
         webMetaData.setConfigName((String)contextParams.get("jbossws-config-name"));
      if (contextParams.containsKey("jbossws-config-file"))
         webMetaData.setConfigFile((String)contextParams.get("jbossws-config-file"));
      webMetaData.setSecurityDomain(wmd.getSecurityDomain());
      webMetaData.setPublishLocationAdapter(getPublishLocationAdpater(wmd));
      webMetaData.setSecurityMetaData(getSecurityMetaData(wmd.getSecurityContraints()));
      
      return webMetaData;
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

   private List<JSESecurityMetaData> getSecurityMetaData(final Iterator securityConstraints)
   {
      ArrayList<JSESecurityMetaData> unifiedsecurityMetaData = new ArrayList<JSESecurityMetaData>();

      while (securityConstraints.hasNext())
      {
         WebSecurityMetaData securityMetaData = (WebSecurityMetaData)securityConstraints.next();

         JSESecurityMetaData current = new JSESecurityMetaData();
         unifiedsecurityMetaData.add(current);

         current.setTransportGuarantee(securityMetaData.getTransportGuarantee());

         HashMap resources = securityMetaData.getWebResources();
         for (Object webResourceObj : resources.values())
         {
            WebResourceCollection webResource = (WebResourceCollection)webResourceObj;
            JSEResourceCollection currentResource = current.addWebResource(webResource.getName());
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
