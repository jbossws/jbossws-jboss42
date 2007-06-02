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
import java.util.Iterator;
import java.util.List;

import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.BeanMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData.PublishLocationAdapter;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public abstract class AbstractApplicationMetaDataAdapter
{
   public UnifiedApplicationMetaData buildUnifiedApplicationMetaData(Deployment dep, UnifiedDeploymentInfo udi, ApplicationMetaData apmd)
   {
      dep.getContext().addAttachment(ApplicationMetaData.class, apmd);

      UnifiedApplicationMetaData umd = new UnifiedApplicationMetaData();
      buildUnifiedBeanMetaData(umd, apmd);
      umd.setConfigName(apmd.getConfigName());
      umd.setConfigFile(apmd.getConfigFile());
      umd.setWebServiceContextRoot(apmd.getWebServiceContextRoot());
      umd.setSecurityDomain(apmd.getSecurityDomain());
      umd.setPublishLocationAdapter(getPublishLocationAdpater(apmd));

      dep.getContext().addAttachment(UnifiedApplicationMetaData.class, umd);
      return umd;
   }

   protected PublishLocationAdapter getPublishLocationAdpater(final ApplicationMetaData apmd)
   {
      return new PublishLocationAdapter()
      {
         public String getWsdlPublishLocationByName(String name)
         {
            return apmd.getWsdlPublishLocationByName(name);
         }
      };
   }

   protected void buildUnifiedBeanMetaData(UnifiedApplicationMetaData umd, ApplicationMetaData metaData)
   {
      List<UnifiedBeanMetaData> beans = new ArrayList<UnifiedBeanMetaData>();
      Iterator it = metaData.getEnterpriseBeans();
      while (it.hasNext())
      {
         BeanMetaData bmd = (BeanMetaData)it.next();
         UnifiedBeanMetaData ubmd = buildUnifiedBeanMetaData(bmd);
         if (ubmd != null)
         {
            beans.add(ubmd);
         }
      }
      umd.setEnterpriseBeans(beans);
   }

   protected abstract UnifiedBeanMetaData buildUnifiedBeanMetaData(BeanMetaData bmd);
}
