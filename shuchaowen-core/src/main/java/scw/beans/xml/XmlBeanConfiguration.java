package scw.beans.xml;

import org.w3c.dom.NodeList;

import scw.beans.BeanConfiguration;

public abstract class XmlBeanConfiguration implements BeanConfiguration {
	private NodeList nodeList;

	public NodeList getNodeList() {
		return nodeList;
	}

	public void setNodeList(NodeList nodeList) {
		this.nodeList = nodeList;
	}
}
