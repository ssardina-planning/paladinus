package main.java.paladinus.search.dfs.iterative;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import main.java.paladinus.heuristic.Heuristic;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.search.SearchConnector;
import main.java.paladinus.search.SearchFlag;
import main.java.paladinus.search.SearchNode;
import main.java.paladinus.search.dfs.DepthFirstSearch;
import main.java.paladinus.util.Pair;

/**
 * 
 * An iterative depth-first search algorithm for FOND Planning.
 * 
 * @author Ramon Fraga Pereira
 *
 */
public class IterativeDepthFirstSearch extends DepthFirstSearch {
	
	protected double POLICY_SIZE = 0;
	protected double NEW_POLICY_BOUND = 0;

	public IterativeDepthFirstSearch(Problem problem, Heuristic heuristic, String strategies, String criterion) {
		super(problem, heuristic, strategies, criterion);
	}
	
	@Override
	public Result run() {
		/* Start measuring search time. */
		starttime = System.currentTimeMillis();

		/* Get initial state and insert it with depth 0. */
		this.initialNode = this.lookupAndInsertNode(problem.getSingleInitialState(), 0);
		assert ((SearchNode) this.initialNode).getDepth() == 0;

		SearchFlag flag = doIterativeSearch(false, (SearchNode) this.initialNode);
		this.searchStatus = flag;

		/* Finish measuring search time. */
		endtime = System.currentTimeMillis();
		
		System.out.println("\n# Closed-Solved Nodes = " + closedSolvedNodes.size());
		
		if (DEBUG)
			dumpStateSpace(this.NUMBER_ITERATIONS);

		if(timeout())
			return Result.TIMEOUT;
		
		if (flag == SearchFlag.GOAL) {
			return Result.PROVEN;
		} else if (flag == SearchFlag.DEAD_END || flag == SearchFlag.NO_POLICY) {
			return Result.DISPROVEN;
		} else return Result.TIMEOUT;
	}
	
	protected SearchFlag doIterativeSearch(Boolean unitaryBound, SearchNode node) {
		if(unitaryBound) {
			this.POLICY_BOUND = 0;			
		} else {
			this.POLICY_BOUND = node.getHeuristic();
			if(this.POLICY_BOUND == Double.POSITIVE_INFINITY)
				return SearchFlag.NO_POLICY;
		}
		SearchFlag flag = SearchFlag.NO_POLICY;
		
		this.NEW_POLICY_BOUND = Double.POSITIVE_INFINITY;
		System.out.println("\n> Bound Initial: " + this.POLICY_BOUND);
		System.out.println();
		do {
			System.out.println("> Bound: " + this.POLICY_BOUND);
			this.POLICY_SIZE = 0d;
			
			this.dumpingCounterStateSpace = 0;
			this.NUMBER_ITERATIONS++;

			Set<SearchNode> closedSolved = new HashSet<>();
			this.closedVisitedNodes.clear();
			
			Pair<SearchFlag, Set<SearchNode>> resultSearch = doIterativeSearch(node, closedSolved, this.POLICY_SIZE, this.POLICY_BOUND);
			flag = resultSearch.first;
			this.closedSolvedNodes = resultSearch.second;
			
			if(unitaryBound) {
				this.POLICY_BOUND++;				
			} else this.POLICY_BOUND = this.NEW_POLICY_BOUND;
			
			this.NEW_POLICY_BOUND = Double.POSITIVE_INFINITY;
		} while (flag != SearchFlag.GOAL && this.POLICY_BOUND < Double.POSITIVE_INFINITY && flag != SearchFlag.TIMEOUT);
		return flag;
	}
	
	protected Pair<SearchFlag, Set<SearchNode>> doIterativeSearch(SearchNode node, Set<SearchNode> closedSolved, double policySize, double policyBound) {
		if (DEBUG)
			dumpStateSpace(this.NUMBER_ITERATIONS);
		
		if(timeout())
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.TIMEOUT, null);

		RECURSION_COUNTER++;
		
		if(node.isGoalNode() || closedSolved.contains(node)) {
			closedSolved.addAll(this.closedVisitedNodes);
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, closedSolved);
		} else if (this.closedVisitedNodes.contains(node))
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
		
		this.closedVisitedNodes.add(node);
		
		PriorityQueue<SearchConnector> connectors = this.getNodeConnectors(node);
		
		NODE_EXPANSIONS++;
		
		while(!connectors.isEmpty()) {
			SearchConnector c = connectors.poll();
			
			if(policySize + 1 + c.getEvaluationFunctionAccordingToCriterion() > policyBound && closedSolved.size() == 0) {
				if(policySize + 1 + c.getEvaluationFunctionAccordingToCriterion() < this.NEW_POLICY_BOUND )
					this.NEW_POLICY_BOUND = policySize + 1 + c.getEvaluationFunctionAccordingToCriterion();
			} else if(policySize + 1 > policyBound) {
				if(policySize + 1 < this.NEW_POLICY_BOUND)
					this.NEW_POLICY_BOUND = policySize + 1;
			} else {
				Set<SearchNode> pathsFound = new HashSet<>();
				
				boolean newGoalPathFound = true;
				
				Set<SearchNode> copyClosedSolved = new HashSet<>(closedSolved);
				
				while(newGoalPathFound == true) {
					newGoalPathFound = false;
					Set<SearchNode> findingGoalPath = new HashSet<>();
					
					for(SearchNode s: c.getChildren()) {
						if(!pathsFound.contains(s))
							findingGoalPath.add(s);
					}
					for(SearchNode s: findingGoalPath) {
						Pair<SearchFlag, Set<SearchNode>> resultSearch = doIterativeSearch(s, copyClosedSolved, policySize+1, policyBound);
						
						SearchFlag flag = resultSearch.first;
						copyClosedSolved = new HashSet<SearchNode>(resultSearch.second);
						
						if(flag == SearchFlag.GOAL){
							newGoalPathFound = true;
							pathsFound.add(s);
						}
					}
					if(pathsFound.size() == c.getChildren().size()) {
						this.closedVisitedNodes.remove(node);
						node.setMarkedConnector(c);
						return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, copyClosedSolved);
					}
				}
			}
		}
		this.closedVisitedNodes.remove(node);
		return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
	}
	
	@Override
	public void printStats(boolean simulatePlan) {
		System.out.println("# Number Iterations         = " + this.NUMBER_ITERATIONS);
		super.printStats(simulatePlan);
	}
}
