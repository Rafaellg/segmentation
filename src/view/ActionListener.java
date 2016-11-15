package view;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ActionListener implements java.awt.event.ActionListener, Runnable {

	Controller ctrl;

	Method method;

	public ActionListener(Controller controller, String action) {
		ctrl = controller;
		Class<Controller> c = Controller.class;
		try {
			method = c.getMethod(action, new Class<?>[] {});
		} catch (SecurityException e) {
			e.printStackTrace();
			ctrl.raiseException("Houve um erro inesperado associando a alguma ação do controlador.\n" + e.toString());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			ctrl.raiseException("Houve um erro inesperado associando a alguma ação do controlador..\n" + e.toString());
		}
	}

	public void actionPerformed(ActionEvent event) {
		new Thread(this).start();
	}

	public void run() {
		if (method == null) {
			return;
		}
		try {
			method.invoke(ctrl);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			ctrl.raiseException("Houve um erro inesperado associando a alguma ação do controlador..\n" + e.toString());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			ctrl.raiseException("Houve um erro inesperado associando a alguma ação do controlador..\n" + e.toString());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Throwable t = e.getCause();
			StackTraceElement[] st = t.getStackTrace();
			StringBuffer s = new StringBuffer();
			for (int i = 0; i < st.length; i++) {
				if (st[i].isNativeMethod()) {
					break;
				}
				s.append("\t\ten " + st[i].toString() + "\n");
			}
			ctrl.raiseException("Houve um erro inesperado no programa.\n" + t.toString() + "\n"
					+ s.toString());
		}
	}
}
