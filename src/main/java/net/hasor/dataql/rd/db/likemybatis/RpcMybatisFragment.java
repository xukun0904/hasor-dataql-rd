package net.hasor.dataql.rd.db.likemybatis;

import net.hasor.core.Singleton;
import net.hasor.dataql.Hints;
import net.hasor.dataql.fx.db.likemybatis.*;
import net.hasor.dataql.rd.db.runsql.RpcSqlFragment;
import net.hasor.db.dal.fxquery.FxQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * 修改底层为远程调用数据源
 *
 * @author xukun
 * @version : 2021-08-20
 */
@Singleton
public class RpcMybatisFragment extends RpcSqlFragment {
    @Override
    public Object runFragment(Hints hint, Map<String, Object> paramMap, String fragmentString) throws Throwable {
        SqlNode sqlNode = parseSqlNode(fragmentString.trim());
        FxQuery fxSql = new RpcMybatisSqlQuery(sqlNode);
        if (usePage(hint)) {
            return this.usePageFragment(fxSql, hint, paramMap);
        } else {
            return this.noPageFragment(fxSql, hint, paramMap);
        }
    }

    /**
     * 枷锁防止多线程事件
     *
     * @param fragmentString
     * @throws Exception
     */
    private synchronized SqlNode parseSqlNode(String fragmentString) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(fragmentString.getBytes()));
        Element root = document.getDocumentElement();
        String tagName = root.getTagName();
        SqlNode sqlNode = new TextSqlNode("");
        if ("select".equalsIgnoreCase(tagName)) {
            sqlNode.setSqlNode(SqlMode.Query);
        } else {
            return sqlNode;
        }
        parseNodeList(sqlNode, root.getChildNodes());
        return sqlNode;
    }

    private void parseNodeList(SqlNode sqlNode, NodeList nodeList) {
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                sqlNode.addChildNode(new TextSqlNode(node.getNodeValue().trim()));
            } else if (node.getNodeType() != Node.COMMENT_NODE) {
                String nodeName = node.getNodeName();
                SqlNode childNode;
                if ("foreach".equalsIgnoreCase(nodeName)) {
                    childNode = parseForeachSqlNode(node);
                } else if ("if".equalsIgnoreCase(nodeName)) {
                    childNode = new IfSqlNode(getNodeAttributeValue(node, "test"));
                } else if ("trim".equalsIgnoreCase(nodeName)) {
                    childNode = parseTrimSqlNode(node);
                } else if ("set".equalsIgnoreCase(nodeName)) {
                    childNode = parseSetSqlNode();
                } else if ("where".equalsIgnoreCase(nodeName)) {
                    childNode = parseWhereSqlNode();
                } else {
                    throw new UnsupportedOperationException("Unsupported tags :" + nodeName);
                }
                sqlNode.addChildNode(childNode);
                if (node.hasChildNodes()) {
                    parseNodeList(childNode, node.getChildNodes());
                }
            }
        }
    }

    /**
     * 解析foreach节点
     */
    private ForeachSqlNode parseForeachSqlNode(Node node) {
        ForeachSqlNode foreachSqlNode = new ForeachSqlNode();
        foreachSqlNode.setCollection(getNodeAttributeValue(node, "collection"));
        foreachSqlNode.setSeparator(getNodeAttributeValue(node, "separator"));
        foreachSqlNode.setClose(getNodeAttributeValue(node, "close"));
        foreachSqlNode.setOpen(getNodeAttributeValue(node, "open"));
        foreachSqlNode.setItem(getNodeAttributeValue(node, "item"));
        return foreachSqlNode;
    }

    /**
     * 解析trim节点
     */
    private TrimSqlNode parseTrimSqlNode(Node node) {
        TrimSqlNode trimSqlNode = new TrimSqlNode();
        trimSqlNode.setPrefix(getNodeAttributeValue(node, "prefix"));
        trimSqlNode.setPrefixOverrides(getNodeAttributeValue(node, "prefixOverrides"));
        trimSqlNode.setSuffix(getNodeAttributeValue(node, "suffix"));
        trimSqlNode.setSuffixOverrides(getNodeAttributeValue(node, "suffixOverrides"));
        return trimSqlNode;
    }

    /**
     * 解析set节点
     */
    private SetSqlNode parseSetSqlNode() {
        SetSqlNode setSqlNode = new SetSqlNode();
        return setSqlNode;
    }

    /**
     * 解析where节点
     */
    private WhereSqlNode parseWhereSqlNode() {
        WhereSqlNode whereSqlNode = new WhereSqlNode();
        return whereSqlNode;
    }

    private String getNodeAttributeValue(Node node, String attributeKey) {
        Node item = node.getAttributes().getNamedItem(attributeKey);
        return item != null ? item.getNodeValue() : null;
    }
}
