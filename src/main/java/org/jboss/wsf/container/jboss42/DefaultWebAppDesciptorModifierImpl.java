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
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.logging.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Modifies web.xml for jbossws
 *
 * @author Thomas.Diesler@jboss.org
 * @since 19-May-2006
 */
public class DefaultWebAppDesciptorModifierImpl implements WebAppDesciptorModifier
{
   // logging support
   private static Logger log = Logger.getLogger(DefaultWebAppDesciptorModifierImpl.class);
   private static final List FORBIDDEN_CONTEXT_PARAMETER_NAMES = java.util.Arrays.asList(
         new String[]{"jbossws-sun-jaxws-url"}
   );

   public RewriteResults modifyDescriptor(Deployment dep, Document webXml) throws ClassNotFoundException
   {
      RewriteResults results = new RewriteResults();
      Element root = webXml.getRootElement();

      String propKey = "org.jboss.ws.webapp.ServletClass";
      String servletClass = (String)dep.getProperty(propKey);
      if (servletClass == null)
         throw new IllegalStateException("Cannot obtain context property: " + propKey);

      propKey = "org.jboss.ws.webapp.ContextParameterMap";
      Map<String, String> contextParams = (Map<String, String>)dep.getProperty(propKey);
      if (contextParams != null)
      {
         for (Map.Entry<String, String> entry : contextParams.entrySet())
         {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            // JBWS-2243
            if (false == FORBIDDEN_CONTEXT_PARAMETER_NAMES.contains(paramName))
            {
               Element contextParam = root.addElement("context-param");
               contextParam.addElement("param-name").addText(paramName);
               contextParam.addElement("param-value").addText(paramValue);
            }
         }
      }

      propKey = "org.jboss.ws.webapp.ServletContextListener";
      String listenerClass = (String)dep.getProperty(propKey);
      if (listenerClass != null)
      {
         Element listener = root.addElement("listener");
         listener.addElement("listener-class").setText(listenerClass);
      }

      for (Iterator it = root.elementIterator("servlet"); it.hasNext();)
      {
         Element servlet = (Element)it.next();
         String linkName = servlet.element("servlet-name").getTextTrim();

         // find the servlet-class
         Element classElement = servlet.element("servlet-class");

         // JSP
         if (classElement == null)
            continue;

         String orgServletClassName = classElement.getTextTrim();

         // Get the servlet class
         Class orgServletClass = null;
         try
         {
            ClassLoader loader = dep.getInitialClassLoader();
            orgServletClass = loader.loadClass(orgServletClassName);
         }
         catch (ClassNotFoundException ex)
         {
            log.warn("Cannot load servlet class: " + orgServletClassName);
         }

         String targetBeanName = null;

         // Nothing to do if we have an <init-param>
         if (isAlreadyModified(servlet))
         {
            for (Iterator itParam = servlet.elementIterator("init-param"); itParam.hasNext();)
            {
               Element elParam = (Element)itParam.next();
               String paramName = elParam.element("param-name").getTextTrim();
               String paramValue = elParam.element("param-value").getTextTrim();
               if (Endpoint.SEPID_DOMAIN_ENDPOINT.equals(paramName))
               {
                  targetBeanName = paramValue;
               }
            }
         }
         else
         {
            // Check if it is a real servlet that we can ignore
            if (orgServletClass != null && javax.servlet.Servlet.class.isAssignableFrom(orgServletClass))
            {
               log.info("Ignore servlet: " + orgServletClassName);
               continue;
            }
            else if (orgServletClassName.endsWith("Servlet"))
            {
               log.info("Ignore <servlet-class> that ends with 'Servlet': " + orgServletClassName);
               continue;
            }

            classElement.setText(servletClass);

            // add additional init params
            if (orgServletClassName.equals(servletClass) == false)
            {
               targetBeanName = orgServletClassName;
               Element paramElement = servlet.addElement("init-param");
               paramElement.addElement("param-name").addText(Endpoint.SEPID_DOMAIN_ENDPOINT);
               paramElement.addElement("param-value").addText(targetBeanName);
            }
         }

         if (targetBeanName == null)
            throw new IllegalStateException("Cannot obtain service endpoint bean for: " + linkName);

         // remember the target bean name
         results.sepTargetMap.put(linkName, targetBeanName);
      }

      return results;
   }

   // Return true if the web.xml is already modified
   private boolean isAlreadyModified(Element servlet)
   {
      for (Iterator it = servlet.elementIterator("init-param"); it.hasNext();)
      {
         Element elParam = (Element)it.next();
         String paramName = elParam.element("param-name").getTextTrim();
         if (Endpoint.SEPID_DOMAIN_ENDPOINT.equals(paramName))
            return true;
      }
      return false;
   }
}
