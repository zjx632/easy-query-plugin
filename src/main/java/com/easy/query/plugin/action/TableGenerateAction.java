package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.windows.EntityTableGenerateDialog;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TableGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        EntityTableGenerateDialog entityTableGenerateDialog = new EntityTableGenerateDialog(e);
        SwingUtilities.invokeLater(() -> {
            entityTableGenerateDialog.setVisible(true);
        });
    }
    /**
     * 判断选中的是否是表，是表则显示，否则不显示
     *
     * @param e 事件
     */
    @Override
    public void update(AnActionEvent e) {
        PsiElement[] psiElements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY);

        if (psiElements == null || psiElements.length == 0) {
            return;
        }

        boolean isSelectedTable = psiElements[0] instanceof DasTable;
        e.getPresentation().setVisible(isSelectedTable);
    }

    /**
     * 2022.2.5才有的方法
     * @return
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
