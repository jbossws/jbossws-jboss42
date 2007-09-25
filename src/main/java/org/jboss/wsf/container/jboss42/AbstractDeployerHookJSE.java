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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;

//$Id$

/**
 * An abstract deployer for JSE Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public abstract class AbstractDeployerHookJSE extends ArchiveDeployerHook
{
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      if ((unit.metaData instanceof WebMetaData) == false)
         return false;

      return true;
   }
   
   static class Servlet
   {
      String servletName;
      String servletClass;
      public Servlet(String servletName, String servletClass)
      {
         this.servletName = servletName;
         this.servletClass = servletClass;
      }
      public String getServletClass()
      {
         return servletClass;
      }
      public String getServletName()
      {
         return servletName;
      }
   }
   
   // JBWS 1762
   Map<String, String> getServletClassMap(URL resource)
   {
      if (resource == null)
         return null;
      
      File origWebXml = new File(resource.getFile());
      if (origWebXml.isDirectory())
         return null;

      Map<String, String> retVal = new HashMap<String, String>();
      try
      {
         FileInputStream source = new FileInputStream(origWebXml);
         SAXReader reader = new SAXReader();
         Document document = reader.read(source);
         Element root = document.getRootElement();
         for (Iterator it = root.elementIterator("servlet"); it.hasNext();)
         {
            Element servlet = (Element)it.next();
            String servletName = servlet.element("servlet-name").getTextTrim();
            Element servletClass = servlet.element("servlet-class");
            retVal.put(servletName, servletClass == null ? null : servletClass.getTextTrim());
         }
      }
      catch (Exception ignore)
      {
         return null;
      }
      
      return retVal.size() > 0 ? retVal : null;
   }

}
