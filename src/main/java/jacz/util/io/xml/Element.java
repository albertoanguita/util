package jacz.util.io.xml;

import java.util.*;

/**
 * An XML element of the XML-Dom API
 */
public class Element {

    private final String name;

    private Element parent;

    private Map<String, String> attributes;

    private List<Element> children;

    private String text;

    public Element(String name) {
        this(name, null);
    }

    public Element(String name, Element parent) {
        this.name = name;
        this.parent = parent;
        attributes = new HashMap<>();
        children = new ArrayList<>();
        text = null;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public String getName() {
        return name;
    }

    public Element getParent() {
        return parent;
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public String getAttributeValue(String name) {
        return attributes.get(name);
    }

    public void putAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean hasText() {
        return text != null;
    }

    public List<Element> getChildren() {
        return new ArrayList<>(children);
    }

    public List<Element> getChildren(String name) {
        List<Element> nameChildren = new ArrayList<>();
        for (Element child : children) {
            if (child.getName().equals(name)) {
                nameChildren.add(child);
            }
        }
        return nameChildren;
    }

    public List<Element> getChildren(String name, Map<String, String> attributes) {
        List<Element> nameChildren = new ArrayList<>();
        for (Element child : children) {
            if (child.getName().equals(name) && compareAttributes(child, attributes)) {
                nameChildren.add(child);
            }
        }
        return nameChildren;
    }

    private boolean compareAttributes(Element element, Map<String, String> attributes) {
        for (String attr : attributes.keySet()) {
            if (!element.getAttributeNames().contains(attr) || !element.getAttributeValue(attr).equals(attributes.get(attr))) {
                return false;
            }
        }
//        for (String attr : getAttributeNames()) {
//            if (!attributes.containsKey(attr) || !attributes.get(attr).equals(getAttributeValue(attr))) {
//                return false;
//            }
//        }
        return true;
    }

    public Element getChild(String name) {
        List<Element> children = getChildren(name);
        return (!children.isEmpty()) ? children.get(0) : null;
    }

    public Element getChild(String name, Map<String, String> attributes) {
        List<Element> children = getChildren(name, attributes);
        return (!children.isEmpty()) ? children.get(0) : null;
    }

    public void setChildren(List<Element> children) throws IllegalStateException {
        if (children != null && !children.isEmpty()) {
            checkCanAddChildren();
        }
        this.children = children;
        if (this.children == null) {
            this.children = new ArrayList<Element>();
        }
    }

    public void addChild(Element child) throws IllegalStateException {
        checkCanAddChildren();
        children.add(child);
        child.parent = this;
    }

    public void clearChildren() {
        children.clear();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) throws IllegalStateException {
        checkCanAddText();
        this.text = text;
    }

    public void clearText() {
        text = null;
    }

    private void checkCanAddChildren() throws IllegalStateException {
        if (hasText()) {
            throw new IllegalStateException("Cannot add children to element with text");
        }
    }

    private void checkCanAddText() throws IllegalStateException {
        if (hasChildren()) {
            throw new IllegalStateException("Cannot add text to element with children");
        }
    }
}
