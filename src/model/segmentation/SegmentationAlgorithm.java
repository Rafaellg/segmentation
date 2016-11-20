package model.segmentation;

import model.FeatureMatrix;
import model.SegmentationObserver;

import java.util.HashMap;

/**
 * Interface para os metodos de segmentacao. Extende de <code>Runnable</code> ja que os metodos devem ser threads.
 */
public abstract class SegmentationAlgorithm extends Thread {

	/**
	 * Estabelece os parametros para ser usados na segmentacao
	 *
	 * @param image Imagem a segmentar.
	 * @param observer Observador a invocar para cada iteracao.
	 * @param params Parametros para a segmentacao.
	 */
	public abstract void process(FeatureMatrix image, SegmentationObserver observer, HashMap<String, String> params);
}
