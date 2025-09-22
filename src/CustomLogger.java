import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomLogger implements BurpExtension {
    private MontoyaApi api;
    private HeaderTableModel headerTableModel;
    private JTable headerTable; // 保存表格引用以便操作

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Header Manager");
        api.logging().logToOutput("自定义请求头管理器插件加载成功！");
        api.logging().logToOutput("功能：添加自定义请求头并自动附加到所有HTTP请求");
        api.logging().logToOutput("当前版本：1.0");
        api.logging().logToOutput("author:fly");

        headerTableModel = new HeaderTableModel();
        headerTable = new JTable(headerTableModel);

        // 注册HTTP处理器（关键步骤）
        api.http().registerHttpHandler(new MyHttpHandler(headerTableModel));

        // 注册插件标签页
        api.userInterface().registerSuiteTab("Header Manager", constructHeaderManagerTab());
    }

    private Component constructHeaderManagerTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 标题
        JLabel titleLabel = new JLabel("请求头记录管理");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton addButton = new JButton("添加请求头");
        JButton deleteButton = new JButton("删除选中记录");
        JButton clearButton = new JButton("清空所有记录");

        addButton.addActionListener(e -> showAddHeaderDialog());
        deleteButton.addActionListener(e -> deleteSelectedHeaders());
        clearButton.addActionListener(e -> clearAllHeaders());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        // 配置表格以更好地显示请求头记录
        configureHeaderTable();

        JScrollPane headerScrollPane = new JScrollPane(headerTable);
        headerScrollPane.setBorder(BorderFactory.createTitledBorder("请求头记录列表"));

        // 组装主面板
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(headerScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 状态栏显示记录数量
        JLabel statusLabel = new JLabel("当前记录数: 0");
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // 监听表格模型变化以更新状态
        headerTableModel.addTableModelListener(e ->
                statusLabel.setText("当前记录数: " + headerTableModel.getRowCount())
        );

        return mainPanel;
    }

    // 配置表格以优化请求头记录的显示
    private void configureHeaderTable() {
        // 设置列宽
        headerTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        headerTable.getColumnModel().getColumn(1).setPreferredWidth(350);

        // 设置表格样式
        headerTable.setRowHeight(25);
        headerTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        headerTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // 设置交替行颜色，提高可读性
        headerTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                return c;
            }
        });
    }

    // 显示添加请求头对话框
    private void showAddHeaderDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField nameField = new JTextField();
        JTextField valueField = new JTextField();

        panel.add(new JLabel("请求头名称:"));
        panel.add(nameField);
        panel.add(new JLabel("请求头值:"));
        panel.add(valueField);

        int result = JOptionPane.showConfirmDialog(
                api.userInterface().swingUtils().suiteFrame(),
                panel,
                "添加新请求头",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();

            if (!name.isEmpty()) {
                headerTableModel.addHeader(name, value);
                // 自动选中最后一行
                int lastRow = headerTableModel.getRowCount() - 1;
                headerTable.setRowSelectionInterval(lastRow, lastRow);
            } else {
                JOptionPane.showMessageDialog(
                        api.userInterface().swingUtils().suiteFrame(),
                        "请求头名称不能为空",
                        "输入错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // 删除选中的请求头记录
    private void deleteSelectedHeaders() {
        int[] selectedRows = headerTable.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "请先选择要删除的请求头记录",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 确认删除
        int confirm = JOptionPane.showConfirmDialog(
                api.userInterface().swingUtils().suiteFrame(),
                "确定要删除选中的 " + selectedRows.length + " 条记录吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // 从后往前删除，避免索引问题
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                headerTableModel.removeHeader(selectedRows[i]);
            }
        }
    }

    // 清空所有请求头记录
    private void clearAllHeaders() {
        if (headerTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "没有记录可清空",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                api.userInterface().swingUtils().suiteFrame(),
                "确定要清空所有请求头记录吗？",
                "确认清空",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            headerTableModel.clearAll();
        }
    }

    // 用于存储和管理请求头记录的表格模型
    class HeaderTableModel extends AbstractTableModel {
        private List<HeaderEntry> headers = new ArrayList<>();
        private String[] columnNames = {"请求头名称", "请求头值"};

        // 返回所有用户添加的请求头（安全包装，防止外部修改内部列表）
        public List<HeaderEntry> getAllHeaders() {
            return new ArrayList<>(headers); // 返回副本，避免内部数据被意外篡改
        }
        public void addHeader(String name, String value) {
            headers.add(new HeaderEntry(name, value));
            fireTableRowsInserted(headers.size() - 1, headers.size() - 1);
        }

        public void removeHeader(int index) {
            if (index >= 0 && index < headers.size()) {
                headers.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }

        public void clearAll() {
            int size = headers.size();
            if (size > 0) {
                headers.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }



        @Override
        public int getRowCount() {
            return headers.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HeaderEntry entry = headers.get(rowIndex);
            return columnIndex == 0 ? entry.name : entry.value;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            HeaderEntry entry = headers.get(rowIndex);
            if (columnIndex == 0) {
                entry.name = aValue.toString().trim();
            } else {
                entry.value = aValue.toString().trim();
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    // 请求头实体类
    class HeaderEntry {
        String name;
        String value;

        HeaderEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
