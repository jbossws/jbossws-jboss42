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


import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.common.IOUtils;

import javax.xml.ws.WebServiceException;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;

import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.Document;

/**
 * The rewriter for web.xml
 *
 * @author Thomas.Diesler@jboss.org
 * @since 19-May-2007
 */
public class WebXMLRewriterImpl
{
   private WebAppDesciptorModifier desciptorModifier;

   public WebXMLRewriterImpl()
   {
      this.desciptorModifier = new DefaultWebAppDesciptorModifierImpl();
   }

   public WebAppDesciptorModifier getDesciptorModifier()
   {
      return desciptorModifier;
   }

   public void setDesciptorModifier(WebAppDesciptorModifier desciptorModifier)
   {
      this.desciptorModifier = desciptorModifier;
   }

   public RewriteResults rewriteWebXml(Deployment dep)
   {
      URL warURL = (URL)dep.getProperty("org.jboss.ws.webapp.url");
      File warFile = new File(warURL.getFile());
      if (warFile.isDirectory() == false)
         throw new WebServiceException("Expected a war directory: " + warURL);

      File webXML = new File(warURL.getFile() + "/WEB-INF/web.xml");
      if (webXML.isFile() == false)
         throw new WebServiceException("Cannot find web.xml: " + webXML);

      try
      {
         FileInputStream stream = new FileInputStream(webXML);
         String modifyProperty = (String)dep.getProperty("org.jboss.ws.webapp.modify");
         
         // JBWS 1762
         if ((modifyProperty == null) || (!modifyProperty.equals("false")))
         {
            String suffix = (String)dep.getProperty("org.jboss.ws.webapp.descriptor.suffix");
            if (suffix == null)
               suffix = ".org";
            File orgWebXML = new File(webXML.getCanonicalPath() + suffix);

            // Rename the web.xml
            if (webXML.renameTo(orgWebXML) == false)
               throw new WebServiceException("Cannot rename web.xml: " + orgWebXML);
            
            stream = new FileInputStream(orgWebXML);
         }

         return rewriteWebXml(stream, webXML, dep);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception e)
      {
         throw new WebServiceException(e);
      }
   }

   private RewriteResults rewriteWebXml(InputStream source, File destFile, Deployment dep) throws Exception
   {
      if (destFile == null)
      {
         destFile = File.createTempFile("jbossws-alt-web", "xml", IOUtils.createTempDirectory());
         destFile.deleteOnExit();
      }

      SAXReader reader = new SAXReader();
      Document document = reader.read(source);

      RewriteResults results = desciptorModifier.modifyDescriptor(dep, document);
      results.webXML = destFile.toURL();

      String modifyProperty = (String)dep.getProperty("org.jboss.ws.webapp.modify"); 
      if ((modifyProperty == null) || (!modifyProperty.equals("false")))
      {
         FileOutputStream fos = new FileOutputStream(destFile);
         OutputFormat format = OutputFormat.createPrettyPrint();
         XMLWriter writer = new XMLWriter(fos, format);
         writer.write(document);
         writer.close();
      }

      return results;
   }
}
