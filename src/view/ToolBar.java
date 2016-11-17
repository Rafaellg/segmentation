package view;

import javax.swing.*;

import view.ActionListener;
import view.Controller;

public class ToolBar extends JToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Controller ctrl;

	public ToolBar(Controller controller) {
		super();
		ctrl = controller;
		setFloatable(false);
		add(getButton("Abrir...", "icon-open", "open"));
		add(getButton("Salvar como...", "icon-save", "saveAs"));
		add(getButton("Desfazer tudo", "icon-undo", "undoAll"));
		add(getButton("Segmentar...", "icon-segmentation", "applySegmentation"));
		add(getButton("Zoom in", "icon-zoom-in", "zoomIn"));
		add(getButton("Zoom out", "icon-zoom-out", "zoomOut"));
		add(getButton("Escala original", "icon-original", "zoomOriginal"));
		add(getButton("Exibição horizontal", "icon-horizontal", "viewHorizontal"));
		add(getButton("Exibição vertical", "icon-vertical", "viewVertical"));
	}

	private JButton getButton(String label, String icon, String action) {
		JButton button = new JButton();
		button.setToolTipText(label);
		button.setIcon(new ImageIcon("resources/icons/" + icon + ".png"));
		button.addActionListener(new ActionListener(ctrl, action));
		return button;
	}

	public JMenuItem getMenuItem(String label, String callback) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(new ActionListener(ctrl, callback));
		return item;
	}
	
}
