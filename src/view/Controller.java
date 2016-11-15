package view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import model.FeatureMatrix;
import model.ImageMatrix;
import model.SegmentationObserver;
import model.converters.ImageConverter;
import model.filters.AverageFilter;
import model.filters.BlurFilter;
import model.filters.EqualizeFilter;
import model.filters.FilterAlgorithm;
import model.filters.MaxFilter;
import model.filters.MaxMinFilter;
import model.filters.MidPointFilter;
import model.filters.ReduceResolutionFilter;
import model.filters.SharpenFilter;
import model.segmentation.SegmentationAlgorithm;

public class Controller {

	private CgTpe1 view;

	private BufferedImage original;

	private ImageMatrix matrix;

	private double zoom;

	private SegmentationAlgorithm algorithm;

	private ProgressDialog dialog;

	public Controller(CgTpe1 frame) {
		view = frame;
		original = null;
		matrix = null;
		zoom = 1.0;
		algorithm = null;
		dialog = null;
	}

	public void open() {
		File f = view.getFileFromFileChooser();
		if (f != null) {
			try {
				original = ImageIO.read(f);
			} catch (IOException e) {
				raiseException(String.format("Não foi possíveis abrir o arquivo:"
						+ "\n%s\n\nDetalhe: %s", f.toString(), e
						.getLocalizedMessage()));
			}
			undoAll();
		}
	}

	public void saveAs() {
		if (original == null || matrix == null) {
			raiseException("Sem imagens abertas.");
			return;
		}
		saveAs(view.saveFileWithFileChooser());
	}

	public void saveAs(File f) {
		if (f == null) {
			return;
		}
		try {
			String name = f.getName();
			int i = name.lastIndexOf('.');
			String extension = (i < 0 ? "png" : name.substring(i + 1));
			BufferedImage segmented = matrix.getBufferedImage();
			ImageIO.write(segmented, extension, f);
		} catch (IOException e) {
			view.showErrorDialog("Erro de gravação", String.format(
					"Não foi possível salvar o arquivo:\n%s\n\nDetalhe: %s", f
							.toString(), e.getLocalizedMessage()));
		}
	}

	public void undoAll() {
		if (original == null) {
			raiseException("Sem imagens abertas..");
			return;
		}

		zoom = 1.0;
		ImageView iv = view.getImageView();
		iv.redrawImages(original, original, zoom);
		// asumiendo que original == matrix.getBufferedImage()

		try {
			matrix = new ImageMatrix(original);
		} catch (IOException e) {
			raiseException(e.getMessage());
			return;
		}
	}

	public void quit() {
		view.quit();
	}

	public void blurFilter() {
		filter(new BlurFilter());
	}

	public void reduceResolutionFilter() {
		filter(new ReduceResolutionFilter());
	}

	public void sharpenFilter() {
		filter(new SharpenFilter());
	}

	public void maxFilter() {
		filter(new MaxFilter());
	}

	public void minFilter() {
		filter(new AverageFilter());
	}

	public void maxMinFilter() {
		filter(new MaxMinFilter());
	}

	public void midPointFilter() {
		filter(new MidPointFilter());
	}

	public void equalizeFilter() {
		filter(new EqualizeFilter());
	}

	public void filter(FilterAlgorithm fa) {
		if (original == null || matrix == null) {
			raiseException("Sem imagens abertas.");
			return;
		}
		ImageMatrix output = new ImageMatrix(matrix.getWidth(), matrix
				.getHeight());
		fa.filter(matrix, output);
		matrix = output;
		view.getImageView().redrawImage(matrix.getBufferedImage(), zoom);
	}

	public void applySegmentation() {
		if (original == null || matrix == null) {
			raiseException("Sem imagens abertas.");
			return;
		}
		OptionsPanel p = new OptionsPanel();
		p.setVisible(true);
		if (view.showConfirmDialog(p, "Segmentar")) {
			ImageConverter ic = p.getSelectedFeature(matrix);
			final FeatureMatrix fm = ic.createFeatureMatrix();
			getProgressDialog().open("Processando...");
			stopSegmentation();
			algorithm = p.getSelectedSegmentationMethod();
			algorithm.process(fm, new SegmentationObserver() {

				private int i = 0;

				public void onChange() {
					String s = String.format("Processando... (%d)",
							Integer.valueOf(++i));
					getProgressDialog().setLabel(s);
					matrix = fm.getImageMatrix();
					view.getImageView().redrawImage(matrix.getBufferedImage(),
							zoom);
				}

				public void onComplete() {
					getProgressDialog().close();
				}

			}, p.getSegmentationParameters());
			algorithm.start();
		}
	}

	public ProgressDialog getProgressDialog() {
		if (dialog == null) {
			ActionListener al = new ActionListener(this, "stopSegmentation");
			dialog = new ProgressDialog(view.createDialog(
					"Progresso da segmentação", false), al);
		}
		return dialog;
	}

	public void stopSegmentation() {
		if (algorithm == null) {
			return;
		}
		algorithm.interrupt();
		algorithm = null;
	}

	public void viewHorizontal() {
		view.getImageView().setHorizontalView();
	}

	public void viewVertical() {
		view.getImageView().setVerticalView();
	}

	public void zoomIn() {
		scale(zoom * 3.0 / 2.0);
	}

	public void zoomOut() {
		scale(zoom * 2.0 / 3.0);
	}

	public void zoomOriginal() {
		scale(1.0);
	}

	public void scale(double factor) {
		if (original == null || matrix == null) {
			raiseException("Sem imagens abertas.");
			return;
		}
		if (factor > 4.0) {
			raiseException("Não é possível ampliar mais.");
			return;
		}
		if (factor < 1.0 / 8.0) {
			raiseException("Não é possível distanciar mais.");
			return;
		}
		zoom = factor;
		BufferedImage segmented = matrix.getBufferedImage();
		view.getImageView().redrawImages(original, segmented, zoom);
	}

	public void about() {
		final String s = "Demonstração de Segmentação\n"
				+ "Adaptação de um trabalho de Computação Gráfica – ITBA – 2008.\n\n"
				+ "Autores da adaptação:\n"
				+ "\tRafael Guimarães de Sousa\n"
				+ "\tTainá Viriato Mendes\n"
				+ "\tAna Cristina Pereira\n"
				+ "\tMarley Ribeiro Luz\n\n"
				+ "Autores do trabalho original:\n"
				+ "\tRafael Martín Bigio <rbigio@alu.itba.edu.ar>\n"
				+ "\tSantiago Andrés Coffey <scoffey@alu.itba.edu.ar>\n"
				+ "\tAndrés Santiago Gregoire <agregoir@alu.itba.edu.ar>\n";
		view.showInformationDialog("Sobre o programa", s);
	}

	public void raiseException(String message) {
		view.showErrorDialog("Erro", message);
	}

}
