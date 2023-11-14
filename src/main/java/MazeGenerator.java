import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javalib.worldimages.*;

// Represents a Cell in a Graph
class Cell {
  // distance from left of maze
  int x;

  // distance from top of maze
  int y;

  Cell top;
  Cell right;
  Cell bottom;
  Cell left;

  // edges leading out from this cell
  ArrayList<Edge> outEdges;

  ArrayList<Cell> neighbors;

  public Cell(int x, int y) {
    this.x = x;
    this.y = y;

    this.outEdges = new ArrayList<Edge>();
    this.neighbors = new ArrayList<Cell>();
  }

  // returns this cell's hash code
  public int hashCode() {
    return this.x + this.y * 10000;
  }

  // returns whether this cell equals another given object.
  // allows comparison without having the same
  // outEdges or neighbors
  public boolean equals(Object other) {
    if (!(other instanceof Cell)) {
      return false;
    } else {
      Cell cell = (Cell) other;
      return this.x == cell.x && this.y == cell.y;
    }
  }

  // sorts this cell's outEdges
  public ArrayList<Edge> sortOutEdges() {
    Collections.sort(this.outEdges, new SortEdgeByWeight());
    return this.outEdges;

  }

  // makes the top left cell
  WorldImage makeStartCell(int cellSize) {
    return new RectangleImage(7 * cellSize / 8, 7 * cellSize / 8, 
        OutlineMode.SOLID, Color.green);
  }

  // makes the bottom right cell
  WorldImage makeEndCell(int cellSize) {
    return new RectangleImage(7 * cellSize / 8, 7 * cellSize / 8, 
        OutlineMode.SOLID, Color.red);
  }

  //draws individual cells
  WorldImage makeCell(int cellSize, boolean bottom, boolean right, Color color) {

    int bottom1 = bottom ? 1 : 0;
    int right1 = right ? 1 : 0;

    return new OverlayOffsetImage(
        new RectangleImage(1 * right1, cellSize + 1, OutlineMode.OUTLINE, color),
        -cellSize / 2, cellSize / 2,
        new RectangleImage(cellSize + 1, 1 * bottom1, OutlineMode.OUTLINE, color));
  }


  // displays this cell as a light-blue cell
  WorldImage makeLightBlueCell(int cellSize) {
    return new RectangleImage(3 * cellSize / 4, 3 * cellSize / 4, 
        OutlineMode.SOLID, Color.cyan);
  }

  // displays this cell as a dark-blue cell
  WorldImage makeDarkBlueCell(int cellSize) {
    return new RectangleImage(3 * cellSize / 4, 3 * cellSize / 4, 
        OutlineMode.SOLID, Color.blue);
  }

  // displays this cell as a black cell
  WorldImage makeBlackCell(int cellSize) {
    return new RectangleImage(5 * cellSize / 8, 5 * cellSize / 8, 
        OutlineMode.SOLID, Color.black);
  }

}

// Comparator to sort edges by weight
class SortEdgeByWeight implements Comparator<Edge> {
  public int compare(Edge a, Edge b) {
    return a.weight - b.weight;
  }
}

// represents an edge in a graph
class Edge {
  Cell from;
  Cell to;
  int weight;

  Edge(Cell from, Cell to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // returns this edge's hash code
  public int hashCode() {
    return this.from.hashCode() + this.to.hashCode();
  }

  // returns whether this Edge equals another object
  public boolean equals(Object other) {
    if (!(other instanceof Edge)) {
      return false;
    } else {
      Edge that = (Edge) other;
      return (this.from.equals(that.from) && this.to.equals(that.to))
          || (this.from.equals(that.to) && this.to.equals(that.from));
    }
  }
}

// represents a graph
class Graph {
  ArrayList<Cell> cells;

  public Graph(ArrayList<Cell> cells) {
    this.cells = cells;
  }
}

// represents a class to run Kruskal's algorithm
class Kruskal {
  Graph graph;

  HashMap<Cell, Cell> representatives;
  ArrayList<Edge> edgesInTree;
  ArrayList<Edge> worklist;

  public Kruskal(Graph graph) {
    this.graph = graph;

    this.representatives = new HashMap<Cell, Cell>();
    for (Cell cell : this.graph.cells) {
      representatives.put(cell, cell);
    }

    this.edgesInTree = new ArrayList<Edge>();

    this.worklist = new ArrayList<Edge>();
    for (Cell cell : this.graph.cells) {
      for (Edge edge : cell.outEdges) {
        this.worklist.add(edge);
      }
    }

    Collections.sort(this.worklist, new SortEdgeByWeight());

    this.worklist = this.removeDuplicates(this.worklist);
  }

  // checks if an ArrayList of Edge contains given Edge
  boolean contains(ArrayList<Edge> list, Edge e) {
    for (int i = 0; i < list.size(); i++) {
      return list.get(i).equals(e);
    }
    return false;
  }

  // removes duplicates from a given ArrayList of Edge
  ArrayList<Edge> removeDuplicates(ArrayList<Edge> worklist) {
    ArrayList<Edge> newList = new ArrayList<Edge>();

    // Traverse through the first list
    for (Edge element : worklist) {

      // If this element is not present in newList
      // then add it
      if (!this.contains(newList, element)) {

        newList.add(element);
      }
    }

    // return the new list
    return newList;
  }

  // finds the representative of a given Cell
  Cell findRepresentative(Cell node) {
    while (this.representatives.get(node) != node) {
      node = this.representatives.get(node);
    }
    return node;
  }

  // unions two Cell representatives
  void unionRepresentative(Cell to, Cell from) {
    this.representatives.put(
        this.findRepresentative(from),
        this.findRepresentative(to));
  }

  // runs Kruskal's algorithm on this class's graph
  void runKruskalImmediately() {
    for (int currentEdgeCount = 0;
        this.edgesInTree.size() < this.representatives.size() - 1;
        currentEdgeCount++) {
      Edge currentEdge = this.worklist.get(currentEdgeCount );
      if (this.findRepresentative(currentEdge.from) 
          == this.findRepresentative(currentEdge.to)) {
        continue;
      } else {
        this.edgesInTree.add(currentEdge);
        this.unionRepresentative(
            this.findRepresentative(currentEdge.from),
            this.findRepresentative(currentEdge.to));
      }
    }
  }

  // runs Kruskal's algorithm on this class's graph.
  // Must be provided an incrementing currentEdgeCount
  // (such as by an onTick function) to work
  boolean runKruskal(int currentEdgeCount) {
    if (this.edgesInTree.size() < this.representatives.size() - 1) {
      Edge currentEdge = this.worklist.get(currentEdgeCount);
      if (this.findRepresentative(currentEdge.from) 
          == this.findRepresentative(currentEdge.to)) {
        return true;
      } else {
        this.edgesInTree.add(currentEdge);
        this.unionRepresentative(
            this.findRepresentative(currentEdge.from),
            this.findRepresentative(currentEdge.to));
        return true;
      }
    } else {
      return false;
    }
  }

  // returns the sum of weights in this graph,
  // used to test if tree is a MST
  int sumWeights() {
    int total = 0;
    for (Edge e : this.edgesInTree) {
      total += e.weight;
    }
    return total;
  }
}