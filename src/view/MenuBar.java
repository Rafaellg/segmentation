package view;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

public class MenuBar extends JMenuBar {

	private static final long serialVersionUID = 1L;

	private Controller ctrl;

	public MenuBar(Controller controller) {
		super();
		ctrl = controller;
		add(getFileMenu());
		add(getEditMenu());
		add(getViewMenu());
		add(getHelpMenu());
	}

	public JMenu getFileMenu() {
		JMenu menu = new JMenu("Arquivo");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.add(getMenuItem("Abrir...", "open", KeyEvent.VK_A, 'O'));
		menu.add(getMenuItem("Salvar como...", "saveAs", KeyEvent.VK_G, 'S'));
		menu.add(getMenuItem("Sair", "quit", KeyEvent.VK_S, 'Q'));
		return menu;
	}

	public JMenu getEditMenu() {
		JMenu menu = new JMenu("Editar");
		menu.setMnemonic(KeyEvent.VK_E);
		menu.add(getMenuItem("Desfazer tudo", "undoAll", KeyEvent.VK_D, 'Z'));
		menu.add(getMenuItem("Segmentar...", "applySegmentation", KeyEvent.VK_S, 'G'));

		return menu;
	}

	public JMenu getViewMenu() {
		JMenu menu = new JMenu("Ver");
		menu.setMnemonic(KeyEvent.VK_V);
		menu.add(getMenuItem("Zoom in", "zoomIn", KeyEvent.VK_C, 'K'));
		menu.add(getMenuItem("Zoom out", "zoomOut", KeyEvent.VK_L, 'L'));
		menu.add(getMenuItem("Escala original", "zoomOriginal", KeyEvent.VK_O,
				'0'));
		menu.add(getMenuItem("Exibição horizontal",
				"viewHorizontal", KeyEvent.VK_I, ','));
		menu.add(getMenuItem("Exibição vertical", "viewVertical",
				KeyEvent.VK_A, '.'));
		return menu;
	}

	public JMenu getHelpMenu() {
		JMenu menu = new JMenu("Ajuda");
		menu.setMnemonic(KeyEvent.VK_Y);
		menu.add(getMenuItem("Sobre", "about", KeyEvent.VK_A, 'A'));
		return menu;
	}

	public JMenuItem getMenuItem(String label, String callback, int mnemonic,
			char accel) {
		JMenuItem item = new JMenuItem(label, mnemonic);
		item.setAccelerator(KeyStroke
				.getKeyStroke(accel, ActionEvent.CTRL_MASK));
		item.addActionListener(new ActionListener(ctrl, callback));
		return item;
	}

	public JMenuItem getMenuItem(String label, String callback, int mnemonic,
			int accel) {
		JMenuItem item = new JMenuItem(label, mnemonic);
		item.setAccelerator(KeyStroke.getKeyStroke(accel, 0));
		item.addActionListener(new ActionListener(ctrl, callback));
		return item;
	}
}
