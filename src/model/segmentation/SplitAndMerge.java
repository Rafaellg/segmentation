package model.segmentation;

import java.util.*;

import model.FeatureMatrix;
import model.SegmentationObserver;

/**
 * Metodo split and merge para segmentacao.
 */
public class SplitAndMerge extends SegmentationAlgorithm {

	private FeatureMatrix image;
	private SegmentationObserver observer;

	private double splitStandardDeviation = 5;
	private double mergeStandardDeviation = 10;
	private int minSize = 3;

	public void process(FeatureMatrix image, SegmentationObserver observer, HashMap<String, String> params) {
		this.image = image;
		this.observer = observer;

		// Parametros da segmentacao
		String s = params.get("splitStandardDeviation");
		System.out.println(s);
		if (s != null) {
			splitStandardDeviation = Double.parseDouble(s);
		}
		s = params.get("mergeStandardDeviation");
		System.out.println(s);
		if (s != null) {
			mergeStandardDeviation = Double.parseDouble(s);
		}
		s = params.get("minSize");
		System.out.println(s);
		if (s != null) {
			minSize = Integer.parseInt(s);
		}

		System.out.println("Executando split and merge " + splitStandardDeviation + " " + mergeStandardDeviation + " " + minSize);
	}

	/**
	 * Dada uma lista de segmentos, os ordena de menor para maior (tamanho), elimina os que contenham zonas (segmentos vazios)
	 * e redistribui os numeros de segmento desde 1
	 *
	 * @param segments
	 *            Lista de segmentos a processar.
	 */
	private void sortAndRemoveEmpty(ArrayList<ImageSegment> segments) {

		List<ImageSegment> auxList = new ArrayList<>();
		for (ImageSegment s : segments) {
			if (s.size() != 0) {
				auxList.add(s);
			}
		}

		ImageSegment[] segmentsArray = new ImageSegment[auxList.size()];
		Arrays.sort(auxList.toArray(segmentsArray),
				Comparator.comparingInt(ImageSegment::size));

        /*Arrays.sort(auxList.toArray(segmentsArray),
        new Comparator<ImageSegment>() {
            public int compare(ImageSegment arg0, ImageSegment arg1) {
                return arg0.size() - arg1.size();
            }
        });*/

		segments.clear();
		int index = 1;
		for (ImageSegment s : segmentsArray) {
			segments.add(s);
			s.segmentIndex = index++;
		}
	}

	public void run() {
		Set<ImageZone> zones = new HashSet<>();
		int currentSegmentIndex = 1;
		boolean changed = true;

		zones.add(new ImageZone(0, image.getWidth(), 0, image.getHeight(),
				currentSegmentIndex));

		/* SPLIT */
		while (changed) {
			if (Thread.interrupted())
				return;
			Set<ImageZone> auxZones = new HashSet<>();

			changed = false;

			for (ImageZone zone : zones) {

				// Pinta os segmentos na imagem
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						image.getSegment()[i][j] = (byte) zone.segment.segmentIndex;
					}
				}

				// Determina se o segmento deve ser dividido ou nao
				if (zone.isHomogeneus() || zone.size() <= minSize
						|| zone.xTo - zone.xFrom == 1
						|| zone.yTo - zone.yFrom == 1) {

					// Nao divide
					auxZones.add(zone);
				} else {

					// Divide
					changed = true;
					auxZones.addAll(Arrays.asList(zone.divide(currentSegmentIndex)));
					currentSegmentIndex += 4;
				}
			}

			observer.onChange();
			zones = auxZones;
		}

		/* COSMOVISION */
		ArrayList<ImageSegment> segments = new ArrayList<>();
		for (ImageZone zone : zones) {
			if (Thread.interrupted())
				return;

			ImageSegment currentSegment = zone.segment;

			Set<ImageZone> neighbourZones = new HashSet<>();
			neighbourZones.addAll(zone.north);
			neighbourZones.addAll(zone.south);
			neighbourZones.addAll(zone.east);
			neighbourZones.addAll(zone.west);

			for (ImageZone z : neighbourZones) {
				if (z.segment != currentSegment) {
					currentSegment.neighbours.add(z.segment);
				}
			}
			segments.add(currentSegment);
		}

		/* MERGE */
		changed = true;
		while (changed) {
			if (Thread.interrupted())
				return;
			changed = false;

			// Elimina os segmentos que nao possuam zonas e ordena a lista de menor para maior (tamanho)
			sortAndRemoveEmpty(segments);

			// Itera sobre os segmentos e busca se pode juntar com alguem com algum vizinho
			for (ImageSegment segment : segments) {

				// Verifica se as areas permanecem sem qualquer salto
				if (segment.zones.size() == 0)
					continue;

				// Busca o melhor vizinho homogeneo
				ImageSegment bestNeighbour = segment
						.getBestHomogeneousNeighbour();

				// Se encontrado, me uno a ele
				if (bestNeighbour != null) {

					segment.mergeWithNeighbour(bestNeighbour);
					changed = true;

					// Repinta o segmento na imagem
					for (ImageZone zone : segment.zones) {
						for (int i = zone.yFrom; i < zone.yTo; i++) {
							for (int j = zone.xFrom; j < zone.xTo; j++) {
								image.getSegment()[i][j] = (byte) (segment.segmentIndex);
							}
						}
					}
				}
			}
			observer.onChange();
		}
		if (observer != null) {
			observer.onComplete();
		}
	}

	// ---------- Objetos

	/**
     * Um segmento de uma imagem. Formado por varias ImageZone. Contem um conjunto de vizinho nas quatro direcoes.
	 */
	class ImageSegment {

		private Set<ImageSegment> neighbours = new HashSet<>();
		private Set<ImageZone> zones = new HashSet<>();
		private int segmentIndex;

		/**
         * Cria um novo segmento com uma unica zona e um numero especifico de segmentos
		 */
        ImageSegment(ImageZone zone, int segment) {
			this.zones.add(zone);
			zone.segment = this;
			this.segmentIndex = segment;
		}

		/**
         * Busca entre todos os vizinhos aqueles que sao homogeneos, e de todos estes retorna o que tem o menor desvio no vetor
         * e se juntara com ele
		 *
		 * @return O melhor vertice homogeneo
		 */
        ImageSegment getBestHomogeneousNeighbour() {

			double bestNeighbourDistance = Double.MAX_VALUE;
			ImageSegment bestNeighbour = null;

			// Busca o melhor vizinho
			for (ImageSegment neighbour : this.neighbours) {
				if (neighbour.size() == 0) {
					continue;
				}
				if (this.isHomogeneousWithRespectTo(neighbour)) {
					if (this.distanceWithNeighbour(neighbour) < bestNeighbourDistance) {
						bestNeighbourDistance = this
								.distanceWithNeighbour(neighbour);
						bestNeighbour = neighbour;
					}
				}
			}
			return bestNeighbour;
		}

		/**
         * Verifica se o segmento atual e homogeneo com outro segmento
		 */
        boolean isHomogeneousWithRespectTo(ImageSegment segment) {
			for (int i = 0; i < image.getDepth(); i++) {
				if (this.distanceWith(segment, i) > mergeStandardDeviation)
					return false;
			}
			return true;
		}

		/**
         * Junta o segmento atual com outro. Todas as zonas do outro sao transferidas para o atual, assim como os vizinhos,
         * e todas as referencias sao atualizadas. Posteriormente, o segmento passado por parametro podera ser eliminado da
         * lista de segmentos
		 * 
		 * @param segment
		 *            Segmentos que deve ser unido.
		 */
        void mergeWithNeighbour(ImageSegment segment) {

			segment.segmentIndex = 0;

			// Mover todas as zonas de segmento vazia para mim
			for (ImageZone zone : segment.zones) {
				zone.segment = this;
			}
			this.zones.addAll(segment.zones);
			segment.zones.clear();

			// Troca as referencias dos vizinhos do segmento.
			for (ImageSegment neighbour : segment.neighbours) {
				neighbour.neighbours.remove(segment); /* siempre */

				// Nao me considero vizinho
				if (neighbour != this) {
					neighbour.neighbours.add(this);
				}
			}

			// Adiciono ele a meus vizinhos no segmento
			this.neighbours.addAll(segment.neighbours);
			this.neighbours.remove(this);

			// Excluimos os vizinhos e as zonas do mesmo
			segment.neighbours.clear();
			segment.zones.clear();
		}

		/**
         * Calcula o desvio padrao de cada componente do vetor a respeito do vizinho que recebe como argumento.
		 * 
		 * @param segment
		 *            Vizinho usado para medir a distancia
		 * @return Desvio padrao.
		 */
        double distanceWithNeighbour(ImageSegment segment) {
			double acum = 0;
			for (int i = 0; i < image.getDepth(); i++) {
				acum += Math.pow(this.distanceWith(segment, i), 2);
			}
			return Math.sqrt(acum);
		}

		/**
         * Calcula o desvio padrao de todos os pixels do segmento atual com os do segmento, para um componente do vetor.
		 * 
		 * @param segment
		 *            Segmento que sera comparado.
		 * @param feature
		 *            Dimensao a avaliar o vetor
		 * @return Desvio padrao da dimensao.
		 */
		private double distanceWith(ImageSegment segment, int feature) {

			double mean = 0;
			double standardDeviation = 0;
			int count = 0;

			for (ImageZone zone : zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						mean += image.getData()[i][j][feature];
						count++;
					}
				}
			}

			for (ImageZone zone : segment.zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						mean += image.getData()[i][j][feature];
						count++;
					}
				}
			}

			mean = mean / count;

			for (ImageZone zone : zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						standardDeviation += Math.pow(
								image.getData()[i][j][feature] - mean, 2);
					}
				}
			}
			for (ImageZone zone : segment.zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						standardDeviation += Math.pow(
								image.getData()[i][j][feature] - mean, 2);
					}
				}
			}
			standardDeviation = Math.sqrt(standardDeviation / count);
			return standardDeviation;
		}

		/**
         * Calcula o tamanho de um segmento com a somatoria dos tamanhos das zonas contidas nele
		 * 
		 * @return Quantidade de pixels que incluem o segmento.
		 */
		int size() {
			int count = 0;
			for (ImageZone zone : zones) {
				count += (zone.yTo - zone.yFrom) * (zone.xTo - zone.xFrom);
			}
			return count;
		}
	}

	/**
     * Um quadrado da imagem. Um segmento e formado por varios deste.
	 */
	class ImageZone {
		private int xFrom;
		private int yFrom;
		private int xTo;
		private int yTo;

		private ImageSegment segment;

		private Set<ImageZone> east = new HashSet<>();
		private Set<ImageZone> west = new HashSet<>();
		private Set<ImageZone> north = new HashSet<>();
		private Set<ImageZone> south = new HashSet<>();

		ImageZone(int x_from, int x_to, int y_from, int y_to,
				int segmentIndex) {

			if (x_to <= x_from || y_to <= y_from) {
				throw new RuntimeException(
						"Nao pode existir uma zona menor do que 1 pixel.");
			}

			this.xFrom = x_from;
			this.yFrom = y_from;
			this.xTo = x_to;
			this.yTo = y_to;

			this.segment = new ImageSegment(this, segmentIndex);
		}

		/**
         * Divide um segmento que contem apenas uma zona e retorna 4 segmentos, cada um com 1 zona.
		 */
		ImageZone[] divide(int segment) {

			int x_mid = xFrom + ((xTo - xFrom) / 2);
			int y_mid = yFrom + ((yTo - yFrom) / 2);

			ImageZone nw = new ImageZone(xFrom, x_mid, yFrom, y_mid, segment);
			ImageZone ne = new ImageZone(x_mid, xTo, yFrom, y_mid, segment + 1);
			ImageZone sw = new ImageZone(xFrom, x_mid, y_mid, yTo, segment + 2);
			ImageZone se = new ImageZone(x_mid, xTo, y_mid, yTo, segment + 3);

			// Define para os vizinhos do norte externos
			nw.north = new HashSet<>();
			ne.north = new HashSet<>();
			for (ImageZone neighbour : north) {
				neighbour.south.remove(this);

				if (neighbour.xFrom < nw.xTo) {
					nw.north.add(neighbour);
					neighbour.south.add(nw);
				}

				if (neighbour.xTo > ne.xFrom) {
					ne.north.add(neighbour);
					neighbour.south.add(ne);
				}
			}

            // Define para os vizinhos do sul externos
			sw.south = new HashSet<>();
			se.south = new HashSet<>();
			for (ImageZone neighbour : south) {
				neighbour.north.remove(this);

				if (neighbour.xFrom < sw.xTo) {
					sw.south.add(neighbour);
					neighbour.north.add(sw);
				}

				if (neighbour.xTo > se.xFrom) {
					se.south.add(neighbour);
					neighbour.north.add(se);
				}
			}

            // Define para os vizinhos do norte oeste
			nw.west = new HashSet<>();
			sw.west = new HashSet<>();
			for (ImageZone neighbour : west) {
				neighbour.east.remove(this);

				if (neighbour.yFrom < nw.yTo) {
					nw.west.add(neighbour);
					neighbour.east.add(nw);
				}

				if (neighbour.yFrom < sw.yTo) {
					sw.west.add(neighbour);
					neighbour.east.add(sw);
				}
			}

            // Define para os vizinhos do norte leste
			ne.east = new HashSet<>();
			se.east = new HashSet<>();
			for (ImageZone neighbour : east) {
				neighbour.west.remove(this);

				if (neighbour.yFrom < ne.yTo) {
					ne.east.add(neighbour);
					neighbour.west.add(ne);
				}

				if (neighbour.yFrom < se.yTo) {
					se.east.add(neighbour);
					neighbour.west.add(se);
				}
			}

			/* Vecinos internos */
			nw.south = new HashSet<>(Collections.singletonList(sw));
			nw.east = new HashSet<>(Collections.singletonList(ne));
			ne.west = new HashSet<>(Collections.singletonList(nw));
			ne.south = new HashSet<>(Collections.singletonList(se));
			sw.north = new HashSet<>(Collections.singletonList(nw));
			sw.east = new HashSet<>(Collections.singletonList(se));
			se.north = new HashSet<>(Collections.singletonList(ne));
			se.west = new HashSet<>(Collections.singletonList(sw));

			return new ImageZone[] { nw, ne, sw, se };
		}

		/**
         * Verifica se o segmento atual cumpre o criterio de homogenidade.
		 */
		boolean isHomogeneus() {
			for (int i = 0; i < image.getDepth(); i++) {
				if (this.standardDeviation(i) > splitStandardDeviation)
					return false;
			}
			return true;
		}

		double standardDeviation(int feature) {
			double mean = mean(feature);
			double acum = 0;
			int count = 0;

			for (int i = yFrom; i < yTo; i++) {
				for (int j = xFrom; j < xTo; j++) {
					acum += Math.pow(image.getData()[i][j][feature] - mean, 2);
					count++;
				}
			}

			acum = acum / count;
			return Math.sqrt(acum);
		}

		double mean(int feature) {
			double acum = 0;
			int count = 0;

			for (int i = yFrom; i < yTo; i++) {
				for (int j = xFrom; j < xTo; j++) {
					acum += image.getData()[i][j][feature];
					count++;
				}
			}

			return acum / count;
		}

		int size() {
			return (xTo - xFrom) * (yTo - yFrom);
		}
	}

}
