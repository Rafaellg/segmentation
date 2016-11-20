package model.segmentation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import model.FeatureMatrix;
import model.SegmentationObserver;

/**
 * Metodo K-means para segmentacao.
 */
public class KMeans extends SegmentationAlgorithm {

	private Cluster[] clusters;
	private FeatureMatrix image;
	private SegmentationObserver observer;
	private boolean useAllClusters = false;
	private int clustersCount = 10;

	public void process(FeatureMatrix image, SegmentationObserver observer, HashMap<String, String> params) {
		this.image = image;
		this.observer = observer;

		String s = params.get("useAllClusters");
		if (s != null) {
			useAllClusters = new Integer(s) != 0;
		}

		s = params.get("clustersCount");
		if (s != null) {
			clustersCount = new Integer(s);
		}

		this.clusters = new Cluster[clustersCount];

	}

	private void generateClusters() {
		boolean hasChanged = true;
		boolean[] clustersEmptyStatus = new boolean[clusters.length];		

		// Enquanto nao ha mudancas nos clusters
		while (hasChanged && !isInterrupted()) {
			for (int i = 0; i < clustersEmptyStatus.length; i++) {
				clustersEmptyStatus[i] = true;
            }
			hasChanged = false;

			// Coloca cada objeto no cluster mais proximo
			for (int k = 0; k < clusters.length; k++) {
				Cluster c = clusters[k];
				for (int i = 0; i < c.getObjects().size(); i++) {
					Point object = c.getObjects().get(i);
					int bestCluster = -1;

					double minDistance = Double.MAX_VALUE;

					// Busca o cluster que tem seu centroide mais proximo deste objeto
					for (int m = 0; m < clusters.length; m++) {

						// Se o cluster nao esta ocupado, coloca ele la
						if (useAllClusters && (clusters[m].getCentroid() == null && clustersEmptyStatus[m])) {
							bestCluster = m;
							minDistance = 0;
							clustersEmptyStatus[m] = false;
						} else {
							if (clusters[m].getCentroid() != null
									&& distanceBetween(KMeans.this.image
											.getData()[object.y][object.x],
											clusters[m].getCentroid()) < minDistance) {
								bestCluster = m;
								minDistance = distanceBetween(KMeans.this.image
										.getData()[object.y][object.x],
										clusters[m].getCentroid());
							}
						}
					}

					// Verifica se e necessario colocar o objeto em um novo cluster
					if (bestCluster != k) {
						hasChanged = true;
						KMeans.this.image.getSegment()[object.y][object.x] = (byte) bestCluster;
					}
				}

			}

			rebuildClusters();

			if (observer != null) {
				observer.onChange();
			}
		}
		if (observer != null) {
			observer.onComplete();
		}
	}

	private void rebuildClusters() {

		this.clusters = new Cluster[this.clusters.length];

		for (int i = 0; i < clusters.length; i++) {
			clusters[i] = new Cluster();
		}

		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				clusters[image.getSegment()[i][j]].add(new Point(j, i));
			}
		}

        for (Cluster cluster : clusters) {
            cluster.updateCentroid();
        }

	}

	private void randomInit() {

		Random r = new Random();
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				int clusterIndex = r.nextInt(clusters.length);
				image.getSegment()[i][j] = (byte) clusterIndex;
			}
		}

	}

	private double distanceBetween(int[] vec1, int[] vec2) {

		double dist = 0;

		for (int i = 0; i < vec1.length; i++) {
			dist += Math.pow(vec1[i] - vec2[i], 2);
		}

		return Math.sqrt(dist);
	}

	public void run() {
		randomInit();
		rebuildClusters();
		generateClusters();
	}

	// ---------- Objetos

	class Cluster {
		private List<Point> objects; /* Objetos que abrigam o ponto */
		private int[] centroid; /* Centroide */

        Cluster() {
			objects = new ArrayList<>();
			centroid = null;
		}

		public void add(Point o) {
		    // Nao se computa o centroide por questoes de performance
			objects.add(o);
		}

		void updateCentroid() {
			if (objects != null && objects.size() > 0) {
				centroid = euclideanMeanDistance(objects);
			} else {
				centroid = null;
			}
		}

		int[] getCentroid() {
			return centroid;
		}

		List<Point> getObjects() {
			return objects;
		}

		int[] euclideanMeanDistance(List<Point> points) {

			int[] info = new int[KMeans.this.image.getDepth()];
			int[] output = new int[info.length];

			for (Point p : points) {
				int[] data = KMeans.this.image.getData()[p.y][p.x];
				for (int i = 0; i < data.length; i++) {
					info[i] += data[i];
				}
			}

			for (int i = 0; i < info.length; i++) {
				output[i] = (info[i] / points.size());
			}

			return output;
		}
	}
}
