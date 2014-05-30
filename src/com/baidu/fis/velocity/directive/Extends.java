package com.baidu.fis.velocity.directive;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.*;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.ASTprocess;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by 2betop on 5/7/14.
 */
public class Extends extends AbstractInclude {

    protected Map<String, Node> map;

    @Override
    public String getName() {
        return "extends";
    }

    @Override
    public int getType() {
        return BLOCK;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        Boolean isTopNode = false;
        Writer buffer = writer;

        Node parent = node.jjtGetParent();
        if ((parent == null || parent instanceof ASTprocess) &&
                context.getTemplateNameStack().length < 2) {
            isTopNode = true;
        }

        if (isTopNode) {
            buffer = new StringWriter();
        }

        this.doRender(context, buffer, node);

        // 只有当它为顶级 node 的时候才这么做，当然不能是被extends时。
        if (isTopNode) {
            writer.write(fisResource.filterContent(buffer.toString()));
            fisResource.reset();
        }

        return true;
    }

    @Override
    protected void preRender(InternalContextAdapter context) {

        if (this.map != null && !this.map.isEmpty()) {
            List macroLibraries = context.getMacroLibraries();
            String templateName = macroLibraries.get(macroLibraries.size() - 1).toString();
            Block.pushTemplate(templateName);
            Block.registerBlocks(templateName, this.map);
        }

        super.preRender(context);
    }

    @Override
    protected void postRender(InternalContextAdapter context) {
        String templateName = Block.popTemplate();
        Block.unRegisterBlocks(templateName);
        super.postRender(context);
    }

    protected void doRender(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        /*
         *  did we get an argument?
         */
        if ( node.jjtGetNumChildren() == 1 )
        {
            throw new VelocityException("#extends(): argument missing at " +
                    Log.formatFileString(this));
        }

        String target = node.jjtGetChild(0).value(context).toString();

        if (target.isEmpty()) {
            throw  new VelocityException("#extends(): the first argument is empty.");
        }


        Node content = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        Map<String, Node> map = new HashMap<String, Node>();
        ArrayList<Node> children = new ArrayList<Node>();

        for (int i = 0, len = content.jjtGetNumChildren(); i < len; i++) {
            Node child = content.jjtGetChild(i);
            String blockId;

            // 找出 content 节点
            if (child instanceof ASTDirective &&
                    ((ASTDirective)child).getDirectiveName().equals("block")) {
                blockId = child.jjtGetChild(0).value(context).toString();

                map.put(blockId, child);
            }

            children.add(child);
        }

        this.map = map;
        Collection<Node> blocks = new ArrayList<Node>(map.values());

        super.render(context, writer, node);

        // 把覆盖过的删除了。
        for(Node block:blocks) {
            if (!map.containsValue(block)) {
                children.remove(block);
            }
        }

        blocks.clear();
        map.clear();
        this.map = null;

        // 把 rest 的 Node 渲染了
        for (Node child:children) {
            child.render(context, writer);
        }
    }
}
