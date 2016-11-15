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
		add(getButton("Abrir...", "document-open", "open"));
		add(getButton("Salvar como...", "media-floppy", "saveAs"));
		add(getButton("Desfazer tudo", "edit-undo", "undoAll"));
		add(getButton("Segmentar...", "stock_filters-pop-art", "applySegmentation"));
		add(getButton("Zoom in", "zoom-in", "zoomIn"));
		add(getButton("Zoom out", "zoom-out", "zoomOut"));
		add(getButton("Escala original", "zoom-original", "zoomOriginal"));
		add(getButton("Exibição horizontal", "stock_view-left-right", "viewHorizontal"));
		add(getButton("Exibição vertical", "stock_view-top-bottom", "viewVertical"));
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
