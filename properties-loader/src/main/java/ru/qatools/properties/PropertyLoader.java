package ru.qatools.properties;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import ru.qatools.properties.converters.CharsetConverter;
import ru.qatools.properties.converters.URIConverter;
import ru.qatools.properties.decorators.DefaultFieldDecorator;
import ru.qatools.properties.decorators.FieldDecorator;
import ru.qatools.properties.exeptions.PropertyLoaderException;
import ru.qatools.properties.providers.DefaultPropertyProvider;
import ru.qatools.properties.providers.PropertyProvider;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Eroshenko Artem eroshenkoam@yandex-team.ru
 *         Date: 09.04.15
 */
public class PropertyLoader {

    protected ClassLoader classLoader = getClass().getClassLoader();

    protected Properties defaults = new Properties();

    protected Properties compiled = new Properties();

    protected FieldDecorator fieldDecorator = new DefaultFieldDecorator();

    protected PropertyProvider propertyProvider = new DefaultPropertyProvider();

    protected Map<String, String> cache = new HashMap<>();

    protected final ConvertUtilsBean converters = new ExtendedConvertUtilsBean();

    /**
     * Do not instance this class by yourself. Use {@link #newInstance()} instead.
     */
    PropertyLoader() {
    }

    /**
     * Populate given bean using properties from {@link #defaults} and
     * {@link PropertyProvider#provide(ClassLoader, Object)}
     *
     * @see #initFields(Object)
     * @see PropertyProvider
     */
    public void populate(Object bean) {
        compiled.putAll(defaults);
        compiled.putAll(propertyProvider.provide(classLoader, bean));

        initFields(bean);
    }

    /**
     * Init all fields declared in class (and all superclasses) of given bean.
     *
     * @see #initFields(Object, Field[])
     */
    protected void initFields(Object bean) {
        Class<?> clazz = bean.getClass();
        while (clazz != Object.class) {
            initFields(bean, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Init specified fields for given bean. For each field will the key
     * {@link #getFieldKey(Field)}. This key will be used to find string representation
     * of value {@link #getFieldValue(String)} and then converted to field type
     * {@link #convertValueForField(Field, String)} and finally set to field
     * {@link #setValueToField(Field, Object, Object)}. If can't find value for
     * field key ({@link #getFieldValue(String)} returns <code>null</code>) then
     * this field was skipped.
     *
     * @see #getFieldKey(Field)
     * @see #getFieldValue(String)
     * @see #convertValueForField(Field, String)
     * @see #setValueToField(Field, Object, Object)
     */
    protected void initFields(Object bean, Field[] fields) {
        for (Field field : fields) {
            if (fieldDecorator.shouldDecorate(field)) {
                String key = getFieldKey(field);
                String stringValue = getFieldValue(key);
                if (stringValue == null) {
                    continue;
                }
                Object value = convertValueForField(field, stringValue);
                setValueToField(field, bean, value);
            }
        }
    }

    /**
     * Get key for given field using {@link #fieldDecorator}.
     */
    protected String getFieldKey(Field field) {
        String key = fieldDecorator.getFieldKey(field);

        if (key == null) {
            throw new PropertyLoaderException(String.format(
                    "Field decorator <%s> returned null key for field <%s> ", fieldDecorator, field.getName()
            ));
        }
        return key;
    }

    /**
     * Get string representation of field value from cache. If no value for given key
     * then find it in {@link #compiled} properties.
     *
     * @see #getFieldKey(Field)
     */
    protected String getFieldValue(String key) {
        if (!cache.containsKey(key)) {
            String value = compiled.getProperty(key);
            cache.put(key, value);
        }

        return cache.get(key);
    }

    /**
     * Convert given value to specified field type.
     *
     * @throws PropertyLoaderException if any problems occurs during type conversion
     */
    protected Object convertValueForField(Field field, String stringValue) {
        Class<?> type = field.getType();
        try {
            return converters.convert(stringValue, type);
        } catch (Exception e) {
            throw new PropertyLoaderException(String.format(
                    "Can't convert value <%s> to type <%s> for field <%s>", stringValue, type, field.getName()
            ), e);
        }
    }

    /**
     * Set given value to specified field of given object.
     *
     * @throws PropertyLoaderException if some exceptions occurs during reflection calls.
     * @see Field#setAccessible(boolean)
     * @see Field#set(Object, Object)
     */
    protected void setValueToField(Field field, Object bean, Object value) {
        try {
            field.setAccessible(true);
            field.set(bean, value);
        } catch (Exception e) {
            throw new PropertyLoaderException(
                    String.format("Can not set bean <%s> field <%s> value", bean, field), e
            );
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @see #classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Fluent-api builder.
     *
     * @see #setClassLoader(ClassLoader)
     */
    public PropertyLoader withClassLoader(ClassLoader classLoader) {
        setClassLoader(classLoader);
        return this;
    }

    public Properties getDefaults() {
        return defaults;
    }

    /**
     * @see #defaults
     */
    public void setDefaults(Properties defaults) {
        this.defaults = defaults;
    }

    /**
     * Fluent-api builder.
     *
     * @see #setDefaults(Properties)
     */
    public PropertyLoader withDefaults(Properties defaults) {
        setDefaults(defaults);
        return this;
    }

    public FieldDecorator getFieldDecorator() {
        return fieldDecorator;
    }

    /**
     * @see #fieldDecorator
     */
    public void setFieldDecorator(FieldDecorator fieldDecorator) {
        this.fieldDecorator = fieldDecorator;
    }

    /**
     * Fluent-api builder.
     *
     * @see #setFieldDecorator(FieldDecorator)
     */
    public PropertyLoader withFieldDecorator(FieldDecorator fieldDecorator) {
        setFieldDecorator(fieldDecorator);
        return this;
    }

    public PropertyProvider getPropertyProvider() {
        return propertyProvider;
    }

    /**
     * @see #propertyProvider
     */
    public void setPropertyProvider(PropertyProvider propertyProvider) {
        this.propertyProvider = propertyProvider;
    }

    /**
     * Fluent-api builder.
     *
     * @see #setPropertyProvider(PropertyProvider)
     */
    public PropertyLoader withPropertyProvider(PropertyProvider propertyProvider) {
        setPropertyProvider(propertyProvider);
        return this;
    }

    /**
     * Register custom converter for given type.
     */
    public PropertyLoader register(Converter converter, Class<?> type) {
        converters.register(converter, type);
        return this;
    }

    /**
     * Do not instance class by yourself. Use this builder
     */
    public static PropertyLoader newInstance() {
        return new PropertyLoader();
    }

    /**
     * Using this class you can convert {@link Enum} (enums doen't supported for
     * {@link ConvertUtilsBean} by default, you should register enum converter for
     * each concrete class), {@link Charset} and {@link URI}
     */
    protected static class ExtendedConvertUtilsBean extends ConvertUtilsBean {

        /**
         * Register all standard converters and few more. Also configure
         * exception policy (will throw a convert exception if some convert
         * exception occurs)
         */
        public ExtendedConvertUtilsBean() {
            super();
            register(true, true, 0);
            register(new CharsetConverter(), Charset.class);
            register(new URIConverter(), URI.class);
        }

        /**
         * Hack for enums
         */
        @Override
        public Object convert(String value, Class clazz) {
            if (clazz.isEnum()) {
                return Enum.valueOf(clazz, value.toUpperCase().trim());
            } else {
                return super.convert(value, clazz);
            }
        }
    }
}
