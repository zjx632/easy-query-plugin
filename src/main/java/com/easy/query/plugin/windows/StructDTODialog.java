package com.easy.query.plugin.windows;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.easy.query.plugin.core.RenderEasyQueryTemplate;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.*;
import com.easy.query.plugin.core.entity.struct.RenderStructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
import com.easy.query.plugin.windows.ui.dto2ui.JCheckBoxTree;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import lombok.Lombok;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StructDTODialog extends JDialog {
    private final StructDTOContext structDTOContext;
    private final List<ClassNode> classNodes;
    private TreeModel treeModel;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBoxTree entityProps;
    private JCheckBox combineCk;
    private JCheckBox dataCheck;
    private JPanel dynamicBtnPanel;
    private Map<String, String> buttonMaps;

    private TreeModel initTree(List<ClassNode> classNodes) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Entities");
        for (ClassNode classNode : classNodes) {
            DefaultMutableTreeNode parent = new DefaultMutableTreeNode(classNode);
            root.add(parent);
            initProps(parent, classNode);
        }
        return new DefaultTreeModel(root);
    }

    private void initProps(DefaultMutableTreeNode parent, ClassNode classNode) {
        if (CollUtil.isEmpty(classNode.getChildren())) {
            return;
        }
        for (ClassNode child : classNode.getChildren()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            parent.add(childNode);
            initProps(childNode, child);
        }
    }

    public StructDTODialog(StructDTOContext structDTOContext, List<ClassNode> classNodes) {
        this.structDTOContext = structDTOContext;
        this.classNodes = classNodes;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(800, 700);
        setTitle("Struct DTO");
        DialogUtil.centerShow(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        dataCheck.setSelected(true);
        BoxLayout boxLayout = new BoxLayout(dynamicBtnPanel, BoxLayout.LINE_AXIS);
        dynamicBtnPanel.setLayout(boxLayout);
        dynamicIgnoreButtons(structDTOContext.getProject());
    }

    private void dynamicIgnoreButtons(Project project) {
        this.buttonMaps = new LinkedHashMap<>();
        EasyQueryConfig config = EasyQueryQueryPluginConfigData.getAllEnvStructDTOIgnore(new EasyQueryConfig());
        if (config.getConfig() == null) {
            config.setConfig(new HashMap<>());
        }
        String projectName = project.getName();
        String setting = config.getConfig().get(projectName);
        initIgnoreButtons(setting);
        for (Map.Entry<String, String> kv : buttonMaps.entrySet()) {
            JButton jButton = new JButton(kv.getKey());
            jButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onIgnoreCancel(kv.getKey());
                }
            });
            dynamicBtnPanel.add(jButton);
        }

    }

    private void onIgnoreCancel(String key) {
        String s = this.buttonMaps.get(key);
        if (s != null) {
            List<String> ignoreProperties = Arrays.asList(s.split(","));

            TreePath[] checkedPaths = entityProps.getCheckedPaths();
            if (checkedPaths == null || checkedPaths.length == 0) {
                return;
            }
            long count = Arrays.stream(checkedPaths).filter(o -> o.getPathCount() == 2).count();
            if (count != 1) {
                return;
            }
            for (String ignoreProperty : ignoreProperties) {
                this.entityProps.removeCheckedPathsByName(ignoreProperty);
            }
        }
    }

    private void initIgnoreButtons(String setting) {
        try {

            LinkedHashMap<String, String> configMap = JSONObject.parseObject(setting,
                new TypeReference<LinkedHashMap<String, String>>() {
                });
            buttonMaps.putAll(configMap);
        } catch (Exception ignored) {

        }
    }

    private void onOK() {
        // add your code here

        Object root = this.treeModel.getRoot();
        if (root instanceof TreeModel) {
            TreeModel treeModelRoot = (TreeModel) root;
            System.out.println(treeModelRoot);
        }
        TreePath[] checkedPaths = entityProps.getCheckedPaths();
        if (checkedPaths == null || checkedPaths.length == 0) {
            NotificationUtils.notifySuccess("请选择节点", structDTOContext.getProject());
            return;
        }
        long count = Arrays.stream(checkedPaths).filter(o -> o.getPathCount() == 2).count();
        if (count != 1) {
            NotificationUtils.notifySuccess("请选择一个对象节点", structDTOContext.getProject());
            return;
        }

        List<TreeClassNode> nodeList = Arrays.stream(checkedPaths)
            .filter(o -> o.getPathCount() > 1)
            .map(o -> {
                int pathCount = o.getPathCount();
                ClassNode classNode = (ClassNode) ((DefaultMutableTreeNode) o.getLastPathComponent())
                    .getUserObject();
                return new TreeClassNode(pathCount, classNode);
            }).sorted((a, b) -> {
                if (a.getPathCount() != b.getPathCount()) {
                    return a.getPathCount() - b.getPathCount();
                } else {
                    return a.getClassNode().getSort() - b.getClassNode().getSort();
                }
            }).collect(Collectors.toList());

        Iterator<TreeClassNode> iterator = nodeList.iterator();
        TreeClassNode appNode = iterator.next();
        ClassNode app = appNode.getClassNode();
        StructDTOApp structDTOApp = new StructDTOApp(app.getName(), app.getOwner(), structDTOContext.getPackageName(),
            app.getSort());

        String entityDTOName = "InitDTOName";
        String dtoClassName;
        if (StrUtil.isNotBlank(structDTOContext.getDtoClassName())) {
            // 如果从上下文传递了DTO
            dtoClassName = structDTOContext.getDtoClassName();
        } else {
            dtoClassName = StrUtil.subAfter(structDTOApp.getEntityName(), ".", true) + "DTO";
        }

        Messages.InputDialog dialog = new Messages.InputDialog("请输入DTO名称", "提示名称", Messages.getQuestionIcon(),
            dtoClassName, new InputAnyValidatorImpl());
        dialog.show();
        if (dialog.isOK()) {
            String dtoName = dialog.getInputString();
            if (StrUtil.isBlank(dtoName)) {
                Messages.showErrorDialog(structDTOContext.getProject(), "输入的dto名称为空", "错误提示");
                return;
            }
            entityDTOName = dtoName;
        } else {
            return;
        }

        RenderStructDTOContext renderContext = new RenderStructDTOContext(structDTOContext.getProject(),
            structDTOContext.getPath(), structDTOContext.getPackageName(), entityDTOName, structDTOApp,
            structDTOContext.getModule());
        // 设置一下rootPsiClass
        renderContext.setRootPsiClass(appNode.getClassNode().getPsiClass());

        renderContext.setData(dataCheck.isSelected());
        // 传递import
        renderContext.getImports().addAll(structDTOContext.getImports());
        // 获取 app 里面的 import , 那里面的 Imports 也要传递进来
        String selfFullEntityType = app.getSelfFullEntityType();
        // 根据 selfFullEntityType 获取 psiClass
        PsiClass psiClass = PsiJavaFileUtil.getPsiClass(structDTOContext.getProject(), selfFullEntityType);
        // 从psiClass 中获取引入的包
        renderContext.getImports().addAll(PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) psiClass.getContainingFile()));

        // 如果父类中有字段, 则需要把父类的 import 也加入进来
        if (Objects.nonNull(psiClass.getSuperClass()) && ArrayUtil.isNotEmpty(psiClass.getSuperClass().getFields())) {
            renderContext.getImports().addAll(PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) psiClass.getSuperClass().getContainingFile()));
        }

        // 如果传入了DTO ClassName 说明是来自修改, 此时需要删除源文件
        renderContext.setDeleteExistsFile(StrUtil.isNotBlank(structDTOContext.getDtoClassName()));

        PropAppendable base = structDTOApp;
        int i = 0;

        //region 处理选中的路径，组成渲染代码的上下文
        HashSet<String> dtoNames = new HashSet<>();
        dtoNames.add(entityDTOName);
        while (iterator.hasNext()) {
            TreeClassNode treeClassNode = iterator.next();
            ClassNode classNode = treeClassNode.getClassNode();
            PsiField psiField = classNode.getPsiField();
            PsiAnnotation psiAnnoColumn = classNode.getPsiAnnoColumn();
            PsiAnnotation psiAnnoNavigate = classNode.getPsiAnnoNavigate();
            PsiAnnotation psiAnnoNavigateFlat = classNode.getPsiAnnoNavigateFlat();

            if (StrUtil.isNotBlank(classNode.getSelfFullEntityType())) {
                // 引入了类, 去把对应类的 import 全都提取出来放到里面
                // 根据 selfFullEntityType 获取 psiClass
                PsiClass nodePsiClass = PsiJavaFileUtil.getPsiClass(structDTOContext.getProject(), classNode.getSelfFullEntityType());
                // 从psiClass 中获取引入的包
                renderContext.getImports().addAll(PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) nodePsiClass.getContainingFile()));

            }

            if (treeClassNode.getPathCount() > 3) {
                PropAppendable propAppendable = renderContext.getEntities().stream()
                    .filter(o -> {
                        boolean allow = (o.getPathCount() + 1) == treeClassNode.getPathCount()
                            && Objects.equals(o.getSelfEntityType(), classNode.getOwner())
                            && Objects.equals(o.getPropName(), classNode.getOwnerPropertyName());
                        return allow;
                    })
                    .findFirst().orElse(null);
                if (propAppendable == null) {
                    break;
                }
                base = propAppendable;
            }
            StructDTOProp structDTOProp = new StructDTOProp(classNode.getName(), classNode.getPropText(),
                classNode.getOwner(), classNode.isEntity(), classNode.getSelfEntityType(), classNode.getSort(),
                treeClassNode.getPathCount(), classNode.getOwnerFullName(), classNode.getSelfFullEntityType());
            structDTOProp.setClassNode(classNode);


            if (structDTOProp.isEntity()) {
                // 当前是实体
                String dotName = "Internal" + StrUtil.upperFirst(classNode.getName());
                if (!dtoNames.contains(dotName)) {
                    dtoNames.add(dotName);
                    structDTOProp.setDtoName(dotName);
                } else {
                    structDTOProp.setDtoName(dotName + (i++));
                }
                if (StringUtils.isNotBlank(structDTOProp.getPropText())) {
                    if (structDTOProp.getPropText().contains("<") && structDTOProp.getPropText().contains(">")) {
                        // 是集合类型 如 List<T> 替换 <> 里面的原始类型为 innerDTO 类型
                        String regex = "<\\s*" + structDTOProp.getSelfEntityType() + "\\s*>";
                        String newPropText = structDTOProp.getPropText().replaceAll(regex,
                            "<" + structDTOProp.getDtoName() + ">");
                        structDTOProp.setPropText(newPropText);
                    } else {
                        // 原始类型
                        String regex = "private\\s+" + structDTOProp.getSelfEntityType();
                        String newPropText = structDTOProp.getPropText().replaceAll(regex,
                            "private " + structDTOProp.getDtoName());
                        structDTOProp.setPropText(newPropText);
                    }
                    if (Objects.nonNull(psiAnnoNavigate)) {
                        // 字段上有 @Navigate 注解, 精简下注解属性, 只保留 value
                        List<JvmAnnotationAttribute> attrList = psiAnnoNavigate.getAttributes().stream()
                            .filter(attr -> StrUtil.equalsAny(attr.getAttributeName(), "value"))
                            .collect(Collectors.toList());
                        // 过滤后的属性值拼接起来
                        String attrText = attrList.stream().map(attr -> ((PsiNameValuePairImpl) attr).getText()).collect(Collectors.joining(", "));
                        // 再拼成 @Navigate 注解文本
                        String replacement = "@Navigate(" + attrText + ")";
                        // 将原本的注解文本中的 @Navigate 替换为新的
                        String newPropText = structDTOProp.getPropText().replace(psiAnnoNavigate.getText(), replacement);


                        structDTOProp.setPropText(newPropText);
                    }
                }
                renderContext.getEntities().add(structDTOProp);
            }
            if (psiAnnoColumn != null) {

                // 从注解上获取 Column 的 value , conversion,complexPropType
                List<JvmAnnotationAttribute> columnAttrList = psiAnnoColumn.getAttributes().stream()
                    .filter(attr -> StrUtil.equalsAny(attr.getAttributeName(), "value", "conversion", "complexPropType"))
                    // 过滤下值为空的
                    .filter(attr -> Objects.nonNull(attr.getAttributeValue()))
                    .collect(Collectors.toList());
                // 过滤后的属性值拼接起来
                String attrText = columnAttrList.stream().map(attr -> ((PsiNameValuePairImpl) attr).getText()).collect(Collectors.joining(", "));
                // 再拼成 @Column 注解文本
                String replacement = StrUtil.isBlank(attrText) ? "" : "@Column(" + attrText + ")";

                if (!StrUtil.equalsAny(psiAnnoColumn.getText(), replacement)) {
                    // 将原本的注解文本中的 @Navigate 替换为新的
                    String newPropText = structDTOProp.getPropText().replace(psiAnnoColumn.getText(), replacement);
                    structDTOProp.setPropText(newPropText);
                }
            }

            // 获取字段上的注解, 一些其他ORM框架的注解可以直接移除, 方便混用的时候生成
            if (psiField != null) {
                PsiAnnotation[] annotationArr = psiField.getAnnotations();
                for (PsiAnnotation annotation : annotationArr) {
                    String qualifiedName = annotation.getQualifiedName();
                    if (StrUtil.startWithAny(qualifiedName,
                        "com.baomidou.mybatisplus.annotation" // 移除 MyBatisPlus 的注解
                        //
                    )) {
                        structDTOProp.setPropText(structDTOProp.getPropText().replace(annotation.getText(), ""));
                    }
                }
                // 处理完了之后可能会出现多个换行符连一起, 需要替换成一个 \n \n -> \n
                structDTOProp.setPropText(structDTOProp.getPropText().replaceAll("\n\\s+\n", "\n"));
            }

            base.addProp(structDTOProp);
        }
        //endregion


        //region 修改DTO的时候, 保留DTO上面的自定义方法
        PsiClass dtoPsiClass = structDTOContext.getDtoPsiClass();
        if (Objects.nonNull(dtoPsiClass)) {
            // 传递了 DTO PsiClass 说明是修改, 开始进行检测

            // 把DTO上面的包也重新导入到 新的DTO里面
            renderContext.getImports().addAll(PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) dtoPsiClass.getContainingFile()));

            int methodIdx = 1;

            //region 先处理DTO自身方法的保存
            // 先处理DTO自身的方法
            PsiMethod[] dtoMethods = dtoPsiClass.getMethods();
            for (PsiMethod dtoMethod : dtoMethods) {
                // 如果是 LightMethodBuilder , 说明应该是注解生成的 如 lombok getter setter
                if (dtoMethod instanceof LightMethodBuilder) {
                    continue;
                }
                // 如果不是的话, 则是自定义的方法, 需要保留
                String methodContent = dtoMethod.getText();
                // 保留方法, 添加当 root 实体上
                renderContext.getDtoApp().addProp(new StructDTOProp(dtoMethod.getName(),methodContent,"",false,"",10_000+methodIdx,0,"",""));
                methodIdx++;
            }

            //endregion


            //region DTO 内部类的方法保留
            // 再处理DTO 内部类的方法保留

            PsiClass[] innerClasses = dtoPsiClass.getInnerClasses();
            for (PsiClass innerClass : innerClasses) {

                // 从新增实体上找到对应的 entity
                PropAppendable propAppendable = renderContext.getEntities().stream().filter(entity -> {
                    return StrUtil.equals(((StructDTOProp) entity).getDtoName(), innerClass.getName());
                }).findFirst().orElse(null);

                // 如果没有找到对应的 entity, 说明当前实体被删除了, 则不需要保留
                if (propAppendable == null) {
                    continue;
                }

                PsiMethod[] innerMethods = innerClass.getMethods();



                for (PsiMethod innerMethod : innerMethods) {
                    // 如果是 LightMethodBuilder , 说明应该是注解生成的 如 lombok getter setter
                    if (innerMethod instanceof LightMethodBuilder) {
                        continue;
                    }
                    // 如果不是的话, 则是自定义的方法, 需要保留
                    String methodContent = innerMethod.getText();
                    // 保留方法, 添加当当前对应实体上
                    propAppendable.addProp(new StructDTOProp(innerMethod.getName(),methodContent,"",false,"",10_000+methodIdx,0,"",""));
                    methodIdx++;
                }
            }

            //endregion

        }
        //endregion




        //region 最终清理下 import, 有些 import 没有必要
        renderContext.getImports().removeIf(imp -> {
            // 自动生成的 Proxy类 在 dto中无用, 所以需要移除
            if (StrUtil.contains(imp, ".proxy.") && StrUtil.endWith(imp, "Proxy")) {
                return true;
            }
            // eq 一些在实体上的注解, 在 DTO上也没有用
            if (StrUtil.equalsAny(imp,
                "com.easy.query.core.annotation.EntityProxy",
                "com.easy.query.core.annotation.Table",
                "com.easy.query.core.annotation.EasyAlias",
                "lombok.experimental.FieldNameConstants",
                "com.baomidou.mybatisplus.annotation.Version",
                "com.easy.query.core.proxy.ProxyEntityAvailable"
            )) {
                return true;
            }
            // 以某些开头的也移除掉
            if (StrUtil.startWithAny(imp,
                "com.baomidou.mybatisplus.annotation"
            )) {
                return true;
            }
            // keep
            return false;
        });
        //endregion

        boolean b = RenderEasyQueryTemplate.renderStructDTOType(renderContext);
        if (!b) {
            return;
        }
        NotificationUtils.notifySuccess("生成成功", structDTOContext.getProject());
        structDTOContext.setSuccess(true);
        dispose();
    }


    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    // public static void main(String[] args) {
    // StructDTODialog dialog = new StructDTODialog();
    // dialog.pack();
    // dialog.setVisible(true);
    // System.exit(0);
    // }
    private void createUIComponents() {
        this.treeModel = initTree(classNodes);
        entityProps = new JCheckBoxTree(treeModel);
        // UI创建完成后, 默认选中已经存在的路径
        selectDtoPropsPathAfterUiCreate();
    }

    /**
     * UI 创建完成后，选择已经存在的 DTO 路径
     */
    private void selectDtoPropsPathAfterUiCreate() {

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

        if (structDTOContext == null || !StringUtils.isNotBlank(structDTOContext.getDtoClassName())) {
            // 如果不存在 dtoClass，不进行选择
            return;
        }
        // 如果存在 dtoClass，从中获取路径进行选择
        Set<String> selectedPaths = extractCurrentDTOSelectPath();

        Enumeration<TreeNode> enumeration = root.preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            Object[] userObjectPaths = node.getUserObjectPath();
            // 尝试进行全匹配
            // 如果 数组长度小于3, 则直接勾选上
            if (userObjectPaths.length < 3) {
                TreePath treePath = new TreePath(node.getPath());
                entityProps.checkTreeItem(treePath, true);
                continue;
            }
            // 取 index >1 的元素
            String nodePath = Arrays.stream(userObjectPaths).skip(2)
                .map((o) -> {
                    if (o instanceof ClassNode) {
                        return ((ClassNode) o).getName();
                    }
                    return "";
                })
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("."));


            if (selectedPaths.contains(nodePath)) {
                TreePath treePath = new TreePath(node.getPath());
                entityProps.checkTreeItem(treePath, true);
            }
        }

    }

    /**
     * 从 dtoClass 解析需要选择的路径
     */
    private Set<String> extractCurrentDTOSelectPath() {
        Set<String> paths = new HashSet<>();

        try {
            PsiClass dtoPsiClass = PsiJavaFileUtil.getPsiClass(structDTOContext.getProject(), structDTOContext.getPackageName() + "." + structDTOContext.getDtoClassName());
            PsiClass[] innerClasses = dtoPsiClass.getInnerClasses();
            PsiField[] fields = dtoPsiClass.getFields();


            // fields 直接添加
            for (PsiField field : fields) {
                extractInnerClassFieldPath(paths, "", field, innerClasses);
            }

        } catch (Exception e) {
            NotificationUtils.notifyError("错误", "解析 DTO 类型失败", structDTOContext.getProject());
        }

        return paths;
    }

    /**
     * 提取内部类的字段路径
     */
    private void extractInnerClassFieldPath(Set<String> paths, String contextPath, PsiField field, PsiClass[] innerClasses) {
        // 先把当前路径加进去
        String currentFieldPath = Stream.of(contextPath, field.getName()).filter(StrUtil::isNotBlank).collect(Collectors.joining("."));
        paths.add(currentFieldPath);

        // 当前字段加进去之后, 看看字段类型是否是 innerClass
        String fieldEntityClassName = field.getType().getCanonicalText();
        PsiClass fieldEntityPsiClass = Arrays.stream(innerClasses)
            .filter(clazz -> {
                // 类型完全一致
                String innerClassQualifiedName = clazz.getQualifiedName();
                if (StrUtil.equals(innerClassQualifiedName, fieldEntityClassName)) {
                    return true;
                }
                // 可能是包含的那种类型, 如 fieldEntityClassName= List<clazz>
                return StrUtil.contains(fieldEntityClassName, "<" + innerClassQualifiedName + ">");
            })
            .findFirst().orElse(null);
        if (Objects.nonNull(fieldEntityPsiClass)) {
            // 有有对应的 innerClass
            // 获取对应的字段
            PsiField[] innerFields = fieldEntityPsiClass.getFields();
            for (PsiField innerField : innerFields) {
                extractInnerClassFieldPath(paths, currentFieldPath, innerField, innerClasses);
            }
        }
    }

    // protected static TreeModel getDefaultTreeModel() {
    // DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
    // DefaultMutableTreeNode parent;
    //
    // parent = new DefaultMutableTreeNode("colors");
    // root.add(parent);
    // parent.add(new DefaultMutableTreeNode("blue"));
    // parent.add(new DefaultMutableTreeNode("violet"));
    // parent.add(new DefaultMutableTreeNode("red"));
    // parent.add(new DefaultMutableTreeNode("yellow"));
    //
    // parent = new DefaultMutableTreeNode("sports");
    // root.add(parent);
    // parent.add(new DefaultMutableTreeNode("basketball"));
    // parent.add(new DefaultMutableTreeNode("soccer"));
    // parent.add(new DefaultMutableTreeNode("football"));
    // parent.add(new DefaultMutableTreeNode("hockey"));
    //
    // parent = new DefaultMutableTreeNode("food");
    // root.add(parent);
    // parent.add(new DefaultMutableTreeNode("hot dogs"));
    // parent.add(new DefaultMutableTreeNode("pizza"));
    // parent.add(new DefaultMutableTreeNode("ravioli"));
    // parent.add(new DefaultMutableTreeNode("bananas"));
    // return new DefaultTreeModel(root);
    // }
}
