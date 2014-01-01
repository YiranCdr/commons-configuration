/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration.beanutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.convert.ConversionHandler;
import org.apache.commons.configuration.convert.DefaultConversionHandler;
import org.apache.commons.configuration.ex.ConfigurationRuntimeException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for DefaultBeanFactory.
 *
 * @since 1.3
 * @author <a
 * href="http://commons.apache.org/configuration/team-list.html">Commons
 * Configuration team</a>
 * @version $Id$
 */
public class TestDefaultBeanFactory
{
    /** Constant for the test value of the string property. */
    private static final String TEST_STRING = "testString";

    /** Constant for the test value of the numeric property. */
    private static final int TEST_INT = 42;

    /** The object to be tested. */
    private DefaultBeanFactory factory;

    @Before
    public void setUp() throws Exception
    {
        factory = new DefaultBeanFactory();
    }

    /**
     * Creates a bean creation context for a create operation.
     *
     * @param cls the bean class
     * @param decl the bean declaration
     * @return the new creation context
     */
    private static BeanCreationContext createBcc(final Class<?> cls,
            final BeanDeclaration decl)
    {
        return new BeanCreationContext()
        {
            private final BeanHelper beanHelper = new BeanHelper();

            public void initBean(Object bean, BeanDeclaration data)
            {
                beanHelper.initBean(bean, data);
            }

            public Object getParameter()
            {
                return null;
            }

            public BeanDeclaration getBeanDeclaration()
            {
                return decl;
            }

            public Class<?> getBeanClass()
            {
                return cls;
            }

            public Object createBean(BeanDeclaration data)
            {
                return beanHelper.createBean(data);
            }
        };
    }

    /**
     * Tests obtaining the default class. This should be null.
     */
    @Test
    public void testGetDefaultBeanClass()
    {
        assertNull("Default class is not null", factory.getDefaultBeanClass());
    }

    /**
     * Tests whether a correct default conversion handler is set.
     */
    @Test
    public void testDefaultConversionHandler()
    {
        assertSame("Wrong default conversion handler",
                DefaultConversionHandler.INSTANCE,
                factory.getConversionHandler());
    }

    /**
     * Tests whether a custom conversion handler can be passed to the
     * constructor.
     */
    @Test
    public void testInitWithConversionHandler()
    {
        ConversionHandler handler =
                EasyMock.createMock(ConversionHandler.class);
        EasyMock.replay(handler);
        factory = new DefaultBeanFactory(handler);
        assertSame("Wrong conversion handler", handler,
                factory.getConversionHandler());
    }

    /**
     * Tests creating a bean.
     */
    @Test
    public void testCreateBean() throws Exception
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("throwExceptionOnMissing", Boolean.TRUE);
        decl.setBeanProperties(props);
        Object bean = factory.createBean(createBcc(PropertiesConfiguration.class, decl));
        assertNotNull("New bean is null", bean);
        assertEquals("Bean is of wrong class", PropertiesConfiguration.class,
                bean.getClass());
        PropertiesConfiguration config = (PropertiesConfiguration) bean;
        assertTrue("Bean was not initialized", config
                .isThrowExceptionOnMissing());
    }

    /**
     * Tests whether a bean can be created by calling its constructor.
     */
    @Test
    public void testCreateBeanConstructor() throws Exception
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forValue("test"));
        args.add(ConstructorArg.forValue("42"));
        decl.setConstructorArgs(args);
        BeanCreationTestCtorBean bean =
                (BeanCreationTestCtorBean) factory.createBean(createBcc(
                        BeanCreationTestCtorBean.class, decl));
        assertEquals("Wrong string property", "test", bean.getStringValue());
        assertEquals("Wrong int property", 42, bean.getIntValue());
    }

    /**
     * Tests whether nested bean declarations in constructor arguments are taken
     * into account.
     */
    @Test
    public void testCreateBeanConstructorNestedBean() throws Exception
    {
        BeanDeclarationTestImpl declNested = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forValue("test", String.class.getName()));
        declNested.setConstructorArgs(args);
        declNested.setBeanClassName(BeanCreationTestCtorBean.class.getName());
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        decl.setConstructorArgs(Collections.singleton(ConstructorArg
                .forBeanDeclaration(declNested,
                        BeanCreationTestBean.class.getName())));
        BeanCreationTestCtorBean bean =
                (BeanCreationTestCtorBean) factory.createBean(createBcc(
                        BeanCreationTestCtorBean.class, decl));
        assertNotNull("Buddy bean was not set", bean.getBuddy());
        assertEquals("Wrong property of buddy bean", "test", bean.getBuddy()
                .getStringValue());
    }

    /**
     * Tests whether the standard constructor can be found.
     */
    @Test
    public void testFindMatchingConstructorNoArgs()
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Constructor<BeanCreationTestBean> ctor =
                DefaultBeanFactory.findMatchingConstructor(BeanCreationTestBean.class, decl);
        assertEquals("Not the standard constructor", 0,
                ctor.getParameterTypes().length);
    }

    /**
     * Tests whether a matching constructor is found if the number of arguments
     * is unique.
     */
    @Test
    public void testFindMatchingConstructorArgCount()
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forValue(TEST_STRING));
        args.add(ConstructorArg.forValue(String.valueOf(TEST_INT)));
        decl.setConstructorArgs(args);
        Constructor<BeanCreationTestCtorBean> ctor =
                DefaultBeanFactory.findMatchingConstructor(BeanCreationTestCtorBean.class, decl);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        assertEquals("Wrong number of parameters", 2, paramTypes.length);
        assertEquals("Wrong parameter type 1", String.class, paramTypes[0]);
        assertEquals("Wrong parameter type 2", Integer.TYPE, paramTypes[1]);
    }

    /**
     * Tests whether ambiguous constructor arguments are detected.
     */
    @Test(expected = ConfigurationRuntimeException.class)
    public void testFindMatchingConstructorAmbiguous()
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forValue(TEST_STRING));
        decl.setConstructorArgs(args);
        DefaultBeanFactory.findMatchingConstructor(BeanCreationTestCtorBean.class, decl);
    }

    /**
     * Tests whether explicit type declarations are used to resolve ambiguous
     * parameter types.
     */
    @Test
    public void testFindMatchingConstructorExplicitType()
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forBeanDeclaration(setUpBeanDeclaration(),
                BeanCreationTestBean.class.getName()));
        decl.setConstructorArgs(args);
        Constructor<BeanCreationTestCtorBean> ctor =
                DefaultBeanFactory.findMatchingConstructor(BeanCreationTestCtorBean.class, decl);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        assertEquals("Wrong number of parameters", 1, paramTypes.length);
        assertEquals("Wrong parameter type", BeanCreationTestBean.class, paramTypes[0]);
    }

    /**
     * Returns an initialized bean declaration.
     *
     * @return the bean declaration
     */
    private static BeanDeclarationTestImpl setUpBeanDeclaration()
    {
        BeanDeclarationTestImpl data = new BeanDeclarationTestImpl();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("stringValue", TEST_STRING);
        properties.put("intValue", String.valueOf(TEST_INT));
        data.setBeanProperties(properties);
        BeanDeclarationTestImpl buddyData = new BeanDeclarationTestImpl();
        Map<String, Object> properties2 = new HashMap<String, Object>();
        properties2.put("stringValue", "Another test string");
        properties2.put("intValue", new Integer(100));
        buddyData.setBeanProperties(properties2);
        buddyData.setBeanClassName(BeanCreationTestBean.class.getName());

        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("buddy", buddyData);
        data.setNestedBeanDeclarations(nested);
        return data;
    }

    /**
     * Tests the case that no matching constructor is found.
     */
    @Test
    public void testFindMatchingConstructorNoMatch()
    {
        BeanDeclarationTestImpl decl = new BeanDeclarationTestImpl();
        Collection<ConstructorArg> args = new ArrayList<ConstructorArg>();
        args.add(ConstructorArg.forValue(TEST_STRING, getClass().getName()));
        decl.setConstructorArgs(args);
        try
        {
            DefaultBeanFactory.findMatchingConstructor(BeanCreationTestCtorBean.class, decl);
            fail("No exception thrown!");
        }
        catch (ConfigurationRuntimeException crex)
        {
            String msg = crex.getMessage();
            assertTrue("Bean class not found:" + msg,
                    msg.indexOf(BeanCreationTestCtorBean.class.getName()) > 0);
            assertTrue("Parameter value not found: " + msg,
                    msg.indexOf(TEST_STRING) > 0);
            assertTrue("Parameter type not found: " + msg,
                    msg.indexOf("(" + getClass().getName() + ')') > 0);
        }
    }
}
