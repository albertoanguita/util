package jacz.util.io.object_serialization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class with static methods for saving and restoring objects implementing the VersionedObject interface
 */
public class VersionedObjectSerializer {

    private static final byte[] STRING_TYPE = Serializer.serialize("String");
    private static final byte[] BOOLEAN_TYPE = Serializer.serialize("Boolean");
    private static final byte[] BYTE_TYPE = Serializer.serialize("Byte");
    private static final byte[] SHORT_TYPE = Serializer.serialize("Short");
    private static final byte[] INTEGER_TYPE = Serializer.serialize("Integer");
    private static final byte[] LONG_TYPE = Serializer.serialize("Long");
    private static final byte[] FLOAT_TYPE = Serializer.serialize("Float");
    private static final byte[] DOUBLE_TYPE = Serializer.serialize("Double");
    private static final byte[] ENUM_TYPE = Serializer.serialize("Enum");
    private static final byte[] SERIALIZABLE_TYPE = Serializer.serialize("Serializable");

    public static byte[] serializeVersionedObject(VersionedObject versionedObject) {
        byte[] data = Serializer.serialize(versionedObject.getCurrentVersion());
        Map<String, Object> attributes = versionedObject.serialize();
        data = Serializer.addArrays(data, Serializer.serialize(attributes.size()));
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            byte[] attributeName = Serializer.serialize(entry.getKey());
            // find the type of the attributes
            byte[] type;
            byte[] attributeArray;
            Object attribute = entry.getValue();
            if (attribute instanceof String) {
                type = STRING_TYPE;
                attributeArray = Serializer.serialize((String) attribute);
            } else if (attribute instanceof Boolean) {
                type = BOOLEAN_TYPE;
                attributeArray = Serializer.serialize((Boolean) attribute);
            } else if (attribute instanceof Byte) {
                type = BYTE_TYPE;
                attributeArray = Serializer.serialize((Byte) attribute);
            } else if (attribute instanceof Short) {
                type = SHORT_TYPE;
                attributeArray = Serializer.serialize((Short) attribute);
            } else if (attribute instanceof Integer) {
                type = INTEGER_TYPE;
                attributeArray = Serializer.serialize((Integer) attribute);
            } else if (attribute instanceof Long) {
                type = LONG_TYPE;
                attributeArray = Serializer.serialize((Long) attribute);
            } else if (attribute instanceof Float) {
                type = FLOAT_TYPE;
                attributeArray = Serializer.serialize((Float) attribute);
            } else if (attribute instanceof Double) {
                type = DOUBLE_TYPE;
                attributeArray = Serializer.serialize((Double) attribute);
            } else if (attribute instanceof Enum<?>) {
                type = ENUM_TYPE;
                attributeArray = Serializer.addArrays(Serializer.serialize(attribute.getClass().getName()), Serializer.serialize((Enum) attribute));
            } else if (attribute instanceof Serializable) {
                type = SERIALIZABLE_TYPE;
                attributeArray = Serializer.serializeObject((Serializable) attribute);
            } else {
                throw new RuntimeException("Illegal object class for attribute: " + entry.getKey() + ", " + attribute.getClass());
            }
            data = Serializer.addArrays(data, attributeName, type, attributeArray);
        }
        return data;
    }

    public static void deserializeVersionedObject(VersionedObject versionedObject, byte[] data) {
        deserializeVersionedObject(versionedObject, data, new MutableOffset());
    }

    public static void deserializeVersionedObject(VersionedObject versionedObject, byte[] data, MutableOffset offset) {
        String version = Serializer.deserializeString(data, offset);
        int attributeCount = Serializer.deserializeInt(data, offset);
        Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = Serializer.deserializeString(data, offset);
            String type = Serializer.deserializeString(data, offset);
            try {
                switch (type) {
                    case "String":
                        attributes.put(attributeName, Serializer.deserializeString(data, offset));
                        break;

                    case "Boolean":
                        attributes.put(attributeName, Serializer.deserializeBoolean(data, offset));
                        break;

                    case "Byte":
                        attributes.put(attributeName, Serializer.deserializeByte(data, offset));
                        break;

                    case "Short":
                        attributes.put(attributeName, Serializer.deserializeShort(data, offset));
                        break;

                    case "Integer":
                        attributes.put(attributeName, Serializer.deserializeInt(data, offset));
                        break;

                    case "Long":
                        attributes.put(attributeName, Serializer.deserializeLong(data, offset));
                        break;

                    case "Float":
                        attributes.put(attributeName, Serializer.deserializeFloat(data, offset));
                        break;

                    case "Double":
                        attributes.put(attributeName, Serializer.deserializeDouble(data, offset));
                        break;

                    case "Enum":
                        String enumType = Serializer.deserializeString(data, offset);
                        Class enumClass = Class.forName(enumType);
                        attributes.put(attributeName, Serializer.deserializeEnum(enumClass, data, offset));
                        break;

                    case "Serializable":
                        attributes.put(attributeName, Serializer.deserializeObject(data, offset));
                        break;

                    default:
                        // the VersionedObjectSerializer failed when deserializing the byte array
                        throw new RuntimeException("Unexpected type: " + type);
                }
            } catch (RuntimeException | ClassNotFoundException e) {
                versionedObject.errorDeserializing(version, attributes);
                break;
            }
        }
        versionedObject.deserialize(version, attributes);
    }


}
