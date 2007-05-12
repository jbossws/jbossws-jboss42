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

// $Id$

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.WebSecurityMetaData;
import org.jboss.metadata.WebSecurityMetaData.WebResourceCollection;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.metadata.j2ee.UnifiedWebMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedWebSecurityMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedWebMetaData.PublishLocationAdapter;
import org.jboss.ws.metadata.j2ee.UnifiedWebSecurityMetaData.UnifiedWebResourceCollection;

/**
 * Build container independent web meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class WebMetaDataAdapter
{
   public UnifiedWebMetaData buildUnifiedWebMetaData(UnifiedDeploymentInfo udi, WebMetaData webMetaData)
   {
      udi.addAttachment(WebMetaData.class, webMetaData);

      UnifiedWebMetaData umd = new UnifiedWebMetaData();
      umd.setContextRoot(webMetaData.getContextRoot());
      umd.setServletMappings(webMetaData.getServletMappings());
      umd.setServletClassNames(getServletClassMap(webMetaData));
      umd.setConfigName(webMetaData.getConfigName());
      umd.setConfigFile(webMetaData.getConfigFile());
      umd.setSecurityDomain(webMetaData.getSecurityDomain());
      umd.setPublishLocationAdapter(getPublishLocationAdpater(webMetaData));
      umd.setSecurityMetaData(getSecurityMetaData(webMetaData.getSecurityContraints()));

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
