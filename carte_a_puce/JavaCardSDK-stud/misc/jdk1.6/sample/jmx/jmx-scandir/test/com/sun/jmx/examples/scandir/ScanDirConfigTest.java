/*
 * ScanDirConfigTest.java
 * JUnit based test
 *
 * Created on 15 juillet 2006, 20:17
 *
 * @(#)ScanDirConfigTest.java	1.2 06/08/02
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

package com.sun.jmx.examples.scandir;

import com.sun.jmx.examples.scandir.config.XmlConfigUtils;
import com.sun.jmx.examples.scandir.config.FileMatch;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import junit.framework.*;
import com.sun.jmx.examples.scandir.config.DirectoryScannerConfig;
import com.sun.jmx.examples.scandir.config.ScanManagerConfig;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import javax.management.*;

/**
 * Unit tests for {@code ScanDirConfig}
 *
 * @author Sun Microsystems, 2006 - All rights reserved.
 */
public class ScanDirConfigTest extends TestCase {
    
    public ScanDirConfigTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ScanDirConfigTest.class);
        
        return suite;
    }

    /**
     * Test of load method, of class com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testLoad() throws Exception {
        System.out.println("load");
        
        final File file = File.createTempFile("testconf",".xml");
        final ScanDirConfig instance = new ScanDirConfig(file.getAbsolutePath());
        final ScanManagerConfig bean = 
                new  ScanManagerConfig("testLoad");
        final DirectoryScannerConfig dir = 
                new DirectoryScannerConfig("tmp");
        dir.setRootDirectory(file.getParent());
        bean.putScan(dir);
        XmlConfigUtils.write(bean,new FileOutputStream(file),false);
        instance.load();
        
        assertEquals(bean,instance.getConfiguration());
        bean.removeScan(dir.getName());
        XmlConfigUtils.write(bean,new FileOutputStream(file),false);
        
        assertNotSame(bean,instance.getConfiguration());
        
        instance.load();
        
        assertEquals(bean,instance.getConfiguration());
        
    }

    /**
     * Test of save method, of class com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testSave() throws Exception {
        System.out.println("save");

        final File file = File.createTempFile("testconf",".xml");
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ScanManagerMXBean manager = ScanManager.register(mbs);
        
        try {
            final ScanDirConfigMXBean instance = 
                    manager.createOtherConfigurationMBean("testSave",file.getAbsolutePath());
            assertTrue(mbs.isRegistered(
                    ScanManager.makeScanDirConfigName("testSave")));
            final ScanManagerConfig bean = 
                new  ScanManagerConfig("testSave");
            final DirectoryScannerConfig dir = 
                new DirectoryScannerConfig("tmp");
            dir.setRootDirectory(file.getParent());
            bean.putScan(dir);
            instance.setConfiguration(bean);
            instance.save();
            final ScanManagerConfig loaded = 
                new XmlConfigUtils(file.getAbsolutePath()).readFromFile();
            assertEquals(instance.getConfiguration(),loaded);
            assertEquals(bean,loaded);
        
            instance.getConfiguration().removeScan("tmp");
            instance.save();
            assertNotSame(loaded,instance.getConfiguration());
            final ScanManagerConfig loaded2 = 
                new XmlConfigUtils(file.getAbsolutePath()).readFromFile();
            assertEquals(instance.getConfiguration(),loaded2);
        } finally {
            manager.close();
            mbs.unregisterMBean(ScanManager.SCAN_MANAGER_NAME);
        }
        final ObjectName all = 
                new ObjectName(ScanManager.SCAN_MANAGER_NAME.getDomain()+":*");
        assertEquals(0,mbs.queryNames(all,null).size());
    }

    /**
     * Test of saveTo method, of class com.sun.jmx.examples.scandir.ScanProfile.
     */
    /*
    public void testSaveTo() throws Exception {
        System.out.println("saveTo");
        
        String filename = "";
        ScanDirConfig instance = null;
        
        instance.saveTo(filename);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */
    
    /**
     * Test of getXmlConfigString method, of class com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testGetXmlConfigString() throws Exception {
        System.out.println("getXmlConfigString");
        
        try {
            final File file = File.createTempFile("testconf",".xml");
            final ScanDirConfig instance = new ScanDirConfig(file.getAbsolutePath());
            final ScanManagerConfig bean = 
                new  ScanManagerConfig("testGetXmlConfigString");
            final DirectoryScannerConfig dir = 
                new DirectoryScannerConfig("tmp");
            dir.setRootDirectory(file.getParent());
            bean.putScan(dir);
            instance.setConfiguration(bean);
            System.out.println("Expected: " + XmlConfigUtils.toString(bean));
            System.out.println("Received: " + 
                    instance.getConfiguration().toString());
            assertEquals(XmlConfigUtils.toString(bean),
                instance.getConfiguration().toString());
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }


    /**
     * Test of addNotificationListener method, of class 
     * com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testAddNotificationListener() throws Exception {
        System.out.println("addNotificationListener");
        
        final File file = File.createTempFile("testconf",".xml");
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ScanManagerMXBean manager = ScanManager.register(mbs);
        
        try {
            final ScanDirConfigMXBean instance =
                TestUtils.makeNotificationEmitter(  
                    manager.createOtherConfigurationMBean("testSave",
                        file.getAbsolutePath()),
                    ScanDirConfigMXBean.class);   
            assertTrue(mbs.isRegistered(
                    ScanManager.makeScanDirConfigName("testSave")));
            DirectoryScannerConfig dir = 
                    instance.addDirectoryScanner("tmp",file.getParent(),".*",0,0);
            
            final BlockingQueue<Notification> queue = 
                    new LinkedBlockingQueue<Notification>();
            final NotificationListener listener = new NotificationListener() {
                public void handleNotification(Notification notification, 
                            Object handback) {
                    queue.add(notification);
                }
            };
            NotificationFilter filter = null;
            Object handback = null;
        
            ((NotificationEmitter)instance).addNotificationListener(listener, 
                    filter, handback);
            
            instance.save();
            final ScanManagerConfig loaded = 
                new XmlConfigUtils(file.getAbsolutePath()).readFromFile();
            assertEquals(instance.getConfiguration(),loaded);

            final ScanManagerConfig newConfig =
                    instance.getConfiguration();
            newConfig.removeScan("tmp");
            instance.setConfiguration(newConfig);
            instance.save();
            assertNotSame(loaded,instance.getConfiguration());
            final ScanManagerConfig loaded2 = 
                new XmlConfigUtils(file.getAbsolutePath()).readFromFile();
            assertEquals(instance.getConfiguration(),loaded2);
            instance.load();
            for (int i=0;i<4;i++) {
                final Notification n = queue.poll(3,TimeUnit.SECONDS);
                assertNotNull(n);
                assertEquals(TestUtils.getObjectName(instance),n.getSource());
                switch(i) {
                    case 0: case 2:
                        assertEquals(ScanDirConfig.NOTIFICATION_SAVED,n.getType());
                        break;
                    case 1:
                        assertEquals(ScanDirConfig.NOTIFICATION_MODIFIED,n.getType());
                        break;
                    case 3:
                        assertEquals(ScanDirConfig.NOTIFICATION_LOADED,n.getType());
                        break;
                    default: break;
                }
            }
        } finally {
            manager.close();
            mbs.unregisterMBean(ScanManager.SCAN_MANAGER_NAME);
        }
        final ObjectName all = 
                new ObjectName(ScanManager.SCAN_MANAGER_NAME.getDomain()+":*");
        assertEquals(0,mbs.queryNames(all,null).size());
    }

    /**
     * Test of getConfigFilename method, of class 
     * com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testGetConfigFilename() throws Exception {
        System.out.println("getConfigFilename");
        
        final File file = File.createTempFile("testconf",".xml");
        final ScanDirConfig instance = new ScanDirConfig(file.getAbsolutePath());
        
        String result = instance.getConfigFilename();
        assertEquals(file.getAbsolutePath(), new File(result).getAbsolutePath());
        
    }

    /**
     * Test of addDirectoryScanner method, of class 
     * com.sun.jmx.examples.scandir.ScanDirConfig.
     */
    public void testAddDirectoryScanner() throws IOException {
        System.out.println("addDirectoryScanner");
        
        System.out.println("save");

        final File file = File.createTempFile("testconf",".xml");
        final ScanDirConfig instance = new ScanDirConfig(file.getAbsolutePath());
        final ScanManagerConfig bean = 
                new  ScanManagerConfig("testSave");
        final DirectoryScannerConfig dir = 
                new DirectoryScannerConfig("tmp");
        dir.setRootDirectory(file.getParent());
        FileMatch filter = new FileMatch();
        filter.setFilePattern(".*");
        dir.setIncludeFiles(new FileMatch[] {
            filter
        });
        instance.setConfiguration(bean);
        instance.addDirectoryScanner(dir.getName(),
                                     dir.getRootDirectory(),
                                     filter.getFilePattern(),
                                     filter.getSizeExceedsMaxBytes(),
                                     0);
        instance.save();
        final ScanManagerConfig loaded = 
                new XmlConfigUtils(file.getAbsolutePath()).readFromFile();
        assertNotNull(loaded.getScan(dir.getName()));
        assertEquals(dir,loaded.getScan(dir.getName()));
        assertEquals(instance.getConfiguration(),loaded);
        assertEquals(instance.getConfiguration().getScan(dir.getName()),dir);
    }
    
}
