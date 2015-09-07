package jacz.util.io.object_serialization.test;

import jacz.util.io.object_serialization.VersionedObject;
import jacz.util.io.object_serialization.VersionedObjectSerializer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestVersionedObject implements VersionedObject {

    private static class SerClass implements Serializable {

        public int i;

        public String s;

        private SerClass(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }

    public static void main(String[] args) {
        TestVersionedObject testVersionedObject = new TestVersionedObject();

        byte[] data = VersionedObjectSerializer.serializeVersionedObject(testVersionedObject);

        TestVersionedObject testVersionedObject1 = new TestVersionedObject(false);
        VersionedObjectSerializer.deserializeVersionedObject(testVersionedObject1, data);

        System.out.println("END");
    }


    private int i;

    private String s;

    private Test.TestEnum t;

    private boolean b;

    private long l;

    private float f;

    private SerClass serClass;

    public TestVersionedObject() {
        i = 5;
        s = "fuck";
        t = Test.TestEnum.A;
        b = true;
        l = 27;
        f = 0.5f;
        serClass = new SerClass(3, "hello");
    }

    public TestVersionedObject(boolean b) {
        i = 0;
        s = null;
        t = null;
        b = false;
        l = 0;
        f = 0;
        serClass = null;
    }

    @Override
    public String getCurrentVersion() {
        return "1.0";
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("i", i);
        map.put("s", s);
        map.put("t", t);
        map.put("b", b);
        map.put("l", l);
        map.put("f", f);
        map.put("serClass", serClass);
        return map;
    }

    @Override
    public void deserialize(String version, Map<String, Object> attributes) throws RuntimeException {
        if (version.equals(getCurrentVersion())) {
            i = (int) attributes.get("i");
            s = (String) attributes.get("s");
            t = (Test.TestEnum) attributes.get("t");
            b = (boolean) attributes.get("b");
            l = (long) attributes.get("l");
            f = (float) attributes.get("f");
            serClass = (SerClass) attributes.get("serClass");
        }
    }

    @Override
    public void errorDeserializing(String version, Map<String, Object> attributes) {
        System.out.println("error");
    }
}
