/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.util;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionProviderFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Provides support for localization in XWork.
 * <p/>
 * <!-- START SNIPPET: searchorder -->
 * Resource bundles are searched in the following order:<p/>
 * <p/>
 * <ol>
 * <li>ActionClass.properties</li>
 * <li>Interface.properties (every interface and sub-interface)</li>
 * <li>BaseClass.properties (all the way to Object.properties)</li>
 * <li>ModelDriven's model (if implements ModelDriven), for the model object repeat from 1</li>
 * <li>package.properties (of the directory where class is located and every parent directory all the way to the root directory)</li>
 * <li>search up the i18n message key hierarchy itself</li>
 * <li>global resource properties</li>
 * </ol>
 * <p/>
 * <!-- END SNIPPET: searchorder -->
 * <p/>
 * <!-- START SNIPPET: packagenote -->
 * To clarify #5, while traversing the package hierarchy, Struts 2 will look for a file package.properties:<p/>
 * com/<br/>
 * &nbsp; acme/<br/>
 * &nbsp; &nbsp; package.properties<br/>
 * &nbsp; &nbsp; actions/<br/>
 * &nbsp; &nbsp; &nbsp; package.properties<br/>
 * &nbsp; &nbsp; &nbsp; FooAction.java<br/>
 * &nbsp; &nbsp; &nbsp; FooAction.properties<br/>
 * <p/>
 * If FooAction.properties does not exist, com/acme/action/package.properties will be searched for, if
 * not found com/acme/package.properties, if not found com/package.properties, etc.
 * <p/>
 * <!-- END SNIPPET: packagenote -->
 * <p/>
 * <!-- START SNIPPET: globalresource -->
 * A global resource bundle could be specified programatically, as well as the locale.
 * <p/>
 * <!-- END SNIPPET: globalresource -->
 *
 * @author Jason Carreira
 * @author Mark Woon
 * @author Rainer Hermanns
 * @author tm_jee
 * @version $Date: 2009-07-24 01:46:50 +0800 (星期五, 24 七月 2009) $ $Id: LocalizedTextUtil.java 2026 2009-07-23 17:46:50Z musachy $
 */
public class LocalizedTextUtil {

    private static final List<String> DEFAULT_RESOURCE_BUNDLES = new CopyOnWriteArrayList<String>();
    private static final Logger LOG = LoggerFactory.getLogger(LocalizedTextUtil.class);
    private static boolean reloadBundles = false;
    private static final ResourceBundle EMPTY_BUNDLE = new EmptyResourceBundle();
    private static final ConcurrentMap<String, ResourceBundle> bundlesMap = new ConcurrentHashMap<String, ResourceBundle>();
    private static final Map<MessageFormatKey, MessageFormat> messageFormats = new HashMap<MessageFormatKey, MessageFormat>();

    private static ClassLoader delegatedClassLoader;
    
    static {
        clearDefaultResourceBundles();
    }


    /**
     * Clears the internal list of resource bundles.
     */
    public static void clearDefaultResourceBundles() {
        if (DEFAULT_RESOURCE_BUNDLES != null) {
            synchronized (DEFAULT_RESOURCE_BUNDLES) {
                DEFAULT_RESOURCE_BUNDLES.clear();
                DEFAULT_RESOURCE_BUNDLES.add("com/opensymphony/xwork2/xwork-messages");
            }
        } else {
            synchronized (DEFAULT_RESOURCE_BUNDLES) {
                DEFAULT_RESOURCE_BUNDLES.add("com/opensymphony/xwork2/xwork-messages");
            }
        }
    }

    /**
     * Should resorce bundles be reloaded.
     *
     * @param reloadBundles reload bundles?
     */
    public static void setReloadBundles(boolean reloadBundles) {
        LocalizedTextUtil.reloadBundles = reloadBundles;
    }

    /**
     * Add's the bundle to the internal list of default bundles.
     * <p/>
     * If the bundle already exists in the list it will be readded.
     *
     * @param resourceBundleName the name of the bundle to add.
     */
    public static void addDefaultResourceBundle(String resourceBundleName) {
        //make sure this doesn't get added more than once
        synchronized (DEFAULT_RESOURCE_BUNDLES) {
            DEFAULT_RESOURCE_BUNDLES.remove(resourceBundleName);
            DEFAULT_RESOURCE_BUNDLES.add(0, resourceBundleName);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Added default resource bundle '" + resourceBundleName + "' to default resource bundles = " + DEFAULT_RESOURCE_BUNDLES);
        }
    }

    /**
     * Builds a {@link java.util.Locale} from a String of the form en_US_foo into a Locale
     * with language "en", country "US" and variant "foo". This will parse the output of
     * {@link java.util.Locale#toString()}.
     *
     * @param localeStr     The locale String to parse.
     * @param defaultLocale The locale to use if localeStr is <tt>null</tt>.
     * @return requested Locale
     */
    public static Locale localeFromString(String localeStr, Locale defaultLocale) {
        if ((localeStr == null) || (localeStr.trim().length() == 0) || ("_".equals(localeStr))) {
            if (defaultLocale != null) {
                return defaultLocale;
            }
            return Locale.getDefault();
        }

        int index = localeStr.indexOf('_');
        if (index < 0) {
            return new Locale(localeStr);
        }

        String language = localeStr.substring(0, index);
        if (index == localeStr.length()) {
            return new Locale(language);
        }

        localeStr = localeStr.substring(index + 1);
        index = localeStr.indexOf('_');
        if (index < 0) {
            return new Locale(language, localeStr);
        }

        String country = localeStr.substring(0, index);
        if (index == localeStr.length()) {
            return new Locale(language, country);
        }

        localeStr = localeStr.substring(index + 1);
        return new Locale(language, country, localeStr);
    }

    /**
     * Returns a localized message for the specified key, aTextName.  Neither the key nor the
     * message is evaluated.
     *
     * @param aTextName the message key
     * @param locale    the locale the message should be for
     * @return a localized message based on the specified key, or null if no localized message can be found for it
     */
    public static String findDefaultText(String aTextName, Locale locale) {
        List<String> localList = DEFAULT_RESOURCE_BUNDLES;

        for (String bundleName : localList) {
            ResourceBundle bundle = findResourceBundle(bundleName, locale);
            if (bundle != null) {
                reloadBundles();
                try {
                    return bundle.getString(aTextName);
                } catch (MissingResourceException e) {
                    // ignore and try others
                }
            }
        }

        return null;
    }

    /**
     * Returns a localized message for the specified key, aTextName, substituting variables from the
     * array of params into the message.  Neither the key nor the message is evaluated.
     *
     * @param aTextName the message key
     * @param locale    the locale the message should be for
     * @param params    an array of objects to be substituted into the message text
     * @return A formatted message based on the specified key, or null if no localized message can be found for it
     */
    public static String findDefaultText(String aTextName, Locale locale, Object[] params) {
        String defaultText = findDefaultText(aTextName, locale);
        if (defaultText != null) {
            MessageFormat mf = buildMessageFormat(defaultText, locale);
            return mf.format(params);
        }
        return null;
    }

    /**
     * Finds the given resorce bundle by it's name.
     * <p/>
     * Will use <code>Thread.currentThread().getContextClassLoader()</code> as the classloader.
     * If {@link #delegatedClassLoader} is defined and the bundle cannot be found the current
     * classloader it will delegate to that.
     *
     * @param aBundleName the name of the bundle (usually it's FQN classname).
     * @param locale      the locale.
     * @return the bundle, <tt>null</tt> if not found.
     */
    public static ResourceBundle findResourceBundle(String aBundleName, Locale locale) {
        String key = createMissesKey(aBundleName, locale);

        ResourceBundle bundle;

        try {
            if (!bundlesMap.containsKey(key)) {
                bundle = ResourceBundle.getBundle(
                        aBundleName,
                        locale,
                        Thread.currentThread().getContextClassLoader());
                bundlesMap.put(key, bundle);
            }

            bundle = bundlesMap.get(key);
        } catch (MissingResourceException ex) {
            if ( delegatedClassLoader != null) {
                try {
                    if (!bundlesMap.containsKey(key)) {
                        bundle = ResourceBundle.getBundle(
                                aBundleName,
                                locale,
                                delegatedClassLoader);
                        bundlesMap.put(key, bundle);
                    }

                    bundle = bundlesMap.get(key);
                    
                } catch (MissingResourceException e) {
                    bundle = EMPTY_BUNDLE;
                    bundlesMap.put(key, bundle);
                }
            } else {
                bundle = EMPTY_BUNDLE;
                bundlesMap.put(key, bundle);
            }
        }
        return (bundle == EMPTY_BUNDLE) ? null : bundle;
    }

    /**
     * Sets a {@link ClassLoader} to look up the bundle from if none can be found on the current thread's classloader
     *
     * @param classLoader
     */
    public static void setDelegatedClassLoader(final ClassLoader classLoader) {
        synchronized (bundlesMap) {
            delegatedClassLoader = classLoader;
        }
    }

    /**
     * Removes the bundle from any cached "misses"
     *
     * @param bundleName
     */
    public static void clearBundle(final String bundleName) {
        synchronized (bundlesMap) {
            bundlesMap.remove(bundleName);
        }
    }


    /**
     * Creates a key to used for lookup/storing in the bundle misses cache.
     *
     * @param aBundleName the name of the bundle (usually it's FQN classname).
     * @param locale      the locale.
     * @return the key to use for lookup/storing in the bundle misses cache.
     */
    private static String createMissesKey
            (String
                    aBundleName, Locale
                    locale) {
        return aBundleName + "_" + locale.toString();
    }

    /**
     * Calls {@link #findText(Class aClass, String aTextName, Locale locale, String defaultMessage, Object[] args)}
     * with aTextName as the default message.
     *
     * @see #findText(Class aClass, String aTextName, Locale locale, String defaultMessage, Object[] args)
     */
    public static String findText
            (Class
                    aClass, String
                    aTextName, Locale
                    locale) {
        return findText(aClass, aTextName, locale, aTextName, new Object[0]);
    }

    /**
     * Finds a localized text message for the given key, aTextName. Both the key and the message
     * itself is evaluated as required.  The following algorithm is used to find the requested
     * message:
     * <p/>
     * <ol>
     * <li>Look for message in aClass' class hierarchy.
     * <ol>
     * <li>Look for the message in a resource bundle for aClass</li>
     * <li>If not found, look for the message in a resource bundle for any implemented interface</li>
     * <li>If not found, traverse up the Class' hierarchy and repeat from the first sub-step</li>
     * </ol></li>
     * <li>If not found and aClass is a {@link ModelDriven} Action, then look for message in
     * the model's class hierarchy (repeat sub-steps listed above).</li>
     * <li>If not found, look for message in child property.  This is determined by evaluating
     * the message key as an OGNL expression.  For example, if the key is
     * <i>user.address.state</i>, then it will attempt to see if "user" can be resolved into an
     * object.  If so, repeat the entire process fromthe beginning with the object's class as
     * aClass and "address.state" as the message key.</li>
     * <li>If not found, look for the message in aClass' package hierarchy.</li>
     * <li>If still not found, look for the message in the default resource bundles.</li>
     * <li>Return defaultMessage</li>
     * </ol>
     * <p/>
     * When looking for the message, if the key indexes a collection (e.g. user.phone[0]) and a
     * message for that specific key cannot be found, the general form will also be looked up
     * (i.e. user.phone[*]).
     * <p/>
     * If a message is found, it will also be interpolated.  Anything within <code>${...}</code>
     * will be treated as an OGNL expression and evaluated as such.
     *
     * @param aClass         the class whose name to use as the start point for the search
     * @param aTextName      the key to find the text message for
     * @param locale         the locale the message should be for
     * @param defaultMessage the message to be returned if no text message can be found in any
     *                       resource bundle
     * @return the localized text, or null if none can be found and no defaultMessage is provided
     */
    public static String findText
            (Class
                    aClass, String
                    aTextName, Locale
                    locale, String
                    defaultMessage, Object[] args) {
        ValueStack valueStack = ActionContext.getContext().getValueStack();
        return findText(aClass, aTextName, locale, defaultMessage, args, valueStack);

    }

    /**
     * Finds a localized text message for the given key, aTextName. Both the key and the message
     * itself is evaluated as required.  The following algorithm is used to find the requested
     * message:
     * <p/>
     * <ol>
     * <li>Look for message in aClass' class hierarchy.
     * <ol>
     * <li>Look for the message in a resource bundle for aClass</li>
     * <li>If not found, look for the message in a resource bundle for any implemented interface</li>
     * <li>If not found, traverse up the Class' hierarchy and repeat from the first sub-step</li>
     * </ol></li>
     * <li>If not found and aClass is a {@link ModelDriven} Action, then look for message in
     * the model's class hierarchy (repeat sub-steps listed above).</li>
     * <li>If not found, look for message in child property.  This is determined by evaluating
     * the message key as an OGNL expression.  For example, if the key is
     * <i>user.address.state</i>, then it will attempt to see if "user" can be resolved into an
     * object.  If so, repeat the entire process fromthe beginning with the object's class as
     * aClass and "address.state" as the message key.</li>
     * <li>If not found, look for the message in aClass' package hierarchy.</li>
     * <li>If still not found, look for the message in the default resource bundles.</li>
     * <li>Return defaultMessage</li>
     * </ol>
     * <p/>
     * When looking for the message, if the key indexes a collection (e.g. user.phone[0]) and a
     * message for that specific key cannot be found, the general form will also be looked up
     * (i.e. user.phone[*]).
     * <p/>
     * If a message is found, it will also be interpolated.  Anything within <code>${...}</code>
     * will be treated as an OGNL expression and evaluated as such.
     * <p/>
     * If a message is <b>not</b> found a WARN log will be logged.
     *
     * @param aClass         the class whose name to use as the start point for the search
     * @param aTextName      the key to find the text message for
     * @param locale         the locale the message should be for
     * @param defaultMessage the message to be returned if no text message can be found in any
     *                       resource bundle
     * @param valueStack     the value stack to use to evaluate expressions instead of the
     *                       one in the ActionContext ThreadLocal
     * @return the localized text, or null if none can be found and no defaultMessage is provided
     */
    public static String findText
            (Class
                    aClass, String
                    aTextName, Locale
                    locale, String
                    defaultMessage, Object[] args, ValueStack
                    valueStack) {
        String indexedTextName = null;
        if (aTextName == null) {
            LOG.warn("Trying to find text with null key!");
            aTextName = "";
        }
        // calculate indexedTextName (collection[*]) if applicable
        if (aTextName.contains("[")) {
            int i = -1;

            indexedTextName = aTextName;

            while ((i = indexedTextName.indexOf("[", i + 1)) != -1) {
                int j = indexedTextName.indexOf("]", i);
                String a = indexedTextName.substring(0, i);
                String b = indexedTextName.substring(j);
                indexedTextName = a + "[*" + b;
            }
        }

        // search up class hierarchy
        String msg = findMessage(aClass, aTextName, indexedTextName, locale, args, null, valueStack);

        if (msg != null) {
            return msg;
        }

        if (ModelDriven.class.isAssignableFrom(aClass)) {
            ActionContext context = ActionContext.getContext();
            // search up model's class hierarchy
            ActionInvocation actionInvocation = context.getActionInvocation();

            // ActionInvocation may be null if we're being run from a Sitemesh filter, so we won't get model texts if this is null
            if (actionInvocation != null) {
                Object action = actionInvocation.getAction();
                if (action instanceof ModelDriven) {
                    Object model = ((ModelDriven) action).getModel();
                    if (model != null) {
                        msg = findMessage(model.getClass(), aTextName, indexedTextName, locale, args, null, valueStack);
                        if (msg != null) {
                            return msg;
                        }
                    }
                }
            }
        }

        // nothing still? alright, search the package hierarchy now
        for (Class clazz = aClass;
             (clazz != null) && !clazz.equals(Object.class);
             clazz = clazz.getSuperclass()) {

            String basePackageName = clazz.getName();
            while (basePackageName.lastIndexOf('.') != -1) {
                basePackageName = basePackageName.substring(0, basePackageName.lastIndexOf('.'));
                String packageName = basePackageName + ".package";
                msg = getMessage(packageName, locale, aTextName, valueStack, args);

                if (msg != null) {
                    return msg;
                }

                if (indexedTextName != null) {
                    msg = getMessage(packageName, locale, indexedTextName, valueStack, args);

                    if (msg != null) {
                        return msg;
                    }
                }
            }
        }

        // see if it's a child property
        int idx = aTextName.indexOf(".");

        if (idx != -1) {
            String newKey = null;
            String prop = null;

            if (aTextName.startsWith(XWorkConverter.CONVERSION_ERROR_PROPERTY_PREFIX)) {
                idx = aTextName.indexOf(".", XWorkConverter.CONVERSION_ERROR_PROPERTY_PREFIX.length());

                if (idx != -1) {
                    prop = aTextName.substring(XWorkConverter.CONVERSION_ERROR_PROPERTY_PREFIX.length(), idx);
                    newKey = XWorkConverter.CONVERSION_ERROR_PROPERTY_PREFIX + aTextName.substring(idx + 1);
                }
            } else {
                prop = aTextName.substring(0, idx);
                newKey = aTextName.substring(idx + 1);
            }

            if (prop != null) {
                Object obj = valueStack.findValue(prop);
                try {
                    Object actionObj = ReflectionProviderFactory.getInstance().getRealTarget(prop, valueStack.getContext(), valueStack.getRoot());
                    if (actionObj != null) {
                        PropertyDescriptor propertyDescriptor = ReflectionProviderFactory.getInstance().getPropertyDescriptor(actionObj.getClass(), prop);

                        if (propertyDescriptor != null) {
                            Class clazz = propertyDescriptor.getPropertyType();

                            if (clazz != null) {
                                if (obj != null)
                                    valueStack.push(obj);
                                msg = findText(clazz, newKey, locale, null, args);
                                if (obj != null)
                                    valueStack.pop();

                                if (msg != null) {
                                    return msg;
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    LOG.debug("unable to find property " + prop, e);
                }
            }
        }

        // get default
        GetDefaultMessageReturnArg result = null;
        if (indexedTextName == null) {
            result = getDefaultMessage(aTextName, locale, valueStack, args, defaultMessage);
        } else {
            result = getDefaultMessage(aTextName, locale, valueStack, args, null);
            if (result != null && result.message != null) {
                return result.message;
            }
            result = getDefaultMessage(indexedTextName, locale, valueStack, args, defaultMessage);
        }

        // could we find the text, if not log a warn
        if (unableToFindTextForKey(result)) {
            String warn = "Unable to find text for key '" + aTextName + "' ";
            if (indexedTextName != null) {
                warn += " or indexed key '" + indexedTextName + "' ";
            }
            warn += "in class '" + aClass.getName() + "' and locale '" + locale + "'";
            LOG.debug(warn);
        }

        return result != null ? result.message : null;
    }

    /**
     * Determines if we found the text in the bundles.
     *
     * @param result the result so far
     * @return <tt>true</tt> if we could <b>not</b> find the text, <tt>false</tt> if the text was found (=success).
     */
    private static boolean unableToFindTextForKey
            (GetDefaultMessageReturnArg
                    result) {
        if (result == null || result.message == null) {
            return true;
        }

        // did we find it in the bundle, then no problem?
        if (result.foundInBundle) {
            return false;
        }

        // not found in bundle
        return true;
    }

    /**
     * Finds a localized text message for the given key, aTextName, in the specified resource bundle
     * with aTextName as the default message.
     * <p/>
     * If a message is found, it will also be interpolated.  Anything within <code>${...}</code>
     * will be treated as an OGNL expression and evaluated as such.
     *
     * @see #findText(java.util.ResourceBundle, String, java.util.Locale, String, Object[])
     */
    public static String findText
            (ResourceBundle
                    bundle, String
                    aTextName, Locale
                    locale) {
        return findText(bundle, aTextName, locale, aTextName, new Object[0]);
    }

    /**
     * Finds a localized text message for the given key, aTextName, in the specified resource
     * bundle.
     * <p/>
     * If a message is found, it will also be interpolated.  Anything within <code>${...}</code>
     * will be treated as an OGNL expression and evaluated as such.
     * <p/>
     * If a message is <b>not</b> found a WARN log will be logged.
     *
     * @param bundle         the bundle
     * @param aTextName      the key
     * @param locale         the locale
     * @param defaultMessage the default message to use if no message was found in the bundle
     * @param args           arguments for the message formatter.
     */
    public static String findText
            (ResourceBundle
                    bundle, String
                    aTextName, Locale
                    locale, String
                    defaultMessage, Object[] args) {
        ValueStack valueStack = ActionContext.getContext().getValueStack();
        return findText(bundle, aTextName, locale, defaultMessage, args, valueStack);
    }

    /**
     * Finds a localized text message for the given key, aTextName, in the specified resource
     * bundle.
     * <p/>
     * If a message is found, it will also be interpolated.  Anything within <code>${...}</code>
     * will be treated as an OGNL expression and evaluated as such.
     * <p/>
     * If a message is <b>not</b> found a WARN log will be logged.
     *
     * @param bundle         the bundle
     * @param aTextName      the key
     * @param locale         the locale
     * @param defaultMessage the default message to use if no message was found in the bundle
     * @param args           arguments for the message formatter.
     * @param valueStack     the OGNL value stack.
     */
    public static String findText
            (ResourceBundle
                    bundle, String
                    aTextName, Locale
                    locale, String
                    defaultMessage, Object[] args, ValueStack
                    valueStack) {
        try {
            reloadBundles();

            String message = TextParseUtil.translateVariables(bundle.getString(aTextName), valueStack);
            MessageFormat mf = buildMessageFormat(message, locale);

            return mf.format(args);
        } catch (MissingResourceException ex) {
            // ignore
        }

        GetDefaultMessageReturnArg result = getDefaultMessage(aTextName, locale, valueStack, args, defaultMessage);
        if (unableToFindTextForKey(result)) {
            LOG.warn("Unable to find text for key '" + aTextName + "' in ResourceBundles for locale '" + locale + "'");
        }
        return result != null ? result.message : null;
    }

    /**
     * Gets the default message.
     */
    private static GetDefaultMessageReturnArg getDefaultMessage
            (String
                    key, Locale
                    locale, ValueStack
                    valueStack, Object[] args, String
                    defaultMessage) {
        GetDefaultMessageReturnArg result = null;
        boolean found = true;

        if (key != null) {
            String message = findDefaultText(key, locale);

            if (message == null) {
                message = defaultMessage;
                found = false; // not found in bundles
            }

            // defaultMessage may be null
            if (message != null) {
                MessageFormat mf = buildMessageFormat(TextParseUtil.translateVariables(message, valueStack), locale);

                String msg = mf.format(args);
                result = new GetDefaultMessageReturnArg(msg, found);
            }
        }

        return result;
    }

    /**
     * Gets the message from the named resource bundle.
     */
    private static String getMessage
            (String
                    bundleName, Locale
                    locale, String
                    key, ValueStack
                    valueStack, Object[] args) {
        ResourceBundle bundle = findResourceBundle(bundleName, locale);
        if (bundle == null) {
            return null;
        }

        reloadBundles();

        try {
            String message = TextParseUtil.translateVariables(bundle.getString(key), valueStack);
            MessageFormat mf = buildMessageFormat(message, locale);
            return mf.format(args);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static MessageFormat buildMessageFormat
            (String
                    pattern, Locale
                    locale) {
        MessageFormatKey key = new MessageFormatKey(pattern, locale);
        MessageFormat format = null;
        synchronized (messageFormats) {
            format = (MessageFormat) messageFormats.get(key);
            if (format == null) {
                format = new MessageFormat(pattern);
                format.setLocale(locale);
                format.applyPattern(pattern);
                messageFormats.put(key, format);
            }
        }

        return format;
    }

    /**
     * Traverse up class hierarchy looking for message.  Looks at class, then implemented interface,
     * before going up hierarchy.
     */
    private static String findMessage
            (Class
                    clazz, String
                    key, String
                    indexedKey, Locale
                    locale, Object[] args, Set<String>
                    checked, ValueStack
                    valueStack) {
        if (checked == null) {
            checked = new TreeSet<String>();
        } else if (checked.contains(clazz.getName())) {
            return null;
        }

        // look in properties of this class
        String msg = getMessage(clazz.getName(), locale, key, valueStack, args);

        if (msg != null) {
            return msg;
        }

        if (indexedKey != null) {
            msg = getMessage(clazz.getName(), locale, indexedKey, valueStack, args);

            if (msg != null) {
                return msg;
            }
        }

        // look in properties of implemented interfaces
        Class[] interfaces = clazz.getInterfaces();

        for (Class anInterface : interfaces) {
            msg = getMessage(anInterface.getName(), locale, key, valueStack, args);

            if (msg != null) {
                return msg;
            }

            if (indexedKey != null) {
                msg = getMessage(anInterface.getName(), locale, indexedKey, valueStack, args);

                if (msg != null) {
                    return msg;
                }
            }
        }

        // traverse up hierarchy
        if (clazz.isInterface()) {
            interfaces = clazz.getInterfaces();

            for (Class anInterface : interfaces) {
                msg = findMessage(anInterface, key, indexedKey, locale, args, checked, valueStack);

                if (msg != null) {
                    return msg;
                }
            }
        } else {
            if (!clazz.equals(Object.class) && !clazz.isPrimitive()) {
                return findMessage(clazz.getSuperclass(), key, indexedKey, locale, args, checked, valueStack);
            }
        }

        return null;
    }

    private static void reloadBundles() {
        if (reloadBundles) {
            try {
                clearMap(ResourceBundle.class, null, "cacheList");

                // now, for the true and utter hack, if we're running in tomcat, clear
                // it's class loader resource cache as well.
                clearTomcatCache();
            }
            catch (Exception e) {
                LOG.error("Could not reload resource bundles", e);
            }
        }
    }


    private static void clearTomcatCache() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // no need for compilation here.
        Class cl = loader.getClass();

        try {
            if ("org.apache.catalina.loader.WebappClassLoader".equals(cl.getName())) {
                clearMap(cl, loader, "resourceEntries");
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("class loader " + cl.getName() + " is not tomcat loader.");
                }
            }
        }
        catch (Exception e) {
            LOG.warn("couldn't clear tomcat cache", e);
        }
    }


    private static void clearMap
            (Class
                    cl, Object
                    obj, String
                    name)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        Field field = cl.getDeclaredField(name);
        field.setAccessible(true);

        Object cache = field.get(obj);

        synchronized (cache) {
            Class ccl = cache.getClass();
            Method clearMethod = ccl.getMethod("clear");
            clearMethod.invoke(cache);
        }

    }

    /**
     * Clears all the internal lists.
     */
    public static void reset
            () {
        clearDefaultResourceBundles();

        bundlesMap.clear();

        synchronized (messageFormats) {
            messageFormats.clear();
        }
    }

    static class MessageFormatKey {
        String pattern;
        Locale locale;

        MessageFormatKey(String pattern, Locale locale) {
            this.pattern = pattern;
            this.locale = locale;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MessageFormatKey)) return false;

            final MessageFormatKey messageFormatKey = (MessageFormatKey) o;

            if (locale != null ? !locale.equals(messageFormatKey.locale) : messageFormatKey.locale != null)
                return false;
            if (pattern != null ? !pattern.equals(messageFormatKey.pattern) : messageFormatKey.pattern != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (pattern != null ? pattern.hashCode() : 0);
            result = 29 * result + (locale != null ? locale.hashCode() : 0);
            return result;
        }
    }

    static class GetDefaultMessageReturnArg {
        String message;
        boolean foundInBundle;

        public GetDefaultMessageReturnArg(String message, boolean foundInBundle) {
            this.message = message;
            this.foundInBundle = foundInBundle;
        }
    }

    private static class EmptyResourceBundle extends ResourceBundle {
        @Override
        public Enumeration<String> getKeys() {
            return null; // dummy
        }

        @Override
        protected Object handleGetObject(String key) {
            return null; // dummy
        }
    }

}
