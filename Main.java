// http://rain.ifmo.ru/cat/view.php/theory/unsorted/genetic-2005

import java.util.*;

public class Main {
	
	int BOARD_SIZE = 9;
	int L = BOARD_SIZE * BOARD_SIZE;
	int N = 250; //population size
	Random rnd = new Random(566);
	int ITERS = 500;
	int A = 10; //for collisions
	int B = 2; //for number
	double mutationProbability = 0.005;
	double crossoverProbability = 0.1;
	
	class Individual implements Cloneable {
		BitSet data;
		int fitness;
		int collision;
		
		Individual (BitSet data) {
			this.data = data;
			this.recalc();
		}
		
		public int getFitness() {
			return fitness;
		}

		private void recalc() {
			int res = 0;
			for (int i = 0; i < L; ++i) {
				for (int j = i + 1; j < L; ++j) {
					if (data.get(i) && data.get(j) && beat(i, j)) {
						++res;
					}
				}
			}
			collision = res;
			fitness = fitnessFunction(data.cardinality(), res);
		}
		
		/*fitness function must be non-negative
		+placed, -res
		individual with placed = n, res = 0 has max fitness
		*/
		private int fitnessFunction(int figuresPlaced, int res) {
			int maximalCollisions = L * (L - 1) / 2;
			int buff = B * figuresPlaced;
			int debuff = A * (maximalCollisions - res);
			return buff + debuff;
		}
		
		private boolean beat(int i, int j) {
			int x1 = i / BOARD_SIZE;
			int y1 = i % BOARD_SIZE;
			int x2 = j / BOARD_SIZE;
			int y2 = j % BOARD_SIZE;
			boolean king = Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2)) <= 1;
			boolean knight = Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 2;
			knight |= Math.abs(y1 - y2) == 1 && Math.abs(x1 - x2) == 2;
			return king || knight;
		}

		@Override
		protected Object clone() {
			try {
				Individual res = (Individual) super.clone();
				res.data = (BitSet) this.data.clone();
				res.recalc();
				return res;
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException();
			}
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < BOARD_SIZE; ++i) {
				for (int j = 0; j < BOARD_SIZE; ++j) {
					int bit = BOARD_SIZE * i + j;
					sb.append(data.get(bit) ? '*' : '#');
				}
				sb.append('\n');
			}
			return sb.toString();
		}
		
	}
	
	class CrossoverPair {
		Individual first;
		Individual second;
		
		CrossoverPair(Individual first, Individual second) {
			this.first = first;
			this.second = second;
		}
		
	}
	
	private void run() {
		Individual[] population = new Individual[N];
		int startBest = Integer.MIN_VALUE;
		Individual startRes = null;
		for (int i = 0; i < N; ++i) {
			population[i] = new Individual(makeRandom());
			if (population[i].fitness > startBest) {
				startBest = population[i].fitness;
				startRes = population[i];
			}
		}
		for (int iter = 0; iter < ITERS; ++iter) {
			Individual[] nextPopulation = select(population);
			globalCrossover(nextPopulation);
			mutate(nextPopulation);
			population = nextPopulation;
		}
		int best = Integer.MIN_VALUE;
		Individual res = null;
		for (Individual x: population) {
			x.recalc();
			if (x.fitness > best) {
				best = x.fitness;
				res = x;
			}
		}
		System.out.printf("size=%d, max_collisions=%d\n", L,
				L * (L - 1) / 2);
		System.out.printf("placed=%d, collisions=%d, fitness=%d\n",
				res.data.cardinality(), res.collision, res.fitness);
		System.out.printf("start solution had:\nplaced=%d, collisions=%d, fitness=%d\n",
				startRes.data.cardinality(), startRes.collision, startRes.fitness);
		System.out.println();
		System.out.println(res);
	}
	
	private void mutate(Individual[] nextPopulation) {
		for (Individual x: nextPopulation) {
			for (int i = 0; i < L; ++i) {
				if (rnd.nextDouble() <= mutationProbability) {
					x.data.flip(i);
				}
			}
		}
	}

	private void globalCrossover(Individual[] population) {
		//create random permutation that will split population into pairs
		int[] p = new int[population.length];
		for (int i = 0; i < p.length; ++i) p[i] = i;
		for (int i = 0; i < p.length; ++i) {
			int with = rnd.nextInt(i + 1);
			int tmp = p[with];
			p[with] = p[i];
			p[i] = tmp;
		}
		for (int i = 0; i + 1 < population.length; i += 2) {
			if (rnd.nextDouble() <= crossoverProbability) {
				CrossoverPair res = cross(population[p[i]], population[p[i + 1]]);
				population[p[i]] = res.first;
				population[p[i + 1]] = res.second;
			}
		}
	}

	private CrossoverPair cross(Individual p1, Individual p2) {
		Individual ch1 = (Individual) p1.clone();
		Individual ch2 = (Individual) p2.clone();
		int pivot = rnd.nextInt(L);
		for (int bit = 0; bit < pivot; ++bit) {
			boolean tmp = ch1.data.get(bit);
			ch1.data.set(bit, ch2.data.get(bit));
			ch2.data.set(bit, tmp);
		}
		return new CrossoverPair(ch1, ch2);
	}

	private Individual[] select(Individual[] population) {
		Individual[] res = new Individual[N];
		int sumFitness = Arrays.stream(population)
				.peek(Individual::recalc)
				.mapToInt(Individual::getFitness)
				.sum();
		outer:
		for (int i = 0; i < N; ++i) {
			int value = rnd.nextInt(sumFitness);
			int partial = 0;
			for (int who = 0; who < N; ++who) {
				assert(population[who].fitness >= 0);
				partial += population[who].fitness;
				if (partial > value) {
					res[i] = (Individual) population[who].clone();
					continue outer;
				}
			}
		}
		return res;
	}

	private BitSet makeRandom() {
		BitSet res = new BitSet(L);
		randomize(res);
		return res;
	}

	private void randomize(BitSet field) {
		for (int i = 0; i < L; ++i) {
			if (rnd.nextBoolean()) field.set(i);
		}
	}

	public static void main(String[] args) {
		new Main().run();
	}

}
