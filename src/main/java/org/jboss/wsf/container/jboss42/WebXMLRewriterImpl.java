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
import java.io.FileOutputStream;
import java.net.URL;

import javax.xml.ws.WebServiceException;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jboss.util.xml.JBossEntityResolver;
import org.jboss.wsf.common.IOUtils;
import org.jboss.wsf.spi.deployment.Deployment;

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
         // After redeployment there might be a stale copy of the original web.xml.org, we delete it
         File orgWebXML = new File(webXML.getCanonicalPath() + ".org");
         orgWebXML.delete();

         // Rename the web.xml
         if (webXML.renameTo(orgWebXML) == false)
            throw new WebServiceException("Cannot rename web.xml: " + orgWebXML);

         return rewriteWebXml(orgWebXML, webXML, dep);
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

   private RewriteResults rewriteWebXml(File input, File destFile, Deployment dep) throws Exception
   {
      if (destFile == null)
      {
         destFile = File.createTempFile("jbossws-alt-web", "xml", IOUtils.createTempDirectory());
         destFile.deleteOnExit();
      }

      FileInputStream inputStream = null;
      FileOutputStream outputStream = null;

      try
      {
         SAXReader reader = new SAXReader();
         reader.setEntityResolver(new JBossEntityResolver());
         
         inputStream = new FileInputStream(input);
         Document document = reader.read(inputStream);

         RewriteResults results = desciptorModifier.modifyDescriptor(dep, document);
         results.webXML = destFile.toURL();

         outputStream = new FileOutputStream(destFile);
         OutputFormat format = OutputFormat.createPrettyPrint();
         XMLWriter writer = new XMLWriter(outputStream, format);
         writer.write(document);
         writer.close();

         return results;
      }
      finally
      {
         if(inputStream!=null)
            inputStream.close();

         if(outputStream!=null)
            outputStream.close();
      }
   }
}
