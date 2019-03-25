package io.khasang.reflection.di;

import io.khasang.reflection.Gear;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Context {
    public static final String TAG_BEAN = "bean";
    public static final String TAG_PROPERTY = "property";
    private Map<String, Object> objectsById = new HashMap<>();
    private Set<Bean> beans = new HashSet<>();
    private Map<String, Object> objectsByClassName = new HashMap<>();
    public Context(String xmlPath) {
        // парсинг xml -- заполнение beans
        try {
            parseOurXml(xmlPath);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (SAXException e) {
            e.printStackTrace();
            return;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            return;
        }

        // когда прочитали xml и все о конфигурации теперь знаем
        // можно создать экземпляры на основе beans
        // beans -> objectsById
        try {
            instantiateBeans();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void parseOurXml(String xmlPath) throws ParserConfigurationException, IOException, SAXException, InvalidConfigurationException {
        // DOM Parser or SAX Parser
        Document document;
        // Document <- DocumentBuilder <- DocumentBuilderFactory (singleton)
        document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new File(xmlPath));
        Element root = document.getDocumentElement(); // получили корень <root>
        NodeList nodes = root.getChildNodes(); // получили список всех узлов <bean>
        for (int i = 0; i < nodes.getLength(); i++) {
            Node bean = nodes.item(i);
            if (TAG_BEAN.equals(bean.getNodeName())) {
                parseBean(bean);
            }
        }
    }

    private void parseBean(Node bean) throws InvalidConfigurationException {
        NamedNodeMap attributes = bean.getAttributes();
        Node id = attributes.getNamedItem("id");
        String idVal = id.getNodeValue();
        String classVal = attributes.getNamedItem("class").getNodeValue();
//        System.out.printf("id: %s, class: %s %n", idVal, classVal);

        Map<String, Property> properties = new HashMap<>();
        NodeList nodes = bean.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (TAG_PROPERTY.equals(node.getNodeName())) {
                Property property = parseProperty(node);
                properties.put(property.getName(), property);
//                System.out.printf("Propery: %s %n", property);
            }
        }

        beans.add(new Bean(idVal, classVal, properties));
    }

    private Property parseProperty(Node node) throws InvalidConfigurationException {
        NamedNodeMap attributes = node.getAttributes();
        String name = attributes.getNamedItem("name").getNodeValue();
        Node val = attributes.getNamedItem("val");
        if (val != null) {
            // значение примитивного типа val
            return new Property(name, val.getNodeValue(), ValueType.VALUE);
        } else {
            // иначе значение ссылочного типа ref
            Node ref = attributes.getNamedItem("ref");
            if (ref == null) {
                throw new InvalidConfigurationException("Failed to find attribute ref or val: " + name);
            } else {
                return new Property(name, ref.getNodeValue(), ValueType.REF);
            }
        }
    }

    private void instantiateBeans() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, InvalidConfigurationException, NoSuchMethodException, InvocationTargetException {
        for (Bean bean : beans) {
            Class<?> aClass = Class.forName(bean.getClassName());
            Object ob = aClass.getConstructor().newInstance();

            objectsById.put(bean.getId(), ob);
            objectsByClassName.put(bean.getClassName(), ob);
        }

        for (Bean bean : beans) {
            Object ob = objectsByClassName.get(bean.getClassName());
            Class<?> aClass = ob.getClass();

            processAnnotation(aClass, ob);

            // настройка
            for (String id : bean.getProperties().keySet()) {
                Field field = getField(aClass, id);
                if (field == null) {
                    throw new InvalidConfigurationException("Failded to set field " + id + " for class: " + aClass.getName());
                }
                field.setAccessible(true);

                Property property = bean.getProperties().get(id);

                switch (property.getType()) {
                    case VALUE:
                        field.set(ob, convert(field.getType().getSimpleName(), property.getValue()));
                        break;
                    case REF:
                        String refName = property.getValue();
                        if (objectsById.containsKey(refName)) {
                            field.set(ob, objectsById.get(refName));
                        } else {
                            throw new InvalidConfigurationException("Failed instantiate bean, ref: " + id);
                        }
                        break;
                    default:
                        throw new InvalidConfigurationException("Type error");
                }
            }
        }
    }

    private void processAnnotation(Class<?> clazz, Object instance) throws InvalidConfigurationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Auto.class)) {
                Class<?> bClass = Class.forName(field.getType().getName());
                boolean flag = false;
                for (Object object : objectsByClassName.keySet()) {
                    Class<?> aClass = Class.forName(object.toString());
                    if (bClass.isAssignableFrom(aClass)) {
                        flag = true;
                    }
                }

                Auto auto = field.getAnnotation(Auto.class);
                if (auto.isRequired() && !flag) {
                    throw new InvalidConfigurationException("Failed @Auto " + field.getName() + " " + field.getType());
                } else {
                    if (flag) {
                        for (Object object : objectsByClassName.keySet()) {
                            Class<?> aClass = Class.forName(object.toString());
                            if (bClass.isAssignableFrom(aClass)) {
                                field.setAccessible(true);
                                field.set(instance, objectsByClassName.get(object));
                            }
                        }
                    }
                }
            }
        }
    }

    private Object convert(String typeName, String value) throws InvalidConfigurationException {
        switch (typeName) {
            case "int":
            case "Integer":
                return Integer.valueOf(value);
            case "double":
            case "Double":
                return Double.valueOf(value);
            case "float":
            case "Float":
                return Float.valueOf(value);
            case "boolean":
            case "Boolean":
                return Boolean.valueOf(value);
            case "String":
                return String.valueOf(value);
            default:
                throw new InvalidConfigurationException(typeName);
        }
    }

    private Field getField(Class<?> aClass, String fieldName) throws NoSuchFieldException {
        try {
            return aClass.getDeclaredField(fieldName); // java.lang.NoSuchFieldException: count
        } catch (NoSuchFieldException e) {
            Class<?> superclass = aClass.getSuperclass();
            if (superclass == null) {
                throw e;
            } else {
                return getField(superclass, fieldName);
            }
        }
//        return aClass.getField(fieldName); // java.lang.NoSuchFieldException: power
    }

    public Object getBean(String beanId) {
        // возвращает уже созданный и настроенный экземпляр класса (бин)
        return objectsById.get(beanId);
    }
}
