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
package org.jboss.wsf.container.jboss42.serviceref;

// $Id$

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.jboss.ws.integration.ServiceRefMetaData;
import org.jboss.ws.integration.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.w3c.dom.Element;

/**
 * @deprecated
 */
public class DelegatingServiceRefMetaData extends ServiceRefMetaData
{
   UnifiedServiceRefMetaData delegate;
   
   public DelegatingServiceRefMetaData()
   {
      delegate = new UnifiedServiceRefMetaData();
   }
   
   class VirtualFileDelegate implements org.jboss.wsf.spi.deployment.UnifiedVirtualFile
   {
      private UnifiedVirtualFile vfDelegate;
      
      public VirtualFileDelegate(UnifiedVirtualFile vfDelegate)
      {
         this.vfDelegate = vfDelegate;
      }

      public org.jboss.wsf.spi.deployment.UnifiedVirtualFile findChild(String child) throws IOException
      {
         return new VirtualFileDelegate(vfDelegate.findChild(child));
      }

      public URL toURL()
      {
         return vfDelegate.toURL();
      }
   }
   
   public Object getAnnotatedElement()
   {
      return delegate.getAnnotatedElement();
   }

   public String getServiceRefName()
   {
      return delegate.getServiceRefName();
   }

   public void setAnnotatedElement(Object anElement)
   {
      delegate.setAnnotatedElement(anElement);
   }

   public void setServiceRefName(String serviceRefName)
   {
      delegate.setServiceRefName(serviceRefName);
   }

   public void importJBossXml(Element root)
   {
      delegate.importJBossXml(root);
   }

   public void importStandardXml(Element root)
   {
      delegate.importStandardXml(root);
   }

   public boolean isProcessed()
   {
      return delegate.isProcessed();
   }

   public void merge(ServiceRefMetaData sref)
   {
      UnifiedServiceRefMetaData usref = ((DelegatingServiceRefMetaData)sref).delegate;
      delegate.merge(usref);
   }

   public void setProcessed(boolean flag)
   {
      delegate.setProcessed(flag);
   }

   public String getServiceInterface()
   {
      return delegate.getServiceInterface();
   }

   public String getServiceRefType()
   {
      return delegate.getServiceRefType();
   }

   public String getMappingFile()
   {
      return delegate.getMappingFile();
   }

   public void setVfsRoot(UnifiedVirtualFile vfsRoot)
   {
      delegate.setVfsRoot(new VirtualFileDelegate(vfsRoot));
   }
   
   public void setConfigFile(String configFile)
   {
      delegate.setConfigFile(configFile);
   }

   public void setConfigName(String configName)
   {
      delegate.setConfigName(configName);
   }

   public void setMappingFile(String mappingFile)
   {
      delegate.setMappingFile(mappingFile);
   }

   public void setServiceImplClass(String serviceImplClass)
   {
      delegate.setServiceImplClass(serviceImplClass);
   }

   public void setServiceInterface(String serviceInterface)
   {
      delegate.setServiceInterface(serviceInterface);
   }

   public void setServiceQName(QName serviceQName)
   {
      delegate.setServiceQName(serviceQName);
   }

   public void setServiceRefType(String serviceResType)
   {
      delegate.setServiceRefType(serviceResType);
   }

   public void setWsdlFile(String wsdlFile)
   {
      delegate.setWsdlFile(wsdlFile);
   }

   public void setWsdlOverride(String wsdlOverride)
   {
      delegate.setWsdlOverride(wsdlOverride);
   }

   public void setHandlerChain(String handlerChain)
   {
      delegate.setHandlerChain(handlerChain);
   }

   public void addPortComponentRef(DelegatingPortComponentRefMetaData data)
   {
      delegate.addPortComponentRef(data.delegate);
   }

   public void addHandler(DelegatingHandlerMetaData data)
   {
      delegate.addHandler(data.delegate);
   }

   public void setHandlerChains(DelegatingHandlerChainsMetaData data)
   {
      delegate.setHandlerChains(data.delegate);
   }

   public void addCallProperty(DelegatingCallPropertyMetaData data)
   {
      delegate.addCallProperty(data.delegate);
   }
}
